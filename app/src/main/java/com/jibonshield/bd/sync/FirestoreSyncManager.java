package com.jibonshield.bd.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.jibonshield.bd.database.AppDatabase;
import com.jibonshield.bd.database.entity.DisasterUpdateEntity;
import com.jibonshield.bd.database.entity.DistrictEntity;
import com.jibonshield.bd.database.entity.DivisionEntity;
import com.jibonshield.bd.database.entity.ShelterEntity;
import com.jibonshield.bd.database.entity.SyncMetadataEntity;
import com.jibonshield.bd.database.entity.UpazilaEntity;
import com.jibonshield.bd.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Robust Offline-First Cloud Firestore to Room Synchronization Manager.
 * Synchronizes administrative hierarchy (Divisions, Districts, Upazilas),
 * Emergency Shelters, and Disaster Updates into local Room DB.
 */
public class FirestoreSyncManager {

    private static final String TAG = "FirestoreSyncManager";
    private static volatile FirestoreSyncManager INSTANCE;

    public interface SyncCallback {
        void onSyncStarted(String stage);
        void onSyncProgress(String stage, int count);
        void onSyncComplete(boolean success, int totalSynced, String message);
    }

    private final Context context;
    private final AppDatabase appDatabase;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public static FirestoreSyncManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (FirestoreSyncManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FirestoreSyncManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private FirestoreSyncManager(Context context) {
        this.context = context;
        this.appDatabase = AppDatabase.getInstance(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public boolean isFirebaseAvailable() {
        try {
            return !FirebaseApp.getApps(context).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Synchronizes all Firestore collections into Room DB.
     * Respects offline status: if offline, returns immediate success using local cache.
     */
    public void syncAll(SyncCallback callback) {
        executor.execute(() -> {
            if (!NetworkUtils.isInternetAvailable(context)) {
                Log.i(TAG, "Offline: skipping Firestore sync, retaining local Room cache.");
                postComplete(callback, true, 0, "Offline: using local cache");
                return;
            }

            if (!isFirebaseAvailable()) {
                Log.w(TAG, "FirebaseApp not initialized.");
                postComplete(callback, false, 0, "Firebase not available");
                return;
            }

            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            int total = 0;

            try {
                postStarted(callback, "Syncing administrative hierarchy");

                total += syncDivisionsInternal(firestore, false);
                total += syncDistrictsInternal(firestore, false);
                total += syncUpazilasInternal(firestore, false);

                postStarted(callback, "Syncing shelters");
                total += syncSheltersInternal(firestore, false);

                postStarted(callback, "Syncing disaster alerts");
                total += syncDisasterUpdatesInternal(firestore, false);

                postComplete(callback, true, total, "Sync completed successfully (" + total + " records)");
            } catch (Exception e) {
                Log.e(TAG, "Sync encountered an error: " + e.getMessage(), e);
                postComplete(callback, false, total, "Sync error: " + e.getMessage());
            }
        });
    }

    public int syncDivisionsInternal(FirebaseFirestore firestore, boolean forceFull) throws Exception {
        String table = "divisions";
        SyncMetadataEntity meta = appDatabase.syncMetadataDao().getByTable(table);
        long lastSync = (meta != null && !forceFull) ? meta.getLastSyncTimestamp() : 0;

        Query query = firestore.collection("divisions");
        if (lastSync > 0) {
            query = query.whereGreaterThan("updatedAt", new Timestamp(new Date(lastSync)));
        }

        QuerySnapshot snap = Tasks.await(query.get(), 20, TimeUnit.SECONDS);
        List<DivisionEntity> list = new ArrayList<>();
        long maxUpdated = lastSync;

        for (DocumentSnapshot doc : snap.getDocuments()) {
            String id = doc.getId();
            String name = doc.getString("name");
            String nameBn = doc.getString("nameBn");
            String code = doc.getString("code");
            Boolean active = doc.getBoolean("isActive");
            long updated = extractTimestamp(doc, "updatedAt");
            if (updated > maxUpdated) maxUpdated = updated;

            list.add(new DivisionEntity(
                    id,
                    name != null ? name : id,
                    nameBn != null ? nameBn : "",
                    code != null ? code : id.toUpperCase(),
                    active == null || active,
                    updated
            ));
        }

        if (!list.isEmpty()) {
            appDatabase.divisionDao().insertAll(list);
            Log.i(TAG, "Inserted/updated " + list.size() + " divisions in Room.");
        }

        appDatabase.syncMetadataDao().insert(new SyncMetadataEntity(
                table,
                maxUpdated > 0 ? maxUpdated : System.currentTimeMillis(),
                appDatabase.divisionDao().getCount(),
                "SYNCED"
        ));
        return list.size();
    }

    public int syncDistrictsInternal(FirebaseFirestore firestore, boolean forceFull) throws Exception {
        String table = "districts";
        SyncMetadataEntity meta = appDatabase.syncMetadataDao().getByTable(table);
        long lastSync = (meta != null && !forceFull) ? meta.getLastSyncTimestamp() : 0;

        Query query = firestore.collection("districts");
        if (lastSync > 0) {
            query = query.whereGreaterThan("updatedAt", new Timestamp(new Date(lastSync)));
        }

        QuerySnapshot snap = Tasks.await(query.get(), 20, TimeUnit.SECONDS);
        List<DistrictEntity> list = new ArrayList<>();
        long maxUpdated = lastSync;

        for (DocumentSnapshot doc : snap.getDocuments()) {
            String id = doc.getId();
            String name = doc.getString("name");
            String nameBn = doc.getString("nameBn");
            String divisionId = doc.getString("divisionId");
            String code = doc.getString("code");
            Boolean active = doc.getBoolean("isActive");
            long updated = extractTimestamp(doc, "updatedAt");
            if (updated > maxUpdated) maxUpdated = updated;

            list.add(new DistrictEntity(
                    id,
                    name != null ? name : id,
                    nameBn != null ? nameBn : "",
                    divisionId != null ? divisionId.toLowerCase().trim() : "",
                    code != null ? code : id.toUpperCase(),
                    active == null || active,
                    updated
            ));
        }

        if (!list.isEmpty()) {
            appDatabase.districtDao().insertAll(list);
            Log.i(TAG, "Inserted/updated " + list.size() + " districts in Room.");
        }

        appDatabase.syncMetadataDao().insert(new SyncMetadataEntity(
                table,
                maxUpdated > 0 ? maxUpdated : System.currentTimeMillis(),
                appDatabase.districtDao().getCount(),
                "SYNCED"
        ));
        return list.size();
    }

    public int syncUpazilasInternal(FirebaseFirestore firestore, boolean forceFull) throws Exception {
        String table = "upazilas";
        SyncMetadataEntity meta = appDatabase.syncMetadataDao().getByTable(table);
        long lastSync = (meta != null && !forceFull) ? meta.getLastSyncTimestamp() : 0;

        Query query = firestore.collection("upazilas");
        if (lastSync > 0) {
            query = query.whereGreaterThan("updatedAt", new Timestamp(new Date(lastSync)));
        }

        QuerySnapshot snap = Tasks.await(query.get(), 25, TimeUnit.SECONDS);
        List<UpazilaEntity> list = new ArrayList<>();
        long maxUpdated = lastSync;

        for (DocumentSnapshot doc : snap.getDocuments()) {
            String id = doc.getId();
            String name = doc.getString("name");
            String nameBn = doc.getString("nameBn");
            String districtId = doc.getString("districtId");
            String code = doc.getString("code");
            Boolean active = doc.getBoolean("isActive");
            long updated = extractTimestamp(doc, "updatedAt");
            if (updated > maxUpdated) maxUpdated = updated;

            list.add(new UpazilaEntity(
                    id,
                    name != null ? name : id,
                    nameBn != null ? nameBn : "",
                    districtId != null ? districtId.toLowerCase().trim() : "",
                    code != null ? code : id.toUpperCase(),
                    active == null || active,
                    updated
            ));
        }

        if (!list.isEmpty()) {
            appDatabase.upazilaDao().insertAll(list);
            Log.i(TAG, "Inserted/updated " + list.size() + " upazilas in Room.");
        }

        appDatabase.syncMetadataDao().insert(new SyncMetadataEntity(
                table,
                maxUpdated > 0 ? maxUpdated : System.currentTimeMillis(),
                appDatabase.upazilaDao().getCount(),
                "SYNCED"
        ));
        return list.size();
    }

    public int syncSheltersInternal(FirebaseFirestore firestore, boolean forceFull) throws Exception {
        String table = "shelters";
        SyncMetadataEntity meta = appDatabase.syncMetadataDao().getByTable(table);
        long lastSync = (meta != null && !forceFull) ? meta.getLastSyncTimestamp() : 0;

        Query query = firestore.collection("shelters");
        if (lastSync > 0) {
            query = query.whereGreaterThan("updatedAt", new Timestamp(new Date(lastSync)));
        }

        QuerySnapshot snap = Tasks.await(query.get(), 25, TimeUnit.SECONDS);
        List<ShelterEntity> list = new ArrayList<>();
        long maxUpdated = lastSync;

        for (DocumentSnapshot doc : snap.getDocuments()) {
            String id = doc.getId();
            String name = doc.getString("name");
            if (name == null || name.trim().isEmpty() || "Shelter".equalsIgnoreCase(name.trim())) {
                name = doc.getString("name_en");
            }
            if (name == null || name.trim().isEmpty() || "Shelter".equalsIgnoreCase(name.trim())) {
                name = doc.getString("shelterName");
            }

            String nameBn = doc.getString("nameBn");
            if (nameBn == null || nameBn.trim().isEmpty()) {
                nameBn = doc.getString("name_bn");
            }
            if (nameBn == null || nameBn.trim().isEmpty()) {
                nameBn = name;
            }
            if (name == null || name.trim().isEmpty()) {
                name = nameBn;
            }

            String description = doc.getString("description");
            String descriptionBn = doc.getString("descriptionBn");
            String divisionId = doc.getString("divisionId");
            String districtId = doc.getString("districtId");
            if (districtId == null || districtId.trim().isEmpty()) {
                districtId = doc.getString("district");
            }
            String upazilaId = doc.getString("upazilaId");
            if (upazilaId == null || upazilaId.trim().isEmpty()) {
                upazilaId = doc.getString("upazila");
            }
            String address = doc.getString("address");
            String addressBn = doc.getString("addressBn");

            Double lat = doc.getDouble("latitude");
            Double lng = doc.getDouble("longitude");
            Long cap = doc.getLong("capacity");
            Long occ = doc.getLong("currentOccupancy");
            if (occ == null) occ = doc.getLong("current_occupancy");
            String shelterType = doc.getString("shelterType");
            if (shelterType == null || shelterType.trim().isEmpty()) {
                shelterType = doc.getString("shelter_type");
            }

            @SuppressWarnings("unchecked")
            List<String> facilities = (List<String>) doc.get("facilities");
            String contactNumber = doc.getString("contactNumber");
            if (contactNumber == null || contactNumber.trim().isEmpty()) {
                contactNumber = doc.getString("contact_phone");
            }
            Boolean active = doc.getBoolean("isActive");
            if (active == null) {
                Long actLong = doc.getLong("is_active");
                if (actLong != null) active = (actLong == 1);
            }

            long created = extractTimestamp(doc, "createdAt");
            long updated = extractTimestamp(doc, "updatedAt");
            if (updated > maxUpdated) maxUpdated = updated;

            list.add(new ShelterEntity(
                    id,
                    name != null ? name : "Shelter",
                    nameBn != null ? nameBn : "",
                    description != null ? description : "",
                    descriptionBn != null ? descriptionBn : "",
                    divisionId != null ? divisionId.toLowerCase().trim() : "",
                    districtId != null ? districtId.toLowerCase().trim() : "",
                    upazilaId != null ? upazilaId.toLowerCase().trim() : "",
                    address != null ? address : "",
                    addressBn != null ? addressBn : "",
                    lat != null ? lat : 0.0,
                    lng != null ? lng : 0.0,
                    cap != null ? cap.intValue() : 0,
                    occ != null ? occ.intValue() : 0,
                    shelterType != null ? shelterType : "Cyclone Shelter",
                    facilities != null ? facilities : new ArrayList<>(),
                    contactNumber != null ? contactNumber : "",
                    active == null || active,
                    created > 0 ? created : System.currentTimeMillis(),
                    updated > 0 ? updated : System.currentTimeMillis()
            ));
        }

        if (!list.isEmpty()) {
            appDatabase.shelterDao().insertAll(list);
            Log.i(TAG, "Inserted/updated " + list.size() + " shelters in Room.");
        }

        appDatabase.syncMetadataDao().insert(new SyncMetadataEntity(
                table,
                maxUpdated > 0 ? maxUpdated : System.currentTimeMillis(),
                appDatabase.shelterDao().getActiveCount(),
                "SYNCED"
        ));
        return list.size();
    }

    public int syncDisasterUpdatesInternal(FirebaseFirestore firestore, boolean forceFull) throws Exception {
        String table = "disaster_updates";
        SyncMetadataEntity meta = appDatabase.syncMetadataDao().getByTable(table);
        long lastSync = (meta != null && !forceFull) ? meta.getLastSyncTimestamp() : 0;

        Query query = firestore.collection("disaster_updates");
        if (lastSync > 0) {
            query = query.whereGreaterThan("updatedAt", new Timestamp(new Date(lastSync)));
        }

        QuerySnapshot snap = Tasks.await(query.get(), 25, TimeUnit.SECONDS);
        List<DisasterUpdateEntity> list = new ArrayList<>();
        long maxUpdated = lastSync;

        for (DocumentSnapshot doc : snap.getDocuments()) {
            String id = doc.getId();
            String title = doc.getString("title");
            String titleBn = doc.getString("titleBn");
            String description = doc.getString("description");
            String descriptionBn = doc.getString("descriptionBn");
            String disasterType = doc.getString("disasterType");
            String severity = doc.getString("severity");
            String status = doc.getString("status");
            String divisionId = doc.getString("divisionId");
            String districtId = doc.getString("districtId");
            String upazilaId = doc.getString("upazilaId");

            @SuppressWarnings("unchecked")
            List<String> affectedAreas = (List<String>) doc.get("affectedAreas");
            String issuedBy = doc.getString("issuedBy");

            long issuedAt = extractTimestamp(doc, "issuedAt");
            long expiresAt = extractTimestamp(doc, "expiresAt");
            long updated = extractTimestamp(doc, "updatedAt");
            if (updated > maxUpdated) maxUpdated = updated;

            Boolean active = doc.getBoolean("isActive");

            list.add(new DisasterUpdateEntity(
                    id,
                    title != null ? title : "Disaster Alert",
                    titleBn != null ? titleBn : "",
                    description != null ? description : "",
                    descriptionBn != null ? descriptionBn : "",
                    disasterType != null ? disasterType : "Flood",
                    severity != null ? severity : "Moderate",
                    status != null ? status : "Active",
                    divisionId != null ? divisionId.toLowerCase().trim() : "",
                    districtId != null ? districtId.toLowerCase().trim() : "",
                    upazilaId != null ? upazilaId.toLowerCase().trim() : "",
                    affectedAreas != null ? affectedAreas : new ArrayList<>(),
                    issuedBy != null ? issuedBy : "FFWC / BMD",
                    issuedAt > 0 ? issuedAt : System.currentTimeMillis(),
                    expiresAt,
                    updated > 0 ? updated : System.currentTimeMillis(),
                    active == null || active
            ));
        }

        if (!list.isEmpty()) {
            appDatabase.disasterUpdateDao().insertAll(list);
            Log.i(TAG, "Inserted/updated " + list.size() + " disaster updates in Room.");
        }

        appDatabase.syncMetadataDao().insert(new SyncMetadataEntity(
                table,
                maxUpdated > 0 ? maxUpdated : System.currentTimeMillis(),
                appDatabase.disasterUpdateDao().getActiveCount(),
                "SYNCED"
        ));
        return list.size();
    }

    private long extractTimestamp(DocumentSnapshot doc, String field) {
        Object val = doc.get(field);
        if (val instanceof Timestamp) {
            return ((Timestamp) val).toDate().getTime();
        } else if (val instanceof Number) {
            return ((Number) val).longValue();
        } else if (val instanceof Date) {
            return ((Date) val).getTime();
        }
        return 0L;
    }

    private void postStarted(SyncCallback callback, String stage) {
        if (callback != null) {
            mainHandler.post(() -> callback.onSyncStarted(stage));
        }
    }

    private void postComplete(SyncCallback callback, boolean success, int count, String msg) {
        if (callback != null) {
            mainHandler.post(() -> callback.onSyncComplete(success, count, msg));
        }
    }
}

