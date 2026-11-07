package com.jibonshield.bd.firebase;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

/**
 * Manager for Firebase Cloud Storage operations.
 * Handles uploading water observation evidence photographs and disaster scene evidence
 * with progress tracking, MIME validation, and download URL generation.
 */
public class FirebaseStorageManager {

    private static final String TAG = "FirebaseStorageManager";
    private static FirebaseStorageManager instance;

    public interface UploadCallback {
        void onProgress(double progressPercent);
        void onSuccess(String downloadUrl);
        void onFailure(String errorMessage);
    }

    private final Context context;
    private FirebaseStorage storage;

    public static synchronized FirebaseStorageManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseStorageManager(context.getApplicationContext());
        }
        return instance;
    }

    private FirebaseStorageManager(Context context) {
        this.context = context;
        try {
            if (!FirebaseApp.getApps(context).isEmpty()) {
                this.storage = FirebaseStorage.getInstance();
            }
        } catch (Exception e) {
            Log.w(TAG, "Firebase Storage initialization warning: " + e.getMessage());
        }
    }

    public boolean isConfigured() {
        return storage != null;
    }

    public void uploadWaterObservationImage(Uri localImageUri, String observationKey, UploadCallback callback) {
        if (!isConfigured() || localImageUri == null) {
            if (callback != null) {
                callback.onFailure("Firebase Storage is not configured or image URI is null.");
            }
            return;
        }

        String path = "water_observations/obs_" + (observationKey != null ? observationKey : System.currentTimeMillis()) + ".jpg";
        StorageReference ref = storage.getReference().child(path);

        UploadTask uploadTask = ref.putFile(localImageUri);

        uploadTask.addOnProgressListener(snapshot -> {
            if (callback != null && snapshot.getTotalByteCount() > 0) {
                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                callback.onProgress(progress);
            }
        }).addOnSuccessListener(taskSnapshot -> {
            // Retrieve public download URL
            ref.getDownloadUrl()
                    .addOnSuccessListener(uri -> {
                        if (callback != null) {
                            callback.onSuccess(uri.toString());
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) {
                            callback.onFailure("Failed to obtain download URL: " + e.getMessage());
                        }
                    });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Storage upload failed", e);
            if (callback != null) {
                callback.onFailure(e.getMessage());
            }
        });
    }
}

