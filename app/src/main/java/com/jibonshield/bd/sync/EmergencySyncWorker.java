package com.jibonshield.bd.sync;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.jibonshield.bd.firebase.FirebaseAuthManager;
import com.jibonshield.bd.firebase.FirebaseStorageManager;
import com.jibonshield.bd.firebase.FirestoreManager;
import com.jibonshield.bd.models.WaterObservation;
import com.jibonshield.bd.repositories.EmergencyRepository;
import com.jibonshield.bd.utils.NetworkUtils;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WorkManager Worker for background two-way synchronization.
 * Uploads un-synced local water quality observations (and associated evidence photos)
 * to Cloud Firestore and Firebase Storage, and refreshes local warnings and shelters.
 */
public class EmergencySyncWorker extends Worker {

    private static final String TAG = "EmergencySyncWorker";
    public static final String WORK_NAME_PERIODIC = "jibonshield_periodic_sync";
    public static final String WORK_NAME_ONE_TIME = "jibonshield_immediate_sync";

    public EmergencySyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        Log.i(TAG, "Starting EmergencySyncWorker synchronization cycle");

        // Step 1: Connectivity check
        if (!NetworkUtils.isInternetAvailable(context)) {
            Log.w(TAG, "No internet available; rescheduling work.");
            return Result.retry();
        }

        EmergencyRepository repository = new EmergencyRepository(context);
        FirestoreManager firestore = FirestoreManager.getInstance(context);
        FirebaseStorageManager storage = FirebaseStorageManager.getInstance(context);
        String currentUserId = FirebaseAuthManager.getInstance(context).getCurrentUserId();

        // Step 2: Synchronize pending local water observations
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean hasError = new AtomicBoolean(false);

        repository.getPendingWaterObservations(new EmergencyRepository.DataCallback<List<WaterObservation>>() {
            @Override
            public void onDataLoaded(List<WaterObservation> pendingList) {
                if (pendingList == null || pendingList.isEmpty()) {
                    latch.countDown();
                    return;
                }

                Log.i(TAG, "Found " + pendingList.size() + " pending observations to synchronize.");
                CountDownLatch itemLatch = new CountDownLatch(pendingList.size());

                for (WaterObservation obs : pendingList) {
                    processPendingObservation(obs, repository, firestore, storage, currentUserId, itemLatch);
                }

                try {
                    itemLatch.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Log.w(TAG, "Observation sync wait interrupted", e);
                }
                latch.countDown();
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error fetching pending observations", error);
                hasError.set(true);
                latch.countDown();
            }
        });

        try {
            latch.await(45, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.w(TAG, "Work cycle interrupted", e);
        }

        // Step 3: Trigger core entity sync
        SyncManager.getInstance(context).syncAll(null);
        FirestoreSyncManager.getInstance(context).syncAll(null);

        return hasError.get() ? Result.retry() : Result.success();
    }

    private void processPendingObservation(WaterObservation obs,
                                           EmergencyRepository repository,
                                           FirestoreManager firestore,
                                           FirebaseStorageManager storage,
                                           String userId,
                                           CountDownLatch itemLatch) {
        String imageUriStr = obs.getImageUri();
        if (imageUriStr != null && !imageUriStr.trim().isEmpty() && obs.getImageUrl() == null && storage.isConfigured()) {
            // Upload photo first
            storage.uploadWaterObservationImage(Uri.parse(imageUriStr), String.valueOf(obs.getId()), new FirebaseStorageManager.UploadCallback() {
                @Override
                public void onProgress(double progressPercent) {}

                @Override
                public void onSuccess(String downloadUrl) {
                    // Update Firestore with download URL
                    WaterObservation withUrl = new WaterObservation(
                            obs.getId(), obs.getLocationName(), obs.getDistrict(), obs.getSourceType(),
                            obs.getAppearance(), obs.getOdor(), obs.getPhLevel(), obs.getTurbidityStatus(),
                            obs.getSafetyStatus(), obs.getObservationTime(), obs.getReportedBy(), obs.getRemarks(),
                            obs.getImageUri(), downloadUrl, WaterObservation.SYNC_SYNCED,
                            obs.getCreatedAt(), System.currentTimeMillis()
                    );
                    uploadToFirestore(withUrl, repository, firestore, userId, itemLatch);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.w(TAG, "Image upload failed for obs " + obs.getId() + ": " + errorMessage);
                    // Still upload metadata to firestore even if image upload failed
                    uploadToFirestore(obs, repository, firestore, userId, itemLatch);
                }
            });
        } else {
            uploadToFirestore(obs, repository, firestore, userId, itemLatch);
        }
    }

    private void uploadToFirestore(WaterObservation obs,
                                   EmergencyRepository repository,
                                   FirestoreManager firestore,
                                   String userId,
                                   CountDownLatch itemLatch) {
        if (firestore.isConfigured()) {
            firestore.saveWaterObservation(obs, userId, new FirestoreManager.FirestoreCallback() {
                @Override
                public void onSuccess(String documentId) {
                    repository.updateWaterObservationSyncStatus(obs.getId(), WaterObservation.SYNC_SYNCED, obs.getImageUrl(), null);
                    itemLatch.countDown();
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.w(TAG, "Firestore sync failed for obs " + obs.getId() + ": " + errorMessage);
                    itemLatch.countDown();
                }
            });
        } else {
            itemLatch.countDown();
        }
    }

    /**
     * Schedules periodic background synchronization when device is connected to network.
     */
    public static void schedulePeriodicSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                EmergencySyncWorker.class, 6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
        );
    }

    /**
     * Triggers an immediate one-time synchronization pass.
     */
    public static void triggerImmediateSync(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(EmergencySyncWorker.class)
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONE_TIME,
                ExistingWorkPolicy.REPLACE,
                workRequest
        );
    }
}

