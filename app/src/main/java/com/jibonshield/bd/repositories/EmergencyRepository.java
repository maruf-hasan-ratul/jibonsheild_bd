package com.jibonshield.bd.repositories;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.jibonshield.bd.database.AppDatabase;
import com.jibonshield.bd.database.DatabaseContract;
import com.jibonshield.bd.database.ModelMapper;
import com.jibonshield.bd.database.entity.DisasterUpdateEntity;
import com.jibonshield.bd.database.entity.DistrictEntity;
import com.jibonshield.bd.database.entity.DivisionEntity;
import com.jibonshield.bd.database.entity.ShelterEntity;
import com.jibonshield.bd.database.entity.UpazilaEntity;
import com.jibonshield.bd.sync.FirestoreSyncManager;
import com.jibonshield.bd.models.ChecklistItem;
import com.jibonshield.bd.models.ChecklistProgress;
import com.jibonshield.bd.models.EmergencyContact;
import com.jibonshield.bd.models.FloodWarning;
import com.jibonshield.bd.models.PreparednessChecklist;
import com.jibonshield.bd.models.Shelter;
import com.jibonshield.bd.models.SyncMetadata;
import com.jibonshield.bd.models.WaterObservation;
import com.jibonshield.bd.utils.DistrictData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cloud Firestore Repository for JibonShield BD.
 * Replaces SQLite with Cloud Firestore, featuring persistent local offline caching,
 * real-time cloud synchronization, zero main-thread blocking, and graceful fallback.
 */
public class EmergencyRepository {

    private static final String TAG = "EmergencyRepository";

    public interface DatabaseInitCallback {
        void onDatabaseReady(int contactCount);
        void onError(Exception error);
    }

    public interface DataCallback<T> {
        void onDataLoaded(T data);
        void onError(Exception error);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(Exception error);
    }

    public interface SyncCallback {
        void onSyncComplete(int inserted, int updated);
        void onSyncError(Exception error);
    }

    private final Context context;
    private final FirebaseFirestore db;
    private final AppDatabase appDatabase;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public EmergencyRepository() {
        this.context = null;
        this.appDatabase = null;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = null;
        this.db = null;
    }

    public EmergencyRepository(Context context) {
        this.context = context.getApplicationContext();
        this.appDatabase = AppDatabase.getInstance(this.context);
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());

        FirebaseFirestore firestoreInstance = null;
        try {
            if (!FirebaseApp.getApps(this.context).isEmpty()) {
                firestoreInstance = FirebaseFirestore.getInstance();
                try {
                    FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                            .setPersistenceEnabled(true)
                            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                            .build();
                    firestoreInstance.setFirestoreSettings(settings);
                } catch (Exception ignored) {
                    // Settings can only be applied prior to initial Firestore calls
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Firebase initialization notice: " + e.getMessage());
        }
        this.db = firestoreInstance;

        // Trigger background sync silently if connected
        try {
            FirestoreSyncManager.getInstance(this.context).syncAll(null);
        } catch (Exception ignored) {}
    }

    private boolean isFirestoreAvailable() {
        return db != null;
    }

    // =========================================================================
    // 1. DATABASE INITIALIZATION & VERIFICATION
    // =========================================================================

    public void ensureDatabaseInitialized(DatabaseInitCallback callback) {
        if (!isFirestoreAvailable()) {
            if (callback != null) mainHandler.post(() -> callback.onDatabaseReady(7));
            return;
        }

        db.collection(DatabaseContract.EmergencyContactEntry.TABLE_NAME)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot == null || snapshot.isEmpty()) {
                        Log.i(TAG, "Firestore collection empty. Seeding baseline emergency datasets...");
                        seedInitialDataInFirestore(callback);
                    } else {
                        db.collection(DatabaseContract.EmergencyContactEntry.TABLE_NAME)
                                .get()
                                .addOnSuccessListener(allDocs -> {
                                    int count = allDocs != null ? allDocs.size() : 7;
                                    if (callback != null) mainHandler.post(() -> callback.onDatabaseReady(count));
                                })
                                .addOnFailureListener(e -> {
                                    if (callback != null) mainHandler.post(() -> callback.onDatabaseReady(7));
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Offline or initial Firestore read notice, attempting seed verification: " + e.getMessage());
                    seedInitialDataInFirestore(callback);
                });
    }

    private void seedInitialDataInFirestore(DatabaseInitCallback callback) {
        executorService.execute(() -> {
            try {
                WriteBatch batch = db.batch();

                // 1. Seed Emergency Contacts
                List<EmergencyContact> contacts = getSeedContacts();
                for (EmergencyContact c : contacts) {
                    DocumentReference ref = db.collection(DatabaseContract.EmergencyContactEntry.TABLE_NAME).document("contact_" + c.getId());
                    batch.set(ref, emergencyContactToMap(c));
                }

                // 2. Seed Flood Warnings
                List<FloodWarning> warnings = getSeedFloodWarnings();
                for (FloodWarning w : warnings) {
                    DocumentReference ref = db.collection(DatabaseContract.FloodWarningEntry.TABLE_NAME).document("warning_" + w.getId());
                    batch.set(ref, floodWarningToMap(w));
                }

                // 3. Seed Shelters
                List<Shelter> shelters = getSeedShelters();
                for (Shelter s : shelters) {
                    DocumentReference ref = db.collection(DatabaseContract.ShelterEntry.TABLE_NAME).document("shelter_" + s.getId());
                    batch.set(ref, shelterToMap(s));
                }

                // 4. Seed Preparedness Checklists & Items
                seedChecklistsInBatch(batch);

                // Commit batch
                batch.commit().addOnCompleteListener(task -> {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onDatabaseReady(contacts.size()));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error seeding Firestore baseline: " + e.getMessage(), e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onDatabaseReady(7));
                }
            }
        });
    }

    // =========================================================================
    // 2. EMERGENCY CONTACTS REPOSITORY
    // =========================================================================

    public void getEmergencyContacts(DataCallback<List<EmergencyContact>> callback) {
        getEmergencyContactsInternal(false, callback);
    }

    public void getVerifiedEmergencyContacts(DataCallback<List<EmergencyContact>> callback) {
        getEmergencyContactsInternal(true, callback);
    }

    private void getEmergencyContactsInternal(boolean enabledOnly, DataCallback<List<EmergencyContact>> callback) {
        if (!isFirestoreAvailable()) {
            if (callback != null) mainHandler.post(() -> callback.onDataLoaded(getSeedContacts()));
            return;
        }

        Query query = db.collection(DatabaseContract.EmergencyContactEntry.TABLE_NAME)
                .orderBy(DatabaseContract.EmergencyContactEntry.COLUMN_PRIORITY_ORDER, Query.Direction.ASCENDING);

        if (enabledOnly) {
            query = query.whereEqualTo("enabled", true);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<EmergencyContact> list = new ArrayList<>();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            EmergencyContact contact = mapToEmergencyContact(doc);
                            if (contact != null) list.add(contact);
                        }
                    }
                    if (list.isEmpty()) {
                        list = getSeedContacts();
                    }
                    final List<EmergencyContact> finalList = list;
                    if (callback != null) mainHandler.post(() -> callback.onDataLoaded(finalList));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) mainHandler.post(() -> callback.onDataLoaded(getSeedContacts()));
                });
    }

    public void insertEmergencyContact(EmergencyContact contact, ActionCallback callback) {
        if (!isFirestoreAvailable() || contact == null) {
            if (callback != null) mainHandler.post(callback::onSuccess);
            return;
        }

        String docId = "contact_" + (contact.getId() > 0 ? contact.getId() : System.currentTimeMillis());
        db.collection(DatabaseContract.EmergencyContactEntry.TABLE_NAME)
                .document(docId)
                .set(emergencyContactToMap(contact))
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) mainHandler.post(callback::onSuccess);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) mainHandler.post(() -> callback.onError(e));
                });
    }

    public void updateEmergencyContact(EmergencyContact contact, ActionCallback callback) {
        insertEmergencyContact(contact, callback);
    }

    public void syncEmergencyContactsTransactional(List<EmergencyContact> contacts, String sourceName, SyncCallback callback) {
        if (!isFirestoreAvailable() || contacts == null) {
            if (callback != null) mainHandler.post(() -> callback.onSyncComplete(0, 0));
            return;
        }

        WriteBatch batch = db.batch();
        int count = 0;
        for (EmergencyContact contact : contacts) {
            String docId = "contact_" + (contact.getId() > 0 ? contact.getId() : System.currentTimeMillis() + count);
            DocumentReference ref = db.collection(DatabaseContract.EmergencyContactEntry.TABLE_NAME).document(docId);
            batch.set(ref, emergencyContactToMap(contact), SetOptions.merge());
            count++;
        }

        final int total = count;
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) mainHandler.post(() -> callback.onSyncComplete(total, 0));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) mainHandler.post(() -> callback.onSyncError(e));
                });
    }

    // =========================================================================
    // 3. FLOOD WARNINGS REPOSITORY
    // =========================================================================

    public void getFloodWarnings(String regionFilter, DataCallback<List<FloodWarning>> callback) {
        getFloodWarningsInternal(regionFilter, null, callback);
    }

    public void getActiveFloodWarnings(String regionFilter, DataCallback<List<FloodWarning>> callback) {
        getFloodWarningsInternal(regionFilter, Boolean.TRUE, callback);
    }

    public void getExpiredFloodWarnings(String regionFilter, DataCallback<List<FloodWarning>> callback) {
        getFloodWarningsInternal(regionFilter, Boolean.FALSE, callback);
    }

    private void getFloodWarningsInternal(String regionFilter, Boolean activeFilter, DataCallback<List<FloodWarning>> callback) {
        if (appDatabase != null) {
            executorService.execute(() -> {
                List<DisasterUpdateEntity> entities = (activeFilter != null && activeFilter)
                        ? appDatabase.disasterUpdateDao().getAllActive()
                        : appDatabase.disasterUpdateDao().getAll();
                if (entities != null && !entities.isEmpty()) {
                    List<FloodWarning> list = new ArrayList<>();
                    for (DisasterUpdateEntity entity : entities) {
                        list.add(ModelMapper.toFloodWarning(entity));
                    }
                    List<FloodWarning> filtered = filterFloodWarnings(list, regionFilter, activeFilter);
                    if (callback != null && mainHandler != null) {
                        mainHandler.post(() -> callback.onDataLoaded(filtered));
                    }
                }

                // Immediately synchronize with latest Firestore disaster_updates in cloud
                queryFirestoreOrSeedFloodWarnings(regionFilter, activeFilter, callback);
            });
            return;
        }

        queryFirestoreOrSeedFloodWarnings(regionFilter, activeFilter, callback);
    }

    private void queryFirestoreOrSeedFloodWarnings(String regionFilter, Boolean activeFilter, DataCallback<List<FloodWarning>> callback) {
        if (!isFirestoreAvailable()) {
            if (callback != null && mainHandler != null) {
                mainHandler.post(() -> callback.onDataLoaded(filterFloodWarnings(getSeedFloodWarnings(), regionFilter, activeFilter)));
            } else if (callback != null) {
                callback.onDataLoaded(filterFloodWarnings(getSeedFloodWarnings(), regionFilter, activeFilter));
            }
            return;
        }

        db.collection("disaster_updates")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<FloodWarning> list = new ArrayList<>();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        List<DisasterUpdateEntity> entitiesToCache = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            FloodWarning warning = mapToFloodWarning(doc);
                            if (warning != null) list.add(warning);
                            DisasterUpdateEntity ent = mapToDisasterUpdateEntity(doc);
                            if (ent != null) entitiesToCache.add(ent);
                        }
                        if (appDatabase != null && !entitiesToCache.isEmpty()) {
                            executorService.execute(() -> appDatabase.disasterUpdateDao().insertAll(entitiesToCache));
                        }
                    }
                    if (list.isEmpty()) {
                        list = getSeedFloodWarnings();
                    }
                    List<FloodWarning> filtered = filterFloodWarnings(list, regionFilter, activeFilter);
                    if (callback != null && mainHandler != null) mainHandler.post(() -> callback.onDataLoaded(filtered));
                    else if (callback != null) callback.onDataLoaded(filtered);
                })
                .addOnFailureListener(e -> {
                    List<FloodWarning> fallback = filterFloodWarnings(getSeedFloodWarnings(), regionFilter, activeFilter);
                    if (callback != null && mainHandler != null) mainHandler.post(() -> callback.onDataLoaded(fallback));
                    else if (callback != null) callback.onDataLoaded(fallback);
                });
    }

    private List<FloodWarning> filterFloodWarnings(List<FloodWarning> list, String regionFilter, Boolean activeFilter) {
        List<FloodWarning> result = new ArrayList<>();
        for (FloodWarning w : list) {
            boolean matchesRegion = (regionFilter == null || regionFilter.isEmpty() || regionFilter.equalsIgnoreCase("All")
                    || (w.getRegion() != null && w.getRegion().toLowerCase().contains(regionFilter.toLowerCase())));
            if (!matchesRegion) continue;

            if (activeFilter != null) {
                if (activeFilter && !w.isExpired()) {
                    result.add(w);
                } else if (!activeFilter && w.isExpired()) {
                    result.add(w);
                }
            } else {
                result.add(w);
            }
        }
        return result;
    }

    public void getFloodWarningById(long id, DataCallback<FloodWarning> callback) {
        if (!isFirestoreAvailable()) {
            for (FloodWarning w : getSeedFloodWarnings()) {
                if (w.getId() == id) {
                    if (callback != null) mainHandler.post(() -> callback.onDataLoaded(w));
                    return;
                }
            }
            if (callback != null) mainHandler.post(() -> callback.onDataLoaded(null));
            return;
        }

        db.collection(DatabaseContract.FloodWarningEntry.TABLE_NAME)
                .document("warning_" + id)
                .get()
                .addOnSuccessListener(doc -> {
                    FloodWarning warning = doc.exists() ? mapToFloodWarning(doc) : null;
                    if (callback != null) mainHandler.post(() -> callback.onDataLoaded(warning));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) mainHandler.post(() -> callback.onError(e));
                });
    }

    public void insertFloodWarning(FloodWarning warning, ActionCallback callback) {
        if (!isFirestoreAvailable() || warning == null) {
            if (callback != null) mainHandler.post(callback::onSuccess);
            return;
        }

        String docId = "warning_" + (warning.getId() > 0 ? warning.getId() : System.currentTimeMillis());
        db.collection(DatabaseContract.FloodWarningEntry.TABLE_NAME)
                .document(docId)
                .set(floodWarningToMap(warning))
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) mainHandler.post(callback::onSuccess);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) mainHandler.post(() -> callback.onError(e));
                });
    }

    /**
     * Real-time listener on Cloud Firestore disaster_updates collection.
     */
    public ListenerRegistration listenToDisasterUpdates(
            String regionFilter, Boolean activeFilter, DataCallback<List<FloodWarning>> callback) {
        if (!isFirestoreAvailable()) return null;
        return db.collection("disaster_updates")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Real-time snapshot listen error: " + error.getMessage());
                        return;
                    }
                    if (snapshots != null) {
                        List<FloodWarning> list = new ArrayList<>();
                        List<DisasterUpdateEntity> entitiesToCache = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            FloodWarning warning = mapToFloodWarning(doc);
                            if (warning != null) list.add(warning);
                            DisasterUpdateEntity ent = mapToDisasterUpdateEntity(doc);
                            if (ent != null) entitiesToCache.add(ent);
                        }
                        if (appDatabase != null && !entitiesToCache.isEmpty()) {
                            executorService.execute(() -> appDatabase.disasterUpdateDao().insertAll(entitiesToCache));
                        }
                        List<FloodWarning> filtered = filterFloodWarnings(list, regionFilter, activeFilter);
                        if (callback != null && mainHandler != null) {
                            mainHandler.post(() -> callback.onDataLoaded(filtered));
                        } else if (callback != null) {
                            callback.onDataLoaded(filtered);
                        }
                    }
                });
    }

    /**
     * CRUD: CREATE a new disaster update alert in Firestore and local Room database.
     */
    public void createDisasterUpdate(DisasterUpdateEntity update, ActionCallback callback) {
        if (update == null) {
            if (callback != null) mainHandler.post(() -> callback.onError(new IllegalArgumentException("Null entity")));
            return;
        }

        executorService.execute(() -> {
            if (appDatabase != null) {
                appDatabase.disasterUpdateDao().insert(update);
            }

            if (!isFirestoreAvailable()) {
                if (callback != null && mainHandler != null) mainHandler.post(callback::onSuccess);
                return;
            }

            Map<String, Object> map = disasterUpdateToMap(update);
            db.collection("disaster_updates")
                    .document(update.getId())
                    .set(map, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        if (callback != null && mainHandler != null) mainHandler.post(callback::onSuccess);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Firestore write warning: " + e.getMessage());
                        if (callback != null && mainHandler != null) mainHandler.post(callback::onSuccess);
                    });
        });
    }

    /**
     * CRUD: UPDATE an existing disaster update alert in Firestore and Room.
     */
    public void updateDisasterUpdate(String id, Map<String, Object> updates, ActionCallback callback) {
        if (id == null || id.isEmpty()) {
            if (callback != null && mainHandler != null) mainHandler.post(callback::onSuccess);
            return;
        }

        executorService.execute(() -> {
            if (appDatabase != null) {
                DisasterUpdateEntity existing = appDatabase.disasterUpdateDao().getById(id);
                if (existing != null) {
                    if (updates.containsKey("status")) {
                        existing.setStatus((String) updates.get("status"));
                    }
                    if (updates.containsKey("isActive")) {
                        existing.setActive(Boolean.TRUE.equals(updates.get("isActive")));
                    }
                    if (updates.containsKey("severity")) {
                        existing.setSeverity((String) updates.get("severity"));
                    }
                    if (updates.containsKey("description")) {
                        existing.setDescription((String) updates.get("description"));
                    }
                    if (updates.containsKey("title")) {
                        existing.setTitle((String) updates.get("title"));
                    }
                    existing.setUpdatedAt(System.currentTimeMillis());
                    appDatabase.disasterUpdateDao().insert(existing);
                }
            }

            if (!isFirestoreAvailable()) {
                if (callback != null && mainHandler != null) mainHandler.post(callback::onSuccess);
                return;
            }

            updates.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
            db.collection("disaster_updates")
                    .document(id)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        if (callback != null && mainHandler != null) mainHandler.post(callback::onSuccess);
                    })
                    .addOnFailureListener(e -> {
                        db.collection("disaster_updates")
                                .document(id)
                                .set(updates, SetOptions.merge())
                                .addOnSuccessListener(v -> {
                                    if (callback != null && mainHandler != null) mainHandler.post(callback::onSuccess);
                                })
                                .addOnFailureListener(err -> {
                                    if (callback != null && mainHandler != null) mainHandler.post(() -> callback.onError(err));
                                });
                    });
        });
    }

    /**
     * CRUD: DELETE a disaster update alert from Firestore and local Room database.
     */
    public void deleteDisasterUpdate(String id, ActionCallback callback) {
        if (id == null || id.isEmpty()) {
            if (callback != null && mainHandler != null) mainHandler.post(callback::onSuccess);
            return;
        }

        executorService.execute(() -> {
            if (appDatabase != null) {
                appDatabase.disasterUpdateDao().deleteById(id);
            }

            if (!isFirestoreAvailable()) {
                if (callback != null && mainHandler != null) mainHandler.post(callback::onSuccess);
                return;
            }

            db.collection("disaster_updates")
                    .document(id)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        if (callback != null && mainHandler != null) mainHandler.post(callback::onSuccess);
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null && mainHandler != null) mainHandler.post(() -> callback.onError(e));
                    });
        });
    }

    public void syncFloodWarningsTransactional(List<FloodWarning> warnings, String sourceName, SyncCallback callback) {
        if (!isFirestoreAvailable() || warnings == null) {
            if (callback != null) mainHandler.post(() -> callback.onSyncComplete(0, 0));
            return;
        }

        WriteBatch batch = db.batch();
        int count = 0;
        for (FloodWarning warning : warnings) {
            String docId = "warning_" + (warning.getId() > 0 ? warning.getId() : System.currentTimeMillis() + count);
            DocumentReference ref = db.collection(DatabaseContract.FloodWarningEntry.TABLE_NAME).document(docId);
            batch.set(ref, floodWarningToMap(warning), SetOptions.merge());
            count++;
        }

        final int total = count;
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) mainHandler.post(() -> callback.onSyncComplete(total, 0));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) mainHandler.post(() -> callback.onSyncError(e));
                });
    }

    // =========================================================================
    // 4. SHELTERS REPOSITORY
    // =========================================================================

    public void getShelters(String query, String district, DataCallback<List<Shelter>> callback) {
        searchShelters(query, district, null, callback);
    }

    public void searchShelters(String query, String district, String upazila, DataCallback<List<Shelter>> callback) {
        if (isFirestoreAvailable()) {
            queryFirestoreOrSeedShelters(query, district, upazila, callback);
            return;
        }

        if (appDatabase != null) {
            executorService.execute(() -> {
                List<ShelterEntity> entities = appDatabase.shelterDao().getAllActive();
                if (entities != null && !entities.isEmpty()) {
                    List<Shelter> list = new ArrayList<>();
                    for (ShelterEntity entity : entities) {
                        list.add(ModelMapper.toShelter(entity));
                    }
                    List<Shelter> filtered = filterShelters(list, query, district, upazila);
                    if (callback != null && mainHandler != null) {
                        mainHandler.post(() -> callback.onDataLoaded(filtered));
                    } else if (callback != null) {
                        callback.onDataLoaded(filtered);
                    }
                    return;
                }

                List<Shelter> fallback = filterShelters(getSeedShelters(), query, district, upazila);
                if (callback != null && mainHandler != null) {
                    mainHandler.post(() -> callback.onDataLoaded(fallback));
                } else if (callback != null) {
                    callback.onDataLoaded(fallback);
                }
            });
            return;
        }

        if (callback != null) {
            callback.onDataLoaded(filterShelters(getSeedShelters(), query, district, upazila));
        }
    }

    private void queryFirestoreOrSeedShelters(String query, String district, String upazila, DataCallback<List<Shelter>> callback) {
        if (!isFirestoreAvailable()) {
            if (callback != null && mainHandler != null) {
                mainHandler.post(() -> callback.onDataLoaded(filterShelters(getSeedShelters(), query, district, upazila)));
            } else if (callback != null) {
                callback.onDataLoaded(filterShelters(getSeedShelters(), query, district, upazila));
            }
            return;
        }

        db.collection(DatabaseContract.ShelterEntry.TABLE_NAME)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Shelter> list = new ArrayList<>();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Shelter shelter = mapToShelter(doc);
                            if (shelter != null) list.add(shelter);
                        }
                    }
                    if (list.isEmpty()) {
                        list = getSeedShelters();
                    } else {
                        List<Shelter> seeds = getSeedShelters();
                        for (Shelter seed : seeds) {
                            boolean exists = false;
                            for (Shelter existing : list) {
                                if (existing.getId() == seed.getId()) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                list.add(seed);
                            }
                        }
                    }

                    // Update local Room cache in background with genuine shelter names
                    if (!list.isEmpty() && appDatabase != null) {
                        final List<Shelter> sheltersToCache = new ArrayList<>(list);
                        executorService.execute(() -> {
                            try {
                                List<ShelterEntity> entities = new ArrayList<>();
                                for (Shelter s : sheltersToCache) {
                                    entities.add(new ShelterEntity(
                                            "shelter_" + s.getId(),
                                            s.getNameEn(),
                                            s.getNameBn(),
                                            "",
                                            "",
                                            "",
                                            s.getDistrict() != null ? s.getDistrict().toLowerCase().trim() : "",
                                            s.getUpazila() != null ? s.getUpazila().toLowerCase().trim() : "",
                                            s.getAddress() != null ? s.getAddress() : "",
                                            "",
                                            s.getLatitude(),
                                            s.getLongitude(),
                                            s.getCapacity(),
                                            s.getCurrentOccupancy(),
                                            s.getShelterType(),
                                            new ArrayList<>(),
                                            s.getContactPhone(),
                                            s.isActive(),
                                            s.getCreatedAt(),
                                            s.getUpdatedAt()
                                    ));
                                }
                                appDatabase.shelterDao().insertAll(entities);
                            } catch (Exception ignored) {}
                        });
                    }

                    List<Shelter> filtered = filterShelters(list, query, district, upazila);
                    if (callback != null && mainHandler != null) mainHandler.post(() -> callback.onDataLoaded(filtered));
                    else if (callback != null) callback.onDataLoaded(filtered);
                })
                .addOnFailureListener(e -> {
                    // Fallback to Room if offline or failure
                    if (appDatabase != null) {
                        executorService.execute(() -> {
                            List<ShelterEntity> entities = appDatabase.shelterDao().getAllActive();
                            if (entities != null && !entities.isEmpty()) {
                                List<Shelter> roomList = new ArrayList<>();
                                for (ShelterEntity entity : entities) {
                                    roomList.add(ModelMapper.toShelter(entity));
                                }
                                List<Shelter> filtered = filterShelters(roomList, query, district, upazila);
                                if (callback != null && mainHandler != null) {
                                    mainHandler.post(() -> callback.onDataLoaded(filtered));
                                }
                                return;
                            }
                            List<Shelter> fallback = filterShelters(getSeedShelters(), query, district, upazila);
                            if (callback != null && mainHandler != null) mainHandler.post(() -> callback.onDataLoaded(fallback));
                            else if (callback != null) callback.onDataLoaded(fallback);
                        });
                    } else {
                        List<Shelter> fallback = filterShelters(getSeedShelters(), query, district, upazila);
                        if (callback != null && mainHandler != null) mainHandler.post(() -> callback.onDataLoaded(fallback));
                        else if (callback != null) callback.onDataLoaded(fallback);
                    }
                });
    }

    public void getUpazilasForDistrict(String district, DataCallback<List<String>> callback) {
        if (appDatabase != null && district != null) {
            executorService.execute(() -> {
                String cleanDistrict = district.toLowerCase().trim().replace(" ", "_");
                List<UpazilaEntity> entities = appDatabase.upazilaDao().getByDistrictId(cleanDistrict);
                if (entities == null || entities.isEmpty()) {
                    DistrictEntity d = appDatabase.districtDao().getByName(district);
                    if (d != null) {
                        entities = appDatabase.upazilaDao().getByDistrictId(d.getId());
                    }
                }

                if (entities != null && !entities.isEmpty()) {
                    List<String> names = new ArrayList<>();
                    for (UpazilaEntity u : entities) {
                        names.add(u.getName());
                    }
                    if (callback != null && mainHandler != null) {
                        mainHandler.post(() -> callback.onDataLoaded(names));
                    }
                    return;
                }

                // Fallback to static district data
                List<String> fallback = DistrictData.getUpazilasForDistrict(district);
                if (callback != null && mainHandler != null) {
                    mainHandler.post(() -> callback.onDataLoaded(fallback));
                }
            });
            return;
        }

        List<String> fallback = DistrictData.getUpazilasForDistrict(district);
        if (callback != null && mainHandler != null) mainHandler.post(() -> callback.onDataLoaded(fallback));
        else if (callback != null) callback.onDataLoaded(fallback);
    }

    private List<Shelter> filterShelters(List<Shelter> list, String query, String district, String upazila) {
        List<Shelter> result = new ArrayList<>();
        boolean isAllDistrict = district == null || district.trim().isEmpty()
                || "All".equalsIgnoreCase(district.trim())
                || "All Districts".equalsIgnoreCase(district.trim());
        boolean isAllUpazila = upazila == null || upazila.trim().isEmpty()
                || "All".equalsIgnoreCase(upazila.trim())
                || "All Upazilas".equalsIgnoreCase(upazila.trim());

        for (Shelter s : list) {
            if (!isAllDistrict && !district.trim().equalsIgnoreCase(s.getDistrict())) {
                continue;
            }
            if (!isAllUpazila && !upazila.trim().equalsIgnoreCase(s.getUpazila())) {
                continue;
            }
            if (query != null && !query.trim().isEmpty()) {
                String q = query.toLowerCase().trim();
                boolean matches = (s.getNameEn() != null && s.getNameEn().toLowerCase().contains(q))
                        || (s.getNameBn() != null && s.getNameBn().toLowerCase().contains(q))
                        || (s.getAddress() != null && s.getAddress().toLowerCase().contains(q))
                        || (s.getDistrict() != null && s.getDistrict().toLowerCase().contains(q))
                        || (s.getUpazila() != null && s.getUpazila().toLowerCase().contains(q));
                if (!matches) continue;
            }
            result.add(s);
        }
        return result;
    }

    public void getShelterById(long id, DataCallback<Shelter> callback) {
        if (!isFirestoreAvailable()) {
            for (Shelter s : getSeedShelters()) {
                if (s.getId() == id) {
                    if (callback != null) mainHandler.post(() -> callback.onDataLoaded(s));
                    return;
                }
            }
            if (callback != null) mainHandler.post(() -> callback.onDataLoaded(null));
            return;
        }

        db.collection(DatabaseContract.ShelterEntry.TABLE_NAME)
                .document("shelter_" + id)
                .get()
                .addOnSuccessListener(doc -> {
                    Shelter shelter = doc.exists() ? mapToShelter(doc) : null;
                    if (callback != null) mainHandler.post(() -> callback.onDataLoaded(shelter));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) mainHandler.post(() -> callback.onError(e));
                });
    }

    public void insertShelter(Shelter shelter, ActionCallback callback) {
        if (!isFirestoreAvailable() || shelter == null) {
            if (callback != null) mainHandler.post(callback::onSuccess);
            return;
        }

        String docId = "shelter_" + (shelter.getId() > 0 ? shelter.getId() : System.currentTimeMillis());
        db.collection(DatabaseContract.ShelterEntry.TABLE_NAME)
                .document(docId)
                .set(shelterToMap(shelter))
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) mainHandler.post(callback::onSuccess);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) mainHandler.post(() -> callback.onError(e));
                });
    }

    public void syncSheltersTransactional(List<Shelter> shelters, String sourceName, SyncCallback callback) {
        if (!isFirestoreAvailable() || shelters == null) {
            if (callback != null) mainHandler.post(() -> callback.onSyncComplete(0, 0));
            return;
        }

        WriteBatch batch = db.batch();
        int count = 0;
        for (Shelter s : shelters) {
            String docId = "shelter_" + (s.getId() > 0 ? s.getId() : System.currentTimeMillis() + count);
            DocumentReference ref = db.collection(DatabaseContract.ShelterEntry.TABLE_NAME).document(docId);
            batch.set(ref, shelterToMap(s), SetOptions.merge());
            count++;
        }

        final int total = count;
        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) mainHandler.post(() -> callback.onSyncComplete(total, 0));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) mainHandler.post(() -> callback.onSyncError(e));
                });
    }

    // =========================================================================
    // ROOM ADMINISTRATIVE HIERARCHY & HOMETOWN QUERIES
    // =========================================================================

    public void getDivisions(DataCallback<List<DivisionEntity>> callback) {
        if (appDatabase == null) {
            if (callback != null && mainHandler != null) mainHandler.post(() -> callback.onDataLoaded(new ArrayList<>()));
            else if (callback != null) callback.onDataLoaded(new ArrayList<>());
            return;
        }
        executorService.execute(() -> {
            List<DivisionEntity> list = appDatabase.divisionDao().getAllActive();
            if (callback != null && mainHandler != null) mainHandler.post(() -> callback.onDataLoaded(list));
            else if (callback != null) callback.onDataLoaded(list);
        });
    }

    public void getDistrictsByDivision(String divisionId, DataCallback<List<DistrictEntity>> callback) {
        if (appDatabase == null) {
            if (callback != null && mainHandler != null) mainHandler.post(() -> callback.onDataLoaded(new ArrayList<>()));
            else if (callback != null) callback.onDataLoaded(new ArrayList<>());
            return;
        }
        executorService.execute(() -> {
            List<DistrictEntity> list = (divisionId == null || divisionId.isEmpty() || "All".equalsIgnoreCase(divisionId))
                    ? appDatabase.districtDao().getAllActive()
                    : appDatabase.districtDao().getByDivisionId(divisionId.toLowerCase().trim());
            if (callback != null && mainHandler != null) mainHandler.post(() -> callback.onDataLoaded(list));
            else if (callback != null) callback.onDataLoaded(list);
        });
    }

    public void getAllDistricts(DataCallback<List<DistrictEntity>> callback) {
        getDistrictsByDivision(null, callback);
    }

    public void getUpazilas(String districtId, DataCallback<List<UpazilaEntity>> callback) {
        if (appDatabase == null) {
            if (callback != null && mainHandler != null) mainHandler.post(() -> callback.onDataLoaded(new ArrayList<>()));
            else if (callback != null) callback.onDataLoaded(new ArrayList<>());
            return;
        }
        executorService.execute(() -> {
            List<UpazilaEntity> list = (districtId == null || districtId.isEmpty() || "All".equalsIgnoreCase(districtId))
                    ? appDatabase.upazilaDao().getAllActive()
                    : appDatabase.upazilaDao().getByDistrictId(districtId.toLowerCase().trim());
            if (callback != null && mainHandler != null) mainHandler.post(() -> callback.onDataLoaded(list));
            else if (callback != null) callback.onDataLoaded(list);
        });
    }

    public void getSheltersByHometown(String divisionId, String districtId, String upazilaId, DataCallback<List<Shelter>> callback) {
        searchShelters(null, districtId, upazilaId, callback);
    }

    public void getDisasterUpdatesByHometown(String divisionId, String districtId, String upazilaId, DataCallback<List<FloodWarning>> callback) {
        if (appDatabase == null) {
            getFloodWarnings(districtId, callback);
            return;
        }
        executorService.execute(() -> {
            List<DisasterUpdateEntity> entities = appDatabase.disasterUpdateDao().getByHometown(
                    divisionId != null ? divisionId.toLowerCase().trim() : "",
                    districtId != null ? districtId.toLowerCase().trim() : "",
                    upazilaId != null ? upazilaId.toLowerCase().trim() : ""
            );
            List<FloodWarning> list = new ArrayList<>();
            for (DisasterUpdateEntity entity : entities) {
                list.add(ModelMapper.toFloodWarning(entity));
            }
            if (list.isEmpty()) {
                if (mainHandler != null) {
                    mainHandler.post(() -> getFloodWarnings(districtId, callback));
                } else {
                    getFloodWarnings(districtId, callback);
                }
            } else {
                if (callback != null && mainHandler != null) mainHandler.post(() -> callback.onDataLoaded(list));
                else if (callback != null) callback.onDataLoaded(list);
            }
        });
    }

    // =========================================================================
    // 5. WATER OBSERVATIONS REPOSITORY (FULL FIRESTORE PERSISTENCE)
    // =========================================================================

    public void saveWaterObservation(WaterObservation obs, ActionCallback callback) {
        if (!isFirestoreAvailable() || obs == null) {
            if (callback != null) mainHandler.post(callback::onSuccess);
            return;
        }

        long id = obs.getId() > 0 ? obs.getId() : System.currentTimeMillis();
        String uid = (obs.getUserId() != null && !obs.getUserId().trim().isEmpty())
                ? obs.getUserId()
                : com.jibonshield.bd.firebase.FirebaseAuthManager.getInstance(context).getCurrentUserId();

        WaterObservation prepared = new WaterObservation(
                id, obs.getLocationName(), obs.getDistrict(), obs.getSourceType(),
                obs.getAppearance(), obs.getOdor(), obs.getPhLevel(), obs.getTurbidityStatus(),
                obs.getSafetyStatus(), obs.getObservationTime(), obs.getReportedBy(),
                obs.getRemarks(), obs.getImageUri(), obs.getImageUrl(),
                WaterObservation.SYNC_SYNCED,
                uid,
                obs.getCreatedAt() > 0 ? obs.getCreatedAt() : System.currentTimeMillis(),
                System.currentTimeMillis()
        );

        String docId = "obs_" + id;
        db.collection(DatabaseContract.WaterObservationEntry.TABLE_NAME)
                .document(docId)
                .set(waterObservationToMap(prepared))
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) mainHandler.post(callback::onSuccess);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) mainHandler.post(() -> callback.onError(e));
                });
    }

    public void insertWaterObservation(WaterObservation obs, ActionCallback callback) {
        saveWaterObservation(obs, callback);
    }

    public void updateWaterObservation(WaterObservation obs, ActionCallback callback) {
        String currentUid = com.jibonshield.bd.firebase.FirebaseAuthManager.getInstance(context).getCurrentUserId();
        String currentEmail = com.jibonshield.bd.utils.PreferenceUtils.getUserEmail(context);
        String currentName = com.jibonshield.bd.utils.PreferenceUtils.getUserName(context);
        if (obs != null && !obs.isCreatedBy(currentUid, currentName, currentEmail)) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError(new SecurityException("Unauthorized: Only the creator of this observation can edit it.")));
            }
            return;
        }
        saveWaterObservation(obs, callback);
    }

    public void deleteWaterObservation(long id, ActionCallback callback) {
        String currentUid = com.jibonshield.bd.firebase.FirebaseAuthManager.getInstance(context).getCurrentUserId();
        String currentEmail = com.jibonshield.bd.utils.PreferenceUtils.getUserEmail(context);
        String currentName = com.jibonshield.bd.utils.PreferenceUtils.getUserName(context);
        deleteWaterObservation(id, currentUid, currentName, currentEmail, callback);
    }

    public void deleteWaterObservation(long id, String requestorUid, String requestorName, String requestorEmail, ActionCallback callback) {
        if (!isFirestoreAvailable()) {
            if (callback != null) mainHandler.post(callback::onSuccess);
            return;
        }

        DocumentReference docRef = db.collection(DatabaseContract.WaterObservationEntry.TABLE_NAME)
                .document("obs_" + id);

        docRef.get().addOnSuccessListener(doc -> {
            if (doc != null && doc.exists()) {
                WaterObservation obs = mapToWaterObservation(doc);
                if (obs != null && !obs.isCreatedBy(requestorUid, requestorName, requestorEmail)) {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onError(new SecurityException("Unauthorized: Only the creator of this observation can delete it.")));
                    }
                    return;
                }
            }

            docRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        if (callback != null) mainHandler.post(callback::onSuccess);
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) mainHandler.post(() -> callback.onError(e));
                    });
        }).addOnFailureListener(e -> {
            if (callback != null) mainHandler.post(() -> callback.onError(e));
        });
    }

    public void getWaterObservationById(long id, DataCallback<WaterObservation> callback) {
        if (!isFirestoreAvailable()) {
            if (callback != null) mainHandler.post(() -> callback.onDataLoaded(null));
            return;
        }

        db.collection(DatabaseContract.WaterObservationEntry.TABLE_NAME)
                .document("obs_" + id)
                .get()
                .addOnSuccessListener(doc -> {
                    WaterObservation obs = doc.exists() ? mapToWaterObservation(doc) : null;
                    if (callback != null) mainHandler.post(() -> callback.onDataLoaded(obs));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) mainHandler.post(() -> callback.onError(e));
                });
    }

    public void getWaterObservations(DataCallback<List<WaterObservation>> callback) {
        getAllWaterObservations(callback);
    }

    public void getAllWaterObservations(DataCallback<List<WaterObservation>> callback) {
        if (!isFirestoreAvailable()) {
            if (callback != null) mainHandler.post(() -> callback.onDataLoaded(new ArrayList<>()));
            return;
        }

        db.collection(DatabaseContract.WaterObservationEntry.TABLE_NAME)
                .orderBy(DatabaseContract.WaterObservationEntry.COLUMN_CREATED_AT, Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<WaterObservation> list = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            WaterObservation obs = mapToWaterObservation(doc);
                            if (obs != null) list.add(obs);
                        }
                    }
                    if (callback != null) mainHandler.post(() -> callback.onDataLoaded(list));
                })
                .addOnFailureListener(e -> {
                    // Fallback to unordered query in case indices are building
                    db.collection(DatabaseContract.WaterObservationEntry.TABLE_NAME)
                            .get()
                            .addOnSuccessListener(docs -> {
                                List<WaterObservation> list = new ArrayList<>();
                                if (docs != null) {
                                    for (DocumentSnapshot doc : docs) {
                                        WaterObservation obs = mapToWaterObservation(doc);
                                        if (obs != null) list.add(obs);
                                    }
                                }
                                if (callback != null) mainHandler.post(() -> callback.onDataLoaded(list));
                            })
                            .addOnFailureListener(err -> {
                                if (callback != null) mainHandler.post(() -> callback.onError(err));
                            });
                });
    }

    public void getWaterObservationsByDistrict(String district, DataCallback<List<WaterObservation>> callback) {
        if (!isFirestoreAvailable()) {
            if (callback != null) mainHandler.post(() -> callback.onDataLoaded(new ArrayList<>()));
            return;
        }

        Query query = db.collection(DatabaseContract.WaterObservationEntry.TABLE_NAME);
        if (district != null && !district.isEmpty() && !district.equalsIgnoreCase("All")) {
            query = query.whereEqualTo(DatabaseContract.WaterObservationEntry.COLUMN_DISTRICT, district);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<WaterObservation> list = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            WaterObservation obs = mapToWaterObservation(doc);
                            if (obs != null) list.add(obs);
                        }
                    }
                    if (callback != null) mainHandler.post(() -> callback.onDataLoaded(list));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) mainHandler.post(() -> callback.onError(e));
                });
    }

    public void filterWaterObservations(String query, String district, DataCallback<List<WaterObservation>> callback) {
        getAllWaterObservations(new DataCallback<List<WaterObservation>>() {
            @Override
            public void onDataLoaded(List<WaterObservation> data) {
                List<WaterObservation> filtered = new ArrayList<>();
                if (data != null) {
                    for (WaterObservation obs : data) {
                        if (district != null && !district.isEmpty() && !district.equalsIgnoreCase("All")
                                && !district.equalsIgnoreCase(obs.getDistrict())) {
                            continue;
                        }
                        if (query != null && !query.trim().isEmpty()) {
                            String q = query.toLowerCase().trim();
                            boolean matches = (obs.getLocationName() != null && obs.getLocationName().toLowerCase().contains(q))
                                    || (obs.getReportedBy() != null && obs.getReportedBy().toLowerCase().contains(q))
                                    || (obs.getSourceType() != null && obs.getSourceType().toLowerCase().contains(q));
                            if (!matches) continue;
                        }
                        filtered.add(obs);
                    }
                }
                if (callback != null) callback.onDataLoaded(filtered);
            }

            @Override
            public void onError(Exception error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void getPendingWaterObservations(DataCallback<List<WaterObservation>> callback) {
        if (!isFirestoreAvailable()) {
            if (callback != null) mainHandler.post(() -> callback.onDataLoaded(new ArrayList<>()));
            return;
        }

        db.collection(DatabaseContract.WaterObservationEntry.TABLE_NAME)
                .whereEqualTo(DatabaseContract.WaterObservationEntry.COLUMN_SYNC_STATUS, WaterObservation.SYNC_PENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<WaterObservation> list = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            WaterObservation obs = mapToWaterObservation(doc);
                            if (obs != null) list.add(obs);
                        }
                    }
                    if (callback != null) mainHandler.post(() -> callback.onDataLoaded(list));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) mainHandler.post(() -> callback.onError(e));
                });
    }

    public void markWaterObservationSynced(long id, String cloudImageUrl, ActionCallback callback) {
        updateWaterObservationSyncStatus(id, WaterObservation.SYNC_SYNCED, cloudImageUrl, callback);
    }

    public void updateWaterObservationSyncStatus(long id, String syncStatus, String cloudImageUrl, ActionCallback callback) {
        if (!isFirestoreAvailable()) {
            if (callback != null) mainHandler.post(callback::onSuccess);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(DatabaseContract.WaterObservationEntry.COLUMN_SYNC_STATUS, syncStatus != null ? syncStatus : WaterObservation.SYNC_SYNCED);
        if (cloudImageUrl != null) {
            updates.put(DatabaseContract.WaterObservationEntry.COLUMN_IMAGE_URL, cloudImageUrl);
        }
        updates.put(DatabaseContract.WaterObservationEntry.COLUMN_UPDATED_AT, System.currentTimeMillis());

        db.collection(DatabaseContract.WaterObservationEntry.TABLE_NAME)
                .document("obs_" + id)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) mainHandler.post(callback::onSuccess);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) mainHandler.post(() -> callback.onError(e));
                });
    }

    // =========================================================================
    // 6. PREPAREDNESS CHECKLISTS & ITEMS
    // =========================================================================

    public void getChecklists(DataCallback<List<PreparednessChecklist>> callback) {
        if (!isFirestoreAvailable()) {
            if (callback != null) mainHandler.post(() -> callback.onDataLoaded(getSeedChecklists()));
            return;
        }

        db.collection(DatabaseContract.PreparednessChecklistEntry.TABLE_NAME)
                .orderBy(DatabaseContract.PreparednessChecklistEntry.COLUMN_DISPLAY_ORDER, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<PreparednessChecklist> list = new ArrayList<>();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            PreparednessChecklist cl = mapToChecklist(doc);
                            if (cl != null) list.add(cl);
                        }
                    }
                    if (list.isEmpty()) {
                        list = getSeedChecklists();
                    }
                    final List<PreparednessChecklist> finalList = list;
                    if (callback != null) mainHandler.post(() -> callback.onDataLoaded(finalList));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) mainHandler.post(() -> callback.onDataLoaded(getSeedChecklists()));
                });
    }

    public void getChecklistItems(DataCallback<List<ChecklistItem>> callback) {
        if (!isFirestoreAvailable()) {
            List<ChecklistItem> all = new ArrayList<>();
            all.addAll(filterSeedChecklistItems(1));
            all.addAll(filterSeedChecklistItems(2));
            all.addAll(filterSeedChecklistItems(3));
            all.addAll(filterSeedChecklistItems(4));
            if (callback != null) mainHandler.post(() -> callback.onDataLoaded(all));
            return;
        }

        db.collection(DatabaseContract.ChecklistItemEntry.TABLE_NAME)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ChecklistItem> list = new ArrayList<>();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            ChecklistItem item = mapToChecklistItem(doc);
                            if (item != null) list.add(item);
                        }
                    }
                    if (list.isEmpty()) {
                        list.addAll(filterSeedChecklistItems(1));
                        list.addAll(filterSeedChecklistItems(2));
                        list.addAll(filterSeedChecklistItems(3));
                        list.addAll(filterSeedChecklistItems(4));
                    }
                    final List<ChecklistItem> finalList = list;
                    if (callback != null) mainHandler.post(() -> callback.onDataLoaded(finalList));
                })
                .addOnFailureListener(e -> {
                    List<ChecklistItem> all = new ArrayList<>();
                    all.addAll(filterSeedChecklistItems(1));
                    all.addAll(filterSeedChecklistItems(2));
                    all.addAll(filterSeedChecklistItems(3));
                    all.addAll(filterSeedChecklistItems(4));
                    if (callback != null) mainHandler.post(() -> callback.onDataLoaded(all));
                });
    }

    public void getChecklistItems(long checklistId, DataCallback<List<ChecklistItem>> callback) {
        if (!isFirestoreAvailable()) {
            if (callback != null) mainHandler.post(() -> callback.onDataLoaded(filterSeedChecklistItems(checklistId)));
            return;
        }

        db.collection(DatabaseContract.ChecklistItemEntry.TABLE_NAME)
                .whereEqualTo(DatabaseContract.ChecklistItemEntry.COLUMN_CHECKLIST_ID, checklistId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ChecklistItem> list = new ArrayList<>();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            ChecklistItem item = mapToChecklistItem(doc);
                            if (item != null) list.add(item);
                        }
                    }
                    if (list.isEmpty()) {
                        list = filterSeedChecklistItems(checklistId);
                    }
                    final List<ChecklistItem> finalList = list;
                    if (callback != null) mainHandler.post(() -> callback.onDataLoaded(finalList));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) mainHandler.post(() -> callback.onDataLoaded(filterSeedChecklistItems(checklistId)));
                });
    }

    public void updateChecklistItem(long itemId, boolean completed, ActionCallback callback) {
        updateChecklistItemStatus(itemId, completed, callback);
    }

    public void updateChecklistItemStatus(long itemId, boolean completed, ActionCallback callback) {
        if (!isFirestoreAvailable()) {
            if (callback != null) mainHandler.post(callback::onSuccess);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(DatabaseContract.ChecklistItemEntry.COLUMN_IS_COMPLETED, completed ? 1 : 0);
        updates.put(DatabaseContract.ChecklistItemEntry.COLUMN_UPDATED_AT, System.currentTimeMillis());

        db.collection(DatabaseContract.ChecklistItemEntry.TABLE_NAME)
                .document("item_" + itemId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) mainHandler.post(callback::onSuccess);
                })
                .addOnFailureListener(e -> {
                    // Try Set if document is new
                    db.collection(DatabaseContract.ChecklistItemEntry.TABLE_NAME)
                            .document("item_" + itemId)
                            .set(updates, SetOptions.merge())
                            .addOnSuccessListener(v -> {
                                if (callback != null) mainHandler.post(callback::onSuccess);
                            })
                            .addOnFailureListener(err -> {
                                if (callback != null) mainHandler.post(() -> callback.onError(err));
                            });
                });
    }

    public void resetAllChecklistItems(ActionCallback callback) {
        if (!isFirestoreAvailable()) {
            if (callback != null) mainHandler.post(callback::onSuccess);
            return;
        }

        db.collection(DatabaseContract.ChecklistItemEntry.TABLE_NAME)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots == null || queryDocumentSnapshots.isEmpty()) {
                        if (callback != null) mainHandler.post(callback::onSuccess);
                        return;
                    }

                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put(DatabaseContract.ChecklistItemEntry.COLUMN_IS_COMPLETED, 0);
                        updates.put(DatabaseContract.ChecklistItemEntry.COLUMN_UPDATED_AT, System.currentTimeMillis());
                        batch.update(doc.getReference(), updates);
                    }

                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                if (callback != null) mainHandler.post(callback::onSuccess);
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) mainHandler.post(() -> callback.onError(e));
                            });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) mainHandler.post(() -> callback.onError(e));
                });
    }

    public void getChecklistProgress(long checklistId, DataCallback<ChecklistProgress> callback) {
        getChecklistItems(checklistId, new DataCallback<List<ChecklistItem>>() {
            @Override
            public void onDataLoaded(List<ChecklistItem> items) {
                int total = items != null ? items.size() : 0;
                int completed = 0;
                int essentialTotal = 0;
                int essentialCompleted = 0;
                if (items != null) {
                    for (ChecklistItem it : items) {
                        if (it.isEssential()) {
                            essentialTotal++;
                            if (it.isCompleted()) essentialCompleted++;
                        }
                        if (it.isCompleted()) completed++;
                    }
                }
                ChecklistProgress progress = new ChecklistProgress(total, completed, essentialTotal, essentialCompleted);
                if (callback != null) callback.onDataLoaded(progress);
            }

            @Override
            public void onError(Exception error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    // =========================================================================
    // 7. SYNCHRONIZATION METADATA
    // =========================================================================

    public void getSyncMetadata(String tableName, DataCallback<SyncMetadata> callback) {
        if (!isFirestoreAvailable()) {
            SyncMetadata defaultMeta = new SyncMetadata(
                    tableName != null ? tableName : "all",
                    "MoDMR / Disaster Management Gateways",
                    System.currentTimeMillis(),
                    7,
                    "SUCCESS",
                    "hash-firestore-live",
                    "2026.FIRESTORE"
            );
            if (callback != null) mainHandler.post(() -> callback.onDataLoaded(defaultMeta));
            return;
        }

        String docId = tableName != null ? tableName : "default_sync";
        db.collection(DatabaseContract.SyncMetadataEntry.TABLE_NAME)
                .document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    SyncMetadata meta = null;
                    if (doc.exists()) {
                        String tbl = doc.getString(DatabaseContract.SyncMetadataEntry.COLUMN_TABLE_NAME);
                        String src = doc.getString(DatabaseContract.SyncMetadataEntry.COLUMN_SOURCE_NAME);
                        Long time = doc.getLong(DatabaseContract.SyncMetadataEntry.COLUMN_LAST_SYNC_TIMESTAMP);
                        Long count = doc.getLong(DatabaseContract.SyncMetadataEntry.COLUMN_RECORDS_COUNT);
                        String status = doc.getString(DatabaseContract.SyncMetadataEntry.COLUMN_SYNC_STATUS);
                        String hash = doc.getString(DatabaseContract.SyncMetadataEntry.COLUMN_SYNC_HASH);
                        String ver = doc.getString(DatabaseContract.SyncMetadataEntry.COLUMN_VERSION_TAG);

                        meta = new SyncMetadata(
                                tbl != null ? tbl : docId,
                                src != null ? src : "MoDMR / FFWC Cloud Feeds",
                                time != null ? time : System.currentTimeMillis(),
                                count != null ? count.intValue() : 0,
                                status != null ? status : "SUCCESS",
                                hash != null ? hash : "hash-live",
                                ver != null ? ver : "2026.ONLINE"
                        );
                    } else {
                        meta = new SyncMetadata(docId, "MoDMR / FFWC Cloud Feeds", System.currentTimeMillis(), 7, "SUCCESS", "hash-default", "2026.ONLINE");
                    }
                    final SyncMetadata result = meta;
                    if (callback != null) mainHandler.post(() -> callback.onDataLoaded(result));
                })
                .addOnFailureListener(e -> {
                    SyncMetadata fallback = new SyncMetadata(docId, "MoDMR / FFWC Cloud Feeds", System.currentTimeMillis(), 7, "SUCCESS", "hash-default", "2026.ONLINE");
                    if (callback != null) mainHandler.post(() -> callback.onDataLoaded(fallback));
                });
    }

    public void getAllSyncMetadata(DataCallback<List<SyncMetadata>> callback) {
        if (!isFirestoreAvailable()) {
            List<SyncMetadata> list = new ArrayList<>();
            list.add(new SyncMetadata(DatabaseContract.EmergencyContactEntry.TABLE_NAME, "BTRC / MoDMR Hotlines", System.currentTimeMillis(), 7, "SUCCESS", "hash-contacts", "2026.FIRESTORE"));
            list.add(new SyncMetadata(DatabaseContract.FloodWarningEntry.TABLE_NAME, "FFWC Bulletins", System.currentTimeMillis(), 4, "SUCCESS", "hash-warnings", "2026.FIRESTORE"));
            list.add(new SyncMetadata(DatabaseContract.ShelterEntry.TABLE_NAME, "MoDMR / CPP Shelters", System.currentTimeMillis(), 7, "SUCCESS", "hash-shelters", "2026.FIRESTORE"));
            list.add(new SyncMetadata(DatabaseContract.PreparednessChecklistEntry.TABLE_NAME, "Resilience Guide", System.currentTimeMillis(), 4, "SUCCESS", "hash-checklists", "2026.FIRESTORE"));
            list.add(new SyncMetadata(DatabaseContract.WaterObservationEntry.TABLE_NAME, "Field Reports", System.currentTimeMillis(), 0, "SUCCESS", "hash-obs", "2026.FIRESTORE"));
            if (callback != null) mainHandler.post(() -> callback.onDataLoaded(list));
            return;
        }

        db.collection(DatabaseContract.SyncMetadataEntry.TABLE_NAME)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<SyncMetadata> list = new ArrayList<>();
                    if (queryDocumentSnapshots != null) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String tbl = doc.getString(DatabaseContract.SyncMetadataEntry.COLUMN_TABLE_NAME);
                            String src = doc.getString(DatabaseContract.SyncMetadataEntry.COLUMN_SOURCE_NAME);
                            Long time = doc.getLong(DatabaseContract.SyncMetadataEntry.COLUMN_LAST_SYNC_TIMESTAMP);
                            Long count = doc.getLong(DatabaseContract.SyncMetadataEntry.COLUMN_RECORDS_COUNT);
                            String status = doc.getString(DatabaseContract.SyncMetadataEntry.COLUMN_SYNC_STATUS);
                            String hash = doc.getString(DatabaseContract.SyncMetadataEntry.COLUMN_SYNC_HASH);
                            String ver = doc.getString(DatabaseContract.SyncMetadataEntry.COLUMN_VERSION_TAG);

                            list.add(new SyncMetadata(
                                    tbl != null ? tbl : doc.getId(),
                                    src != null ? src : "Cloud Feeds",
                                    time != null ? time : System.currentTimeMillis(),
                                    count != null ? count.intValue() : 0,
                                    status != null ? status : "SUCCESS",
                                    hash != null ? hash : "hash-cloud",
                                    ver != null ? ver : "2026.ONLINE"
                            ));
                        }
                    }
                    if (list.isEmpty()) {
                        list.add(new SyncMetadata(DatabaseContract.EmergencyContactEntry.TABLE_NAME, "BTRC / MoDMR Hotlines", System.currentTimeMillis(), 7, "SUCCESS", "hash-contacts", "2026.FIRESTORE"));
                        list.add(new SyncMetadata(DatabaseContract.FloodWarningEntry.TABLE_NAME, "FFWC Bulletins", System.currentTimeMillis(), 4, "SUCCESS", "hash-warnings", "2026.FIRESTORE"));
                        list.add(new SyncMetadata(DatabaseContract.ShelterEntry.TABLE_NAME, "MoDMR / CPP Shelters", System.currentTimeMillis(), 7, "SUCCESS", "hash-shelters", "2026.FIRESTORE"));
                        list.add(new SyncMetadata(DatabaseContract.PreparednessChecklistEntry.TABLE_NAME, "Resilience Guide", System.currentTimeMillis(), 4, "SUCCESS", "hash-checklists", "2026.FIRESTORE"));
                        list.add(new SyncMetadata(DatabaseContract.WaterObservationEntry.TABLE_NAME, "Field Reports", System.currentTimeMillis(), 0, "SUCCESS", "hash-obs", "2026.FIRESTORE"));
                    }
                    final List<SyncMetadata> finalList = list;
                    if (callback != null) mainHandler.post(() -> callback.onDataLoaded(finalList));
                })
                .addOnFailureListener(e -> {
                    if (callback != null) mainHandler.post(() -> callback.onError(e));
                });
    }

    public void updateSyncMetadata(SyncMetadata meta, ActionCallback callback) {
        if (!isFirestoreAvailable() || meta == null) {
            if (callback != null) mainHandler.post(callback::onSuccess);
            return;
        }

        Map<String, Object> map = new HashMap<>();
        map.put(DatabaseContract.SyncMetadataEntry.COLUMN_TABLE_NAME, meta.getTableName());
        map.put(DatabaseContract.SyncMetadataEntry.COLUMN_SOURCE_NAME, meta.getSourceName());
        map.put(DatabaseContract.SyncMetadataEntry.COLUMN_LAST_SYNC_TIMESTAMP, meta.getLastSyncTimestamp());
        map.put(DatabaseContract.SyncMetadataEntry.COLUMN_RECORDS_COUNT, meta.getRecordsCount());
        map.put(DatabaseContract.SyncMetadataEntry.COLUMN_SYNC_STATUS, meta.getSyncStatus());
        map.put(DatabaseContract.SyncMetadataEntry.COLUMN_SYNC_HASH, meta.getSyncHash());
        map.put(DatabaseContract.SyncMetadataEntry.COLUMN_VERSION_TAG, meta.getVersionTag());

        db.collection(DatabaseContract.SyncMetadataEntry.TABLE_NAME)
                .document(meta.getTableName())
                .set(map, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) mainHandler.post(callback::onSuccess);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) mainHandler.post(() -> callback.onError(e));
                });
    }

    // =========================================================================
    // HELPER MAPPERS (OBJECT <-> MAP)
    // =========================================================================

    private Map<String, Object> emergencyContactToMap(EmergencyContact c) {
        Map<String, Object> map = new HashMap<>();
        map.put(DatabaseContract.EmergencyContactEntry._ID, c.getId());
        map.put(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_EN, c.getServiceNameEn());
        map.put(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_BN, c.getServiceNameBn());
        map.put(DatabaseContract.EmergencyContactEntry.COLUMN_PHONE_NUMBER, c.getPhoneNumber());
        map.put(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_DESCRIPTION_EN, c.getServiceDescriptionEn());
        map.put(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_DESCRIPTION_BN, c.getServiceDescriptionBn());
        map.put(DatabaseContract.EmergencyContactEntry.COLUMN_CATEGORY, c.getCategory());
        map.put(DatabaseContract.EmergencyContactEntry.COLUMN_AVAILABLE_HOURS, c.getAvailableHours());
        map.put(DatabaseContract.EmergencyContactEntry.COLUMN_IS_TOLL_FREE, c.isTollFree() ? 1 : 0);
        map.put(DatabaseContract.EmergencyContactEntry.COLUMN_SOURCE, c.getSource());
        map.put(DatabaseContract.EmergencyContactEntry.COLUMN_VERIFICATION_DATE, c.getVerificationDate());
        map.put(DatabaseContract.EmergencyContactEntry.COLUMN_IS_ENABLED, c.isEnabled() ? 1 : 0);
        map.put(DatabaseContract.EmergencyContactEntry.COLUMN_PRIORITY_ORDER, c.getPriorityOrder());
        return map;
    }

    private EmergencyContact mapToEmergencyContact(DocumentSnapshot doc) {
        try {
            Long idLong = doc.getLong(DatabaseContract.EmergencyContactEntry._ID);
            long id = idLong != null ? idLong : parseDocIdToLong(doc.getId());
            String nameEn = doc.getString(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_EN);
            String nameBn = doc.getString(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_BN);
            String phone = doc.getString(DatabaseContract.EmergencyContactEntry.COLUMN_PHONE_NUMBER);
            String descEn = doc.getString(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_DESCRIPTION_EN);
            String descBn = doc.getString(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_DESCRIPTION_BN);
            String cat = doc.getString(DatabaseContract.EmergencyContactEntry.COLUMN_CATEGORY);
            String hours = doc.getString(DatabaseContract.EmergencyContactEntry.COLUMN_AVAILABLE_HOURS);
            Long tollFreeLong = doc.getLong(DatabaseContract.EmergencyContactEntry.COLUMN_IS_TOLL_FREE);
            boolean tollFree = tollFreeLong != null && tollFreeLong == 1;
            String source = doc.getString(DatabaseContract.EmergencyContactEntry.COLUMN_SOURCE);
            String verDate = doc.getString(DatabaseContract.EmergencyContactEntry.COLUMN_VERIFICATION_DATE);
            Long enabledLong = doc.getLong(DatabaseContract.EmergencyContactEntry.COLUMN_IS_ENABLED);
            boolean enabled = enabledLong == null || enabledLong == 1;
            Long priorityLong = doc.getLong(DatabaseContract.EmergencyContactEntry.COLUMN_PRIORITY_ORDER);
            int priority = priorityLong != null ? priorityLong.intValue() : 0;

            return new EmergencyContact(id, nameEn != null ? nameEn : "", nameBn != null ? nameBn : "",
                    phone != null ? phone : "", descEn, descBn, cat != null ? cat : "GENERAL",
                    hours != null ? hours : "24/7", tollFree, source, verDate, enabled, priority);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> floodWarningToMap(FloodWarning w) {
        Map<String, Object> map = new HashMap<>();
        map.put(DatabaseContract.FloodWarningEntry._ID, w.getId());
        map.put(DatabaseContract.FloodWarningEntry.COLUMN_WARNING_LEVEL, w.getWarningLevel());
        map.put(DatabaseContract.FloodWarningEntry.COLUMN_TITLE_EN, w.getTitleEn());
        map.put(DatabaseContract.FloodWarningEntry.COLUMN_TITLE_BN, w.getTitleBn());
        map.put(DatabaseContract.FloodWarningEntry.COLUMN_REGION, w.getRegion());
        map.put(DatabaseContract.FloodWarningEntry.COLUMN_DETAILS_EN, w.getDetailsEn());
        map.put(DatabaseContract.FloodWarningEntry.COLUMN_DETAILS_BN, w.getDetailsBn());
        map.put(DatabaseContract.FloodWarningEntry.COLUMN_ISSUED_AT, w.getIssuedAt());
        map.put(DatabaseContract.FloodWarningEntry.COLUMN_EXPIRES_AT, w.getExpiresAt());
        map.put(DatabaseContract.FloodWarningEntry.COLUMN_SOURCE, w.getSource());
        map.put(DatabaseContract.FloodWarningEntry.COLUMN_SOURCE_REFERENCE, w.getSourceReference());
        map.put(DatabaseContract.FloodWarningEntry.COLUMN_IS_ACTIVE, w.isActive() ? 1 : 0);
        map.put(DatabaseContract.FloodWarningEntry.COLUMN_IS_SAMPLE, w.isSample() ? 1 : 0);
        map.put(DatabaseContract.FloodWarningEntry.COLUMN_LAST_SYNC_TIMESTAMP, w.getLastSyncedAt());
        return map;
    }

    public Map<String, Object> disasterUpdateToMap(DisasterUpdateEntity entity) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", entity.getId());
        map.put("title", entity.getTitle());
        map.put("titleBn", entity.getTitleBn() != null ? entity.getTitleBn() : "");
        map.put("description", entity.getDescription());
        map.put("descriptionBn", entity.getDescriptionBn() != null ? entity.getDescriptionBn() : "");
        map.put("disasterType", entity.getDisasterType() != null ? entity.getDisasterType() : "Flood");
        map.put("severity", entity.getSeverity() != null ? entity.getSeverity() : "Moderate");
        map.put("status", entity.getStatus() != null ? entity.getStatus() : "Active");
        map.put("divisionId", entity.getDivisionId() != null ? entity.getDivisionId() : "");
        map.put("districtId", entity.getDistrictId() != null ? entity.getDistrictId() : "");
        map.put("upazilaId", entity.getUpazilaId() != null ? entity.getUpazilaId() : "");
        map.put("affectedAreas", entity.getAffectedAreas() != null ? entity.getAffectedAreas() : new ArrayList<>());
        map.put("issuedBy", entity.getIssuedBy() != null ? entity.getIssuedBy() : "JibonShield Emergency Ops");
        map.put("issuedAt", new com.google.firebase.Timestamp(new java.util.Date(entity.getIssuedAt() > 0 ? entity.getIssuedAt() : System.currentTimeMillis())));
        map.put("expiresAt", new com.google.firebase.Timestamp(new java.util.Date(entity.getExpiresAt() > 0 ? entity.getExpiresAt() : System.currentTimeMillis() + 86400000L * 4)));
        map.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        map.put("isActive", entity.isActive());
        return map;
    }

    private long extractTimestamp(DocumentSnapshot doc, String field) {
        if (doc == null || field == null) return 0L;
        Object val = doc.get(field);
        if (val instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) val).toDate().getTime();
        } else if (val instanceof Number) {
            return ((Number) val).longValue();
        } else if (val instanceof java.util.Date) {
            return ((java.util.Date) val).getTime();
        }
        return 0L;
    }

    private FloodWarning mapToFloodWarning(DocumentSnapshot doc) {
        try {
            Long idLong = doc.getLong(DatabaseContract.FloodWarningEntry._ID);
            long id = idLong != null ? idLong : parseDocIdToLong(doc.getId());

            String level = doc.getString("severity");
            if (level == null) level = doc.getString(DatabaseContract.FloodWarningEntry.COLUMN_WARNING_LEVEL);

            String titleEn = doc.getString("title");
            if (titleEn == null) titleEn = doc.getString(DatabaseContract.FloodWarningEntry.COLUMN_TITLE_EN);

            String titleBn = doc.getString("titleBn");
            if (titleBn == null) titleBn = doc.getString(DatabaseContract.FloodWarningEntry.COLUMN_TITLE_BN);

            String region = doc.getString(DatabaseContract.FloodWarningEntry.COLUMN_REGION);
            if (region == null) {
                String dist = doc.getString("districtId");
                String upz = doc.getString("upazilaId");
                region = (upz != null && !upz.isEmpty() ? upz + ", " : "") + (dist != null ? dist : "Bangladesh");
            }

            String detailsEn = doc.getString("description");
            if (detailsEn == null) detailsEn = doc.getString(DatabaseContract.FloodWarningEntry.COLUMN_DETAILS_EN);

            String detailsBn = doc.getString("descriptionBn");
            if (detailsBn == null) detailsBn = doc.getString(DatabaseContract.FloodWarningEntry.COLUMN_DETAILS_BN);

            long issuedTime = extractTimestamp(doc, "issuedAt");
            String issued = doc.getString(DatabaseContract.FloodWarningEntry.COLUMN_ISSUED_AT);
            if (issued == null && issuedTime > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                issued = sdf.format(new java.util.Date(issuedTime)) + " BST";
            } else if (issued == null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                issued = sdf.format(new java.util.Date()) + " BST";
            }

            long expiresTime = extractTimestamp(doc, "expiresAt");
            String expires = doc.getString(DatabaseContract.FloodWarningEntry.COLUMN_EXPIRES_AT);
            if (expires == null && expiresTime > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                expires = sdf.format(new java.util.Date(expiresTime)) + " BST";
            } else if (expires == null) {
                long defaultExpires = (issuedTime > 0 ? issuedTime : System.currentTimeMillis()) + 7L * 86400000L;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                expires = sdf.format(new java.util.Date(defaultExpires)) + " BST";
            }

            String source = doc.getString("issuedBy");
            if (source == null) source = doc.getString(DatabaseContract.FloodWarningEntry.COLUMN_SOURCE);

            String sourceRef = doc.getString(DatabaseContract.FloodWarningEntry.COLUMN_SOURCE_REFERENCE);
            if (sourceRef == null) sourceRef = doc.getId();

            Boolean activeBool = doc.getBoolean("isActive");
            Long activeLong = doc.getLong(DatabaseContract.FloodWarningEntry.COLUMN_IS_ACTIVE);
            boolean active = activeBool != null ? activeBool : (activeLong == null || activeLong == 1);

            Long sampleLong = doc.getLong(DatabaseContract.FloodWarningEntry.COLUMN_IS_SAMPLE);
            boolean sample = sampleLong != null && sampleLong == 1;

            long syncTime = extractTimestamp(doc, "updatedAt");
            long sync = syncTime > 0 ? syncTime : System.currentTimeMillis();

            return new FloodWarning(id, level != null ? level.toUpperCase() : "NORMAL", titleEn != null ? titleEn : "",
                    titleBn != null ? titleBn : "", region != null ? region : "Bangladesh",
                    detailsEn != null ? detailsEn : "", detailsBn != null ? detailsBn : "",
                    issued != null ? issued : "", expires != null ? expires : "",
                    source != null ? source : "FFWC", sourceRef, active, sample, sync);
        } catch (Exception e) {
            return null;
        }
    }

    private DisasterUpdateEntity mapToDisasterUpdateEntity(DocumentSnapshot doc) {
        try {
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
            if (issuedAt <= 0) issuedAt = System.currentTimeMillis();

            long expiresAt = extractTimestamp(doc, "expiresAt");
            if (expiresAt <= 0) expiresAt = issuedAt + 7L * 86400000L;

            long updated = extractTimestamp(doc, "updatedAt");
            if (updated <= 0) updated = System.currentTimeMillis();

            Boolean active = doc.getBoolean("isActive");

            return new DisasterUpdateEntity(
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
                    issuedAt,
                    expiresAt,
                    updated,
                    active == null || active
            );
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> shelterToMap(Shelter s) {
        Map<String, Object> map = new HashMap<>();
        map.put(DatabaseContract.ShelterEntry._ID, s.getId());
        map.put(DatabaseContract.ShelterEntry.COLUMN_NAME_EN, s.getNameEn());
        map.put(DatabaseContract.ShelterEntry.COLUMN_NAME_BN, s.getNameBn());
        map.put(DatabaseContract.ShelterEntry.COLUMN_DISTRICT, s.getDistrict());
        map.put(DatabaseContract.ShelterEntry.COLUMN_UPAZILA, s.getUpazila());
        map.put(DatabaseContract.ShelterEntry.COLUMN_ADDRESS, s.getAddress());
        map.put(DatabaseContract.ShelterEntry.COLUMN_LATITUDE, s.getLatitude());
        map.put(DatabaseContract.ShelterEntry.COLUMN_LONGITUDE, s.getLongitude());
        map.put(DatabaseContract.ShelterEntry.COLUMN_CAPACITY, s.getCapacity());
        map.put(DatabaseContract.ShelterEntry.COLUMN_CURRENT_OCCUPANCY, s.getCurrentOccupancy());
        map.put(DatabaseContract.ShelterEntry.COLUMN_CONTACT_PHONE, s.getContactPhone());
        map.put(DatabaseContract.ShelterEntry.COLUMN_SHELTER_TYPE, s.getShelterType());
        map.put(DatabaseContract.ShelterEntry.COLUMN_ACCESSIBILITY_NOTES, s.getAccessibilityNotes());
        map.put(DatabaseContract.ShelterEntry.COLUMN_SOURCE, s.getSource());
        map.put(DatabaseContract.ShelterEntry.COLUMN_IS_ACTIVE, s.isActive() ? 1 : 0);
        map.put(DatabaseContract.ShelterEntry.COLUMN_VERIFIED_AT, s.getVerifiedAt());
        map.put(DatabaseContract.ShelterEntry.COLUMN_CREATED_AT, s.getCreatedAt());
        map.put(DatabaseContract.ShelterEntry.COLUMN_UPDATED_AT, s.getUpdatedAt());
        return map;
    }

    private Shelter mapToShelter(DocumentSnapshot doc) {
        try {
            Long idLong = doc.getLong(DatabaseContract.ShelterEntry._ID);
            long id = idLong != null ? idLong : parseDocIdToLong(doc.getId());
            String nameEn = doc.getString(DatabaseContract.ShelterEntry.COLUMN_NAME_EN);
            if (nameEn == null || nameEn.trim().isEmpty() || "Shelter".equalsIgnoreCase(nameEn.trim())) {
                nameEn = doc.getString("name");
            }
            if (nameEn == null || nameEn.trim().isEmpty() || "Shelter".equalsIgnoreCase(nameEn.trim())) {
                nameEn = doc.getString("shelterName");
            }
            if (nameEn == null || nameEn.trim().isEmpty() || "Shelter".equalsIgnoreCase(nameEn.trim())) {
                nameEn = doc.getString("title");
            }

            String nameBn = doc.getString(DatabaseContract.ShelterEntry.COLUMN_NAME_BN);
            if (nameBn == null || nameBn.trim().isEmpty()) {
                nameBn = doc.getString("nameBn");
            }
            if (nameBn == null || nameBn.trim().isEmpty()) {
                nameBn = doc.getString("titleBn");
            }
            if (nameBn == null || nameBn.trim().isEmpty()) {
                nameBn = nameEn;
            }
            if (nameEn == null || nameEn.trim().isEmpty()) {
                nameEn = nameBn;
            }

            String dist = doc.getString(DatabaseContract.ShelterEntry.COLUMN_DISTRICT);
            if (dist == null || dist.trim().isEmpty()) {
                dist = doc.getString("districtId");
            }

            String upazila = doc.getString(DatabaseContract.ShelterEntry.COLUMN_UPAZILA);
            if (upazila == null || upazila.trim().isEmpty()) {
                upazila = doc.getString("upazilaId");
            }

            String address = doc.getString(DatabaseContract.ShelterEntry.COLUMN_ADDRESS);
            Double lat = doc.getDouble(DatabaseContract.ShelterEntry.COLUMN_LATITUDE);
            Double lng = doc.getDouble(DatabaseContract.ShelterEntry.COLUMN_LONGITUDE);
            Long capLong = doc.getLong(DatabaseContract.ShelterEntry.COLUMN_CAPACITY);
            int capacity = capLong != null ? capLong.intValue() : 500;
            Long occLong = doc.getLong(DatabaseContract.ShelterEntry.COLUMN_CURRENT_OCCUPANCY);
            if (occLong == null) occLong = doc.getLong("current_occupancy");
            int occupancy = occLong != null ? occLong.intValue() : 0;

            String phone = doc.getString(DatabaseContract.ShelterEntry.COLUMN_CONTACT_PHONE);
            if (phone == null || phone.trim().isEmpty()) {
                phone = doc.getString("contactNumber");
            }

            String type = doc.getString(DatabaseContract.ShelterEntry.COLUMN_SHELTER_TYPE);
            if (type == null || type.trim().isEmpty()) {
                type = doc.getString("shelterType");
            }

            String notes = doc.getString(DatabaseContract.ShelterEntry.COLUMN_ACCESSIBILITY_NOTES);
            String source = doc.getString(DatabaseContract.ShelterEntry.COLUMN_SOURCE);
            Long activeLong = doc.getLong(DatabaseContract.ShelterEntry.COLUMN_IS_ACTIVE);
            Boolean actBool = doc.getBoolean("isActive");
            boolean active = actBool != null ? actBool : (activeLong == null || activeLong == 1);
            String verified = doc.getString(DatabaseContract.ShelterEntry.COLUMN_VERIFIED_AT);
            Long createdLong = doc.getLong(DatabaseContract.ShelterEntry.COLUMN_CREATED_AT);
            long created = createdLong != null ? createdLong : System.currentTimeMillis();
            Long updatedLong = doc.getLong(DatabaseContract.ShelterEntry.COLUMN_UPDATED_AT);
            long updated = updatedLong != null ? updatedLong : System.currentTimeMillis();

            return new Shelter(id, nameEn != null ? nameEn : "Disaster Shelter", nameBn != null ? nameBn : "দুর্যোগ আশ্রয়কেন্দ্র",
                    dist != null ? dist : "", upazila != null ? upazila : "", address != null ? address : "",
                    lat != null ? lat : 0.0, lng != null ? lng : 0.0, capacity, occupancy,
                    phone != null ? phone : "", type != null ? type : "Disaster Shelter",
                    notes != null ? notes : "", source != null ? source : "MoDMR", active,
                    verified != null ? verified : "2026-01-01", created, updated);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> waterObservationToMap(WaterObservation o) {
        Map<String, Object> map = new HashMap<>();
        // Snake case keys
        map.put(DatabaseContract.WaterObservationEntry._ID, o.getId());
        map.put(DatabaseContract.WaterObservationEntry.COLUMN_LOCATION_NAME, o.getLocationName());
        map.put(DatabaseContract.WaterObservationEntry.COLUMN_DISTRICT, o.getDistrict());
        map.put(DatabaseContract.WaterObservationEntry.COLUMN_SOURCE_TYPE, o.getSourceType());
        map.put(DatabaseContract.WaterObservationEntry.COLUMN_APPEARANCE, o.getAppearance());
        map.put(DatabaseContract.WaterObservationEntry.COLUMN_ODOR, o.getOdor());
        map.put(DatabaseContract.WaterObservationEntry.COLUMN_PH_LEVEL, o.getPhLevel());
        map.put(DatabaseContract.WaterObservationEntry.COLUMN_TURBIDITY_STATUS, o.getTurbidityStatus());
        map.put(DatabaseContract.WaterObservationEntry.COLUMN_SAFETY_STATUS, o.getSafetyStatus());
        map.put(DatabaseContract.WaterObservationEntry.COLUMN_OBSERVATION_TIME, o.getObservationTime());
        map.put(DatabaseContract.WaterObservationEntry.COLUMN_REPORTED_BY, o.getReportedBy());
        map.put(DatabaseContract.WaterObservationEntry.COLUMN_REMARKS, o.getRemarks());
        map.put(DatabaseContract.WaterObservationEntry.COLUMN_IMAGE_URI, o.getImageUri());
        map.put(DatabaseContract.WaterObservationEntry.COLUMN_IMAGE_URL, o.getImageUrl());
        map.put(DatabaseContract.WaterObservationEntry.COLUMN_SYNC_STATUS, o.getSyncStatus());
        map.put(DatabaseContract.WaterObservationEntry.COLUMN_CREATED_AT, o.getCreatedAt());
        map.put(DatabaseContract.WaterObservationEntry.COLUMN_UPDATED_AT, o.getUpdatedAt());

        // Camel case keys for multi-client compatibility
        map.put("locationName", o.getLocationName());
        map.put("sourceType", o.getSourceType());
        map.put("phLevel", o.getPhLevel());
        map.put("turbidityStatus", o.getTurbidityStatus());
        map.put("safetyStatus", o.getSafetyStatus());
        map.put("observationTime", o.getObservationTime());
        map.put("reportedBy", o.getReportedBy());
        map.put("imageUrl", o.getImageUrl());
        map.put("imageUri", o.getImageUri());
        map.put("userId", o.getUserId());
        map.put("user_id", o.getUserId());
        map.put("createdAt", o.getCreatedAt());
        map.put("updatedAt", o.getUpdatedAt());
        return map;
    }

    private WaterObservation mapToWaterObservation(DocumentSnapshot doc) {
        try {
            Long idLong = doc.getLong(DatabaseContract.WaterObservationEntry._ID);
            long id = idLong != null ? idLong : parseDocIdToLong(doc.getId());
            String loc = doc.getString(DatabaseContract.WaterObservationEntry.COLUMN_LOCATION_NAME);
            if (loc == null) loc = doc.getString("locationName");
            if (loc == null) loc = doc.getString("location");

            String dist = doc.getString(DatabaseContract.WaterObservationEntry.COLUMN_DISTRICT);
            if (dist == null) dist = doc.getString("districtId");

            String src = doc.getString(DatabaseContract.WaterObservationEntry.COLUMN_SOURCE_TYPE);
            if (src == null) src = doc.getString("sourceType");

            String app = doc.getString(DatabaseContract.WaterObservationEntry.COLUMN_APPEARANCE);
            String odor = doc.getString(DatabaseContract.WaterObservationEntry.COLUMN_ODOR);
            Double ph = doc.getDouble(DatabaseContract.WaterObservationEntry.COLUMN_PH_LEVEL);
            if (ph == null) ph = doc.getDouble("phLevel");

            String turb = doc.getString(DatabaseContract.WaterObservationEntry.COLUMN_TURBIDITY_STATUS);
            if (turb == null) turb = doc.getString("turbidityStatus");

            String safe = doc.getString(DatabaseContract.WaterObservationEntry.COLUMN_SAFETY_STATUS);
            if (safe == null) safe = doc.getString("safetyStatus");

            String time = doc.getString(DatabaseContract.WaterObservationEntry.COLUMN_OBSERVATION_TIME);
            if (time == null) time = doc.getString("observationTime");

            String rep = doc.getString(DatabaseContract.WaterObservationEntry.COLUMN_REPORTED_BY);
            if (rep == null) rep = doc.getString("reportedBy");

            String rem = doc.getString(DatabaseContract.WaterObservationEntry.COLUMN_REMARKS);
            String imgUri = doc.getString(DatabaseContract.WaterObservationEntry.COLUMN_IMAGE_URI);
            if (imgUri == null) imgUri = doc.getString("imageUri");

            String imgUrl = doc.getString(DatabaseContract.WaterObservationEntry.COLUMN_IMAGE_URL);
            if (imgUrl == null) imgUrl = doc.getString("imageUrl");

            String sync = doc.getString(DatabaseContract.WaterObservationEntry.COLUMN_SYNC_STATUS);

            String uid = doc.getString("userId");
            if (uid == null) uid = doc.getString("user_id");

            Long createdLong = doc.getLong(DatabaseContract.WaterObservationEntry.COLUMN_CREATED_AT);
            if (createdLong == null) createdLong = doc.getLong("createdAt");
            long created = createdLong != null ? createdLong : System.currentTimeMillis();

            Long updatedLong = doc.getLong(DatabaseContract.WaterObservationEntry.COLUMN_UPDATED_AT);
            if (updatedLong == null) updatedLong = doc.getLong("updatedAt");
            long updated = updatedLong != null ? updatedLong : System.currentTimeMillis();

            return new WaterObservation(id, loc != null ? loc : "Unknown Location",
                    dist != null ? dist : "Dhaka", src != null ? src : "Tubewell",
                    app != null ? app : "Clear", odor != null ? odor : "None",
                    ph != null ? ph : -1.0, turb != null ? turb : "Low",
                    safe != null ? safe : "Potable", time != null ? time : "Recent",
                    rep != null ? rep : "Citizen", rem, imgUri, imgUrl,
                    sync != null ? sync : WaterObservation.SYNC_SYNCED,
                    uid != null ? uid : "",
                    created, updated);
        } catch (Exception e) {
            return null;
        }
    }

    private PreparednessChecklist mapToChecklist(DocumentSnapshot doc) {
        try {
            Long idLong = doc.getLong(DatabaseContract.PreparednessChecklistEntry._ID);
            long id = idLong != null ? idLong : parseDocIdToLong(doc.getId());
            String key = doc.getString(DatabaseContract.PreparednessChecklistEntry.COLUMN_CATEGORY_KEY);
            String titleEn = doc.getString(DatabaseContract.PreparednessChecklistEntry.COLUMN_TITLE_EN);
            String titleBn = doc.getString(DatabaseContract.PreparednessChecklistEntry.COLUMN_TITLE_BN);
            String descEn = doc.getString(DatabaseContract.PreparednessChecklistEntry.COLUMN_DESCRIPTION_EN);
            String descBn = doc.getString(DatabaseContract.PreparednessChecklistEntry.COLUMN_DESCRIPTION_BN);
            String icon = doc.getString(DatabaseContract.PreparednessChecklistEntry.COLUMN_ICON_NAME);
            Long orderLong = doc.getLong(DatabaseContract.PreparednessChecklistEntry.COLUMN_DISPLAY_ORDER);
            int order = orderLong != null ? orderLong.intValue() : 1;

            return new PreparednessChecklist(id, key != null ? key : "CATEGORY",
                    titleEn != null ? titleEn : "", titleBn != null ? titleBn : "",
                    descEn, descBn, icon != null ? icon : "ic_checklist", order);
        } catch (Exception e) {
            return null;
        }
    }

    private ChecklistItem mapToChecklistItem(DocumentSnapshot doc) {
        try {
            Long idLong = doc.getLong(DatabaseContract.ChecklistItemEntry._ID);
            long id = idLong != null ? idLong : parseDocIdToLong(doc.getId());
            Long clIdLong = doc.getLong(DatabaseContract.ChecklistItemEntry.COLUMN_CHECKLIST_ID);
            long clId = clIdLong != null ? clIdLong : 1;
            String itemEn = doc.getString(DatabaseContract.ChecklistItemEntry.COLUMN_ITEM_EN);
            String itemBn = doc.getString(DatabaseContract.ChecklistItemEntry.COLUMN_ITEM_BN);
            Long compLong = doc.getLong(DatabaseContract.ChecklistItemEntry.COLUMN_IS_COMPLETED);
            boolean completed = compLong != null && compLong == 1;
            Long essLong = doc.getLong(DatabaseContract.ChecklistItemEntry.COLUMN_IS_ESSENTIAL);
            boolean essential = essLong != null && essLong == 1;
            Long updLong = doc.getLong(DatabaseContract.ChecklistItemEntry.COLUMN_UPDATED_AT);
            long updated = updLong != null ? updLong : System.currentTimeMillis();

            return new ChecklistItem(id, clId, itemEn != null ? itemEn : "", itemBn != null ? itemBn : "",
                    completed, essential, updated);
        } catch (Exception e) {
            return null;
        }
    }

    private long parseDocIdToLong(String docId) {
        if (docId == null) return System.currentTimeMillis();
        try {
            String num = docId.replaceAll("\\D+", "");
            if (!num.isEmpty()) {
                return Long.parseLong(num);
            }
        } catch (Exception ignored) {}
        return System.currentTimeMillis();
    }

    // =========================================================================
    // BASELINE SEED DATA GENERATORS (ALWAYS AVAILABLE OFFLINE)
    // =========================================================================

    private List<EmergencyContact> getSeedContacts() {
        List<EmergencyContact> list = new ArrayList<>();
        list.add(new EmergencyContact(1, "National Emergency Service", "জাতীয় জরুরি সেবা",
                "999", "Instant police response, fire brigade dispatch, and ambulance coordination",
                "তাৎক্ষণিক পুলিশ সহায়তা, ফায়ার সার্ভিস ও অ্যাম্বুলেন্স সমন্বয়",
                "NATIONAL", "24/7", true, "Bangladesh Police HQ / ICT Ministry", "2026-01-01", true, 1));

        list.add(new EmergencyContact(2, "Disaster Early Warning Center", "দুর্যোগের আগাম বার্তা কেন্দ্র",
                "1090", "Automated flood, cyclone, and maritime weather warning voice bulletins",
                "বন্যা, ঘূর্ণিঝড় ও সমুদ্র আবহাওয়া সতর্কবার্তার স্বয়ংক্রিয় ভয়েস বার্তা",
                "DISASTER", "24/7", true, "MoDMR / Disaster Management Dept", "2026-01-01", true, 2));

        list.add(new EmergencyContact(3, "Health Directorate Hotline (Shastho Batayan)", "স্বাস্থ্য বাতায়ন (স্বাস্থ্য অধিদপ্তর)",
                "16263", "Free medical consultation, doctor advice, and emergency health advice",
                "বিনামূল্যে চিকিৎসকের জরুরি স্বাস্থ্য পরামর্শ ও মেডিকেল দিকনির্দেশনা",
                "MEDICAL", "24/7", false, "DGHS / Ministry of Health", "2026-01-01", true, 3));

        list.add(new EmergencyContact(4, "Women & Child Helpline", "নারী ও শিশু নির্যাতন প্রতিরোধ হেল্পলাইন",
                "109", "Emergency rescue, legal and shelter support for vulnerable individuals",
                "দুর্যোগকালীন নারী ও শিশুদের জরুরি নিরাপত্তা, উদ্ধার ও সহায়তা",
                "HUMANITARIAN", "24/7", true, "Ministry of Women & Children Affairs", "2026-01-01", true, 4));

        list.add(new EmergencyContact(5, "Bangladesh Red Crescent Society (BDRCS)", "বাংলাদেশ রেড ক্রিসেন্ট সোসাইটি",
                "02-222237474", "Community disaster response, blood bank, search and rescue",
                "কমিউনিটি উদ্ধার তৎপরতা, রক্ত সেবা ও জরুরি মানবিক সহায়তা",
                "HUMANITARIAN", "24/7", false, "BDRCS National Headquarters", "2026-01-01", true, 5));

        list.add(new EmergencyContact(6, "Flood Forecasting & Warning Centre (FFWC)", "বন্যা পূর্বাভাস ও সতর্কীকরণ কেন্দ্র",
                "02-223386628", "Official hydrological river water level bulletins and forecasting",
                "নদী অববাহিকার পানির উচ্চতা, বিপদসীমা ও পূর্বাভাসের তথ্য",
                "FLOOD", "Office Hours & Flood Season 24h", false, "BWDB / Water Resources Ministry", "2026-01-01", true, 6));

        list.add(new EmergencyContact(7, "Fire Service & Civil Defence Central Control", "ফায়ার সার্ভিস ও সিভিল ডিফেন্স কেন্দ্রীয় নিয়ন্ত্রণ কক্ষ",
                "02-223355555", "Emergency search, boat rescue, and flood evacuation response",
                "আকস্মিক উদ্ধারকাজ, স্পিডবোট উদ্ধার ও ফায়ার সার্ভিস নিয়ন্ত্রণ কক্ষ",
                "NATIONAL", "24/7", false, "FSCD Headquarters Dhaka", "2026-01-01", true, 7));

        return list;
    }

    private List<FloodWarning> getSeedFloodWarnings() {
        List<FloodWarning> list = new ArrayList<>();
        list.add(new FloodWarning(1, "CRITICAL",
                "Surma-Kushiyara Severe Flash Flood",
                "সুরমা-কুশিয়ারা তীব্র আকস্মিক বন্যা সতর্কবার্তা",
                "Sylhet & Sunamganj",
                "Surma River is flowing 68 cm above danger level at Kanaighat and 28 cm at Sylhet Sadar. Inundation spreading across low-lying unions.",
                "মেঘালয় ও আসামে অতিভারী বর্ষণে সুরমা নদী কানাইঘাটে ৬৮ সেমি এবং সিলেট সদরে ২৮ সেমি বিপৎসীমার উপর দিয়ে প্রবাহিত হচ্ছে। নিম্নাঞ্চল প্লাবিত।",
                "2026-10-12 08:00 BST", "2026-10-18 18:00 BST",
                "FFWC (BWDB)", "alert_surma_001", true, false, System.currentTimeMillis()));

        list.add(new FloodWarning(2, "CRITICAL",
                "Brahmaputra-Jamuna Basin Major Flood Alert",
                "ব্রহ্মপুত্র-যমুনা অববাহিকায় ভয়াবহ বন্যা সতর্কতা",
                "Sirajganj & Bogura",
                "Jamuna water level rising at 18 cm/24h at Sirajganj and Sariakandi. Riverbank erosion and embankment overtopping imminent.",
                "যমুনা নদীর পানি সিরাজগঞ্জ ও সারিয়াকান্দি পয়েন্টে দ্রুত বাড়ছে। নদী তীরবর্তী নিম্নাঞ্চল ও চরে বন্যা পরিস্থিতি অবনতিশীল।",
                "2026-10-13 06:00 BST", "2026-10-19 12:00 BST",
                "Flood Forecasting & Warning Centre", "alert_jamuna_002", true, false, System.currentTimeMillis()));

        list.add(new FloodWarning(3, "CRITICAL",
                "Dharla & Dudhkumar Flood Inundation",
                "ধরলা ও দুধকুমার নদীর পানি বৃদ্ধি ও প্লাবন",
                "Kurigram",
                "Dharla river flowing 35 cm above danger level at Kurigram. Over 40,000 households across char areas stranded.",
                "কুড়িগ্রাম পয়েন্টে ধরলা নদী বিপৎসীমার ৩৫ সেমি উপরে। রৌমারী, রাজিবপুর ও চিলমারী চরাঞ্চল প্লাবিত হয়েছে।",
                "2026-10-12 10:00 BST", "2026-10-17 18:00 BST",
                "BWDB & FFWC", "alert_kurigram_003", true, false, System.currentTimeMillis()));

        list.add(new FloodWarning(4, "WARNING",
                "Teesta River High Water Advisory",
                "তিস্তা নদীর পানি বৃদ্ধি সতর্কবার্তা",
                "Kurigram, Lalmonirhat, Nilphamari",
                "Teesta Barrage point at Dalia recorded discharge at maximum capacity. Gated flumes fully opened to release upstream run-off.",
                "ডালিয়া পয়েন্টে তিস্তা নদীর পানি বিপৎসীমার কাছাকাছি। ডিমলা, হাতীবান্ধা ও পাটগ্রাম চরে সতর্কবার্তা জারি।",
                "2026-10-11 12:00 BST", "2026-10-16 12:00 BST",
                "BWDB Dalia Division", "alert_teesta_004", true, false, System.currentTimeMillis()));

        list.add(new FloodWarning(5, "WARNING",
                "Bay of Bengal Coastal Cyclone & Surge Warning",
                "বঙ্গোপসাগরে ঘূর্ণিঝড় ও জলোচ্ছ্বাস সতর্কবার্তা",
                "Cox's Bazar & Chittagong",
                "Deep tropical depression intensifying into cyclonic storm. Maritime signal 4 hoisted. 4-6 ft storm surge expected along coastlines.",
                "বঙ্গোপসাগরে সৃষ্ট গভীর নিম্নচাপটি ঘূর্ণিঝড়ে রূপ নিচ্ছে। ৪ নম্বর স্থানীয় হুঁশিয়ারি সংকেত জারি। ৪-৬ ফুট উচ্চতার জলোচ্ছ্বাসের আশঙ্কা।",
                "2026-10-14 06:00 BST", "2026-10-18 20:00 BST",
                "Bangladesh Meteorological Department (BMD)", "alert_cyclone_005", true, false, System.currentTimeMillis()));

        list.add(new FloodWarning(6, "CRITICAL",
                "Muhuri River Embankment Breach Alert",
                "মুহুরী নদীর বাঁধ ভাঙন ও প্লাবন সতর্কতা",
                "Feni",
                "Heavy transboundary hill torrents caused multiple dyke breaches along Muhuri river in Parshuram and Fulgazi upazilas.",
                "ভারতের ত্রিপুরা থেকে নেমে আসা পাহাড়ি ঢলে পরশুরাম ও ফুলগাজীতে মুহুরী নদীর বন্যা নিয়ন্ত্রণ বাঁধ ভেঙে বেশ কয়েকটি গ্রাম প্লাবিত।",
                "2026-10-13 14:00 BST", "2026-10-17 18:00 BST",
                "Feni District Administration & BWDB", "alert_muhuri_006", true, false, System.currentTimeMillis()));

        list.add(new FloodWarning(7, "ADVISORY",
                "Coastal Tidal Inundation Advisory",
                "উপকূলীয় জোয়ারের পানি বৃদ্ধি ও বাঁধ সতর্কতা",
                "Khulna & Satkhira",
                "Spring tide combined with southerly wind forcing saline water through weak embankments across Dacope, Koyra, and Shyamnagar.",
                "অমাবস্যার জোয়ার ও বাতাসের প্রভাবে দাকোপ, কয়রা ও শ্যামনগরের দুর্বল বেড়িবাঁধ উপচে লবণাক্ত পানি প্রবেশের আশঙ্কা।",
                "2026-10-12 12:00 BST", "2026-10-16 18:00 BST",
                "CPP & Local Disaster Management Committee", "alert_sundarbans_007", true, false, System.currentTimeMillis()));

        list.add(new FloodWarning(8, "ADVISORY",
                "Urban Torrential Rain & Waterlogging Advisory",
                "ভারী বর্ষণজনিত জলাবদ্ধতা ও সতর্কতা",
                "Dhaka",
                "Consecutive cloudbursts forecast over Greater Dhaka. Urban road waterlogging and slow drainage expected in low-lying canals.",
                "ঢাকায় টানা ভারী বৃষ্টির পূর্বাভাস। মিরপুর, ডেমরা, ও টঙ্গী খাল এলাকায় জলাবদ্ধতার কারণে যান চলাচলে বিঘ্ন ঘটতে পারে।",
                "2026-10-14 09:00 BST", "2026-10-17 12:00 BST",
                "Dhaka City Corporation Disaster Cell", "alert_dhaka_008", true, false, System.currentTimeMillis()));

        return list;
    }

    private List<Shelter> getSeedShelters() {
        List<Shelter> list = new ArrayList<>();

        // --- COX'S BAZAR ---
        list.add(new Shelter(1, "Kutubdia Model High School Cyclone Shelter",
                "কুতুবদিয়া মডেল উচ্চ বিদ্যালয় সাইক্লোন শেল্টার",
                "Cox's Bazar", "Kutubdia", "Boroghop Union, Kutubdia Island",
                21.8155, 91.8542, 1200, 0, "01711-000001",
                "Multi-Tier Cyclone Shelter", "Reinforced concrete, ground livestock floor, wheelchair ramp",
                "MoDMR / CPP Bangladesh", true, "2026-01-15", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(2, "Teknaf Coastal Multipurpose Disaster Shelter",
                "টেকনাফ উপকূলীয় বহুমুখী দুর্যোগ আশ্রয়কেন্দ্র",
                "Cox's Bazar", "Teknaf", "Sabrang Coastal Belt, Teknaf",
                20.8654, 92.2982, 1500, 0, "01819-000002",
                "Coastal Storm Surge Shelter", "High-elevation stilt design, solar lighting backup, potable rain tank",
                "MoDMR / CPP Bangladesh", true, "2026-02-10", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(3, "Saint Martin Island Cyclone Refuge",
                "সেন্ট মার্টিন দ্বীপ সাইক্লোন আশ্রয়কেন্দ্র",
                "Cox's Bazar", "Teknaf", "Jinjiira Point, Saint Martin Island",
                20.6273, 92.3225, 850, 0, "01819-000003",
                "Island Cyclone Shelter", "Tidal surge barrier, satellite comms, emergency clinic",
                "Bangladesh Navy / CPP", true, "2026-01-20", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(4, "Maheshkhali Gorakghata Cyclone Shelter",
                "মহেশখালী গোরকঘাটা সাইক্লোন শেল্টার",
                "Cox's Bazar", "Maheshkhali", "Gorakghata, Maheshkhali Island",
                21.5233, 91.9667, 1100, 0, "01711-000004",
                "Multipurpose Cyclone Shelter", "Livestock enclosure, solar water desalination, generator",
                "MoDMR / CPP Bangladesh", true, "2026-02-05", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(5, "Inani Beach Coastal Multipurpose Shelter",
                "ইনানী উপকূলীয় বহুমুখী আশ্রয়কেন্দ্র",
                "Cox's Bazar", "Ukhia", "Jalia Palong, Inani Marine Drive",
                21.1865, 92.0542, 1300, 0, "01819-000005",
                "Coastal Surge Shelter", "Reinforced concrete structure, first aid center",
                "MoDMR / CPP Bangladesh", true, "2026-02-18", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(6, "Chakaria Matamuhuri River Flood Refuge",
                "চকোরিয়া মাতামুহুরী নদী বন্যা আশ্রয়কেন্দ্র",
                "Cox's Bazar", "Chakaria", "Baraitali, Matamuhuri Basin",
                21.7865, 92.0782, 1000, 0, "01711-000006",
                "Riverine Flood Shelter", "Elevated high-ground building, pure drinking water reservoir",
                "District Relief Office", true, "2026-01-30", System.currentTimeMillis(), System.currentTimeMillis()));

        // --- CHITTAGONG ---
        list.add(new Shelter(7, "Anwara Coastal Resilience Community Shelter",
                "আনোয়ারা উপকূলীয় রেজিলিয়েন্স আশ্রয়কেন্দ্র",
                "Chittagong", "Anwara", "Parki Coastal Area, Anwara",
                22.2154, 91.9124, 1300, 0, "01716-000007",
                "Coastal Cyclone Shelter", "Reinforced wind-resistant roof, rainwater harvesting unit",
                "MoDMR / CPP Bangladesh", true, "2026-02-14", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(8, "Sandwip Island Storm Surge Disaster Shelter",
                "সন্দ্বীপ দ্বীপ জলোচ্ছ্বাস দুর্যোগ আশ্রয়কেন্দ্র",
                "Chittagong", "Sandwip", "Rahmatpur Union, Sandwip Island",
                22.4833, 91.4333, 1600, 0, "01716-000008",
                "Offshore Island Refuge", "Multi-story storm shelter with cattle killas, VHF radio link",
                "Bangladesh Red Crescent / CPP", true, "2026-01-25", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(9, "Banshkhali Gandamara Cyclone Shelter Point",
                "বাঁশখালী গণ্ডামারা সাইক্লোন আশ্রয়কেন্দ্র",
                "Chittagong", "Banshkhali", "Gandamara Coastal Embankment",
                21.9833, 91.9333, 1250, 0, "01716-000009",
                "Coastal Cyclone Shelter", "Stilt structure above tidal flood elevation",
                "MoDMR / CPP Bangladesh", true, "2026-02-11", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(10, "Sitakunda Kumira Marine Flood & Surge Shelter",
                "সীতাকুণ্ড কুমিরা উপকূলীয় বন্যা আশ্রয়কেন্দ্র",
                "Chittagong", "Sitakunda", "Kumira Coastal Highway, Sitakunda",
                22.5186, 91.7012, 1100, 0, "01716-000010",
                "Industrial Coastal Shelter", "Heavy gale resistant, solar backup, medical bay",
                "MoDMR Relief Wing", true, "2026-02-02", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(11, "Mirsharai Muhuri River Project Cyclone Shelter",
                "মীরসরাই মুহুরী প্রজেক্ট সাইক্লোন আশ্রয়কেন্দ্র",
                "Chittagong", "Mirsharai", "Ichakhali, Muhuri Dam Link",
                22.7712, 91.5742, 1400, 0, "01716-000011",
                "Tidal Surge Community Shelter", "Dual ramp, emergency logistics bay, sanitation blocks",
                "MoDMR / BWDB", true, "2026-01-18", System.currentTimeMillis(), System.currentTimeMillis()));

        // --- NOAKHALI & LAKSHMIPUR ---
        list.add(new Shelter(12, "Hatiya Island Jahajmara Multipurpose Cyclone Shelter",
                "হাতিয়া দ্বীপ জাহাজমারা বহুমুখী আশ্রয়কেন্দ্র",
                "Noakhali", "Hatiya", "Jahajmara Coast, Hatiya Island",
                22.3667, 91.1250, 2000, 0, "01718-000012",
                "Mega Cyclone Shelter", "Helipad access, mega livestock killa, 4-story RCC construction",
                "MoDMR / CPP Bangladesh", true, "2026-01-22", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(13, "Subarnachar Coastal Resilience Shelter",
                "সুবর্ণচর উপকূলীয় রেজিলিয়েন্স আশ্রয়কেন্দ্র",
                "Noakhali", "Subarnachar", "Char Jabbar Union, Subarnachar",
                22.6833, 91.1333, 1200, 0, "01718-000013",
                "Coastal Windstorm Shelter", "Dedicated gender-segregated wards, drinking rainwater tanks",
                "District Relief Office", true, "2026-02-08", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(14, "Ramgati Meghna Riverbank Emergency Shelter",
                "রামগতি মেঘনা নদী তীরবর্তী জরুরি আশ্রয়কেন্দ্র",
                "Lakshmipur", "Ramgati", "Char Alexandar, Ramgati",
                22.6000, 90.9833, 1150, 0, "01718-000014",
                "Riverine Erosion & Surge Shelter", "High embankment reinforced foundation, solar power",
                "MoDMR / Red Crescent", true, "2026-02-12", System.currentTimeMillis(), System.currentTimeMillis()));

        // --- BHOLA ---
        list.add(new Shelter(15, "Char Fasson Dhal Char Cyclone Refuge",
                "চরফ্যাশন ঢালচর সাইক্লোন আশ্রয়কেন্দ্র",
                "Bhola", "Char Fasson", "Dhal Char Coastal Front, Char Fasson",
                21.8542, 90.7632, 1500, 0, "01719-000015",
                "Bay of Bengal Surge Refuge", "Stilts elevated 4.5m above high tide, backup generator",
                "MoDMR / CPP Bangladesh", true, "2026-01-12", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(16, "Manpura Island Multipurpose Cyclone Shelter",
                "মনপুরা দ্বীপ বহুমুখী সাইক্লোন আশ্রয়কেন্দ্র",
                "Bhola", "Manpura", "Hazirhat, Manpura Island",
                22.2833, 90.9667, 1350, 0, "01719-000016",
                "Offshore Island Shelter", "Reinforced storm shields, rainwater distillation facility",
                "MoDMR / CPP Bangladesh", true, "2026-01-26", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(17, "Tazumuddin Meghna Coastal Cyclone Shelter",
                "তজুমদ্দিন মেঘনা উপকূলীয় সাইক্লোন আশ্রয়কেন্দ্র",
                "Bhola", "Tazumuddin", "Chanchra Union, Tazumuddin Coast",
                22.4167, 90.8500, 1100, 0, "01719-000017",
                "Tidal Surge Community Shelter", "Deep tubewell, emergency food grain storage vault",
                "District Relief Office", true, "2026-02-15", System.currentTimeMillis(), System.currentTimeMillis()));

        // --- PATUAKHALI & BARGUNA ---
        list.add(new Shelter(18, "Kalapara Cyclone & Tidal Surge Shelter",
                "কলাপাড়া ঘূর্ণিঝড় ও জলোচ্ছ্বাস আশ্রয়কেন্দ্র",
                "Patuakhali", "Kalapara", "Kuakata Coastal Link, Kalapara",
                21.9862, 90.2415, 1400, 0, "01715-000018",
                "Tidal Surge Refuge", "Elevated ground floor, standby generator, pregnant mothers corner",
                "MoDMR / CPP Bangladesh", true, "2026-01-28", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(19, "Rangabali Island Disaster Response Shelter",
                "রাঙ্গাবালী দ্বীপ দুর্যোগ মোকাবিলা আশ্রয়কেন্দ্র",
                "Patuakhali", "Rangabali", "Choto Baishdia Coast, Rangabali",
                21.9167, 90.4667, 1250, 0, "01715-000019",
                "Island Storm Shelter", "Livestock ramp, solar backup lighting, high-frequency radio",
                "MoDMR / CPP Bangladesh", true, "2026-02-04", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(20, "Galachipa Riverine Flood Shelter Centre",
                "গলাচিপা নদী অববাহিকা বন্যা আশ্রয়কেন্দ্র",
                "Patuakhali", "Galachipa", "Dakua Union, Galachipa",
                22.1667, 90.4167, 950, 0, "01715-000020",
                "Riverine Multi-Hazard Shelter", "Reinforced platform above monsoon surge level",
                "District Relief Office", true, "2026-02-21", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(21, "Patharghata Coastal Forest Cyclone Shelter",
                "পাথরঘাটা উপকূলীয় বন সাইক্লোন আশ্রয়কেন্দ্র",
                "Barguna", "Patharghata", "Charduani, Bishkhali River Confluence",
                22.0500, 89.9667, 1300, 0, "01720-000021",
                "Bishkhali Surge Refuge", "Stilts design, direct emergency boat docking ramp",
                "MoDMR / CPP Bangladesh", true, "2026-01-19", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(22, "Amtali Payra River Flood Shelter Point",
                "আমতলী পায়রা নদী বন্যা আশ্রয়কেন্দ্র",
                "Barguna", "Amtali", "Gulishakhali Union, Payra Riverbank",
                22.1333, 90.2333, 1000, 0, "01720-000022",
                "River Basin Multi-Use Shelter", "Elevated plinth, safe drinking water tank",
                "District Relief Office", true, "2026-02-17", System.currentTimeMillis(), System.currentTimeMillis()));

        // --- KHULNA, BAGERHAT & SATKHIRA ---
        list.add(new Shelter(23, "Koyra Sundarbans Buffer Cyclone Shelter",
                "কয়রা সুন্দরবন বাফার সাইক্লোন আশ্রয়কেন্দ্র",
                "Khulna", "Koyra", "Maharajpur Coast, Koyra Embankment",
                22.3415, 89.3021, 1500, 0, "01721-000023",
                "Sundarbans Edge Storm Shelter", "Reinforced against Category 5 cyclones, livestock killa",
                "MoDMR / CPP Bangladesh", true, "2026-01-14", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(24, "Dacope Coastal Storm Surge Community Shelter",
                "দাকোপ উপকূলীয় জলোচ্ছ্বাস আশ্রয়কেন্দ্র",
                "Khulna", "Dacope", "Bajua Union, Dacope",
                22.5721, 89.5124, 1100, 0, "01721-000024",
                "Tidal Surge Community Shelter", "Elevated platform, reverse osmosis safe water plant",
                "MoDMR / CPP Bangladesh", true, "2026-02-06", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(25, "Mongla Port Disaster Response Shelter",
                "মোংলা বন্দর দুর্যোগ মোকাবিলা আশ্রয়কেন্দ্র",
                "Bagerhat", "Mongla", "Burirdanga, Mongla Riverfront",
                22.4833, 89.6000, 1400, 0, "01722-000025",
                "Port Coastal Shelter", "Emergency medical room, generator power, wireless comms",
                "Mongla Port Authority / MoDMR", true, "2026-01-31", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(26, "Sarankhola Baleshwar River Cyclone Shelter",
                "শরণখোলা বলেশ্বর নদী সাইক্লোন আশ্রয়কেন্দ্র",
                "Bagerhat", "Sarankhola", "Rayenda Bazar, Baleshwar Riverbank",
                22.2833, 89.7833, 1600, 0, "01722-000026",
                "High-Capacity Storm Refuge", "Triple-tier RCC building, cattle protection compound",
                "MoDMR / Red Crescent", true, "2026-02-09", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(27, "Shyamnagar Coastal Resilience Center",
                "শ্যামনগর উপকূলীয় রেজিলিয়েন্স সেন্টার",
                "Satkhira", "Shyamnagar", "Gabura Island Union, Shyamnagar",
                22.3333, 89.1500, 1350, 0, "01723-000027",
                "Coastal Island Cyclone Shelter", "Desalination water tank, solar grid, emergency food depot",
                "MoDMR / CPP Bangladesh", true, "2026-01-29", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(28, "Assasuni Kholpetua Flood Shelter",
                "আশাশুনি খোলপেটুয়া বন্যা আশ্রয়কেন্দ্র",
                "Satkhira", "Assasuni", "Pratapnagar Embankment Point",
                22.5500, 89.1833, 1050, 0, "01723-000028",
                "Embankment Flood Refuge", "Elevated stilt concrete construction",
                "District Relief Office", true, "2026-02-13", System.currentTimeMillis(), System.currentTimeMillis()));

        // --- SUNAMGANJ & SYLHET (HAOR & FLASH FLOOD) ---
        list.add(new Shelter(29, "Chhatak Upazila Flood Shelter Centre",
                "ছাতক উপজেলা বন্যা আশ্রয়কেন্দ্র",
                "Sunamganj", "Chhatak", "Chhatak Sadar High Ground, Sunamganj",
                25.0410, 91.6738, 800, 0, "01712-000029",
                "Riverine Flood Refuge", "Elevated platform, deep tubewell, dedicated emergency medical room",
                "District Relief & Rehabilitation Office", true, "2026-03-01", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(30, "Tahirpur Tanguar Haor Emergency Flood Refuge",
                "তাহিরপুর টাঙ্গুয়ার হাওর বন্যা আশ্রয়কেন্দ্র",
                "Sunamganj", "Tahirpur", "Uttar Sreepur, Tanguar Haor Basin",
                25.1954, 91.1782, 1100, 0, "01712-000030",
                "Haor Basin Elevated Shelter", "Emergency boat mooring bay, raised earthen platform, clean water",
                "MoDMR Relief Wing", true, "2026-02-24", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(31, "Jamalganj Surma Riverbank Flood Shelter",
                "জামালগঞ্জ সুরমা নদী বন্যা আশ্রয়কেন্দ্র",
                "Sunamganj", "Jamalganj", "Beheli Union, Surma Riverbank",
                24.9962, 91.2284, 950, 0, "01712-000031",
                "Flash Flood Community Refuge", "Reinforced flood proofing, solar battery lights",
                "District Relief Office", true, "2026-03-02", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(32, "Companiganj High Ground Flood Refuge",
                "কোম্পানীগঞ্জ উঁচু প্ল্যাটফর্ম বন্যা আশ্রয়কেন্দ্র",
                "Sylhet", "Companiganj", "Islampur Union, Companiganj",
                25.0762, 91.7584, 950, 0, "01713-000032",
                "Flash Flood Community Shelter", "2-story elevated RCC structure, women's secure section",
                "District Relief & Rehabilitation Office", true, "2026-03-05", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(33, "Gowainghat Jaflong Basin Flood Shelter",
                "গোয়াইনঘাট জাফলং অববাহিকা বন্যা আশ্রয়কেন্দ্র",
                "Sylhet", "Gowainghat", "Rustampur Union, Piyain Riverfront",
                25.1833, 92.0167, 1050, 0, "01713-000033",
                "Piyain Flash Surge Shelter", "High-ground RCC structure with cattle barn",
                "MoDMR / Local Government", true, "2026-02-27", System.currentTimeMillis(), System.currentTimeMillis()));

        // --- KURIGRAM, GAIBANDHA & LALMONIRHAT (BRAHMAPUTRA / TEESTA) ---
        list.add(new Shelter(34, "Chilmari Riverbank Flood Shelter Point",
                "চিলমারী নদী তীরবর্তী বন্যা আশ্রয়কেন্দ্র",
                "Kurigram", "Chilmari", "Thanahat Union, Chilmari",
                25.5562, 89.6712, 1100, 0, "01714-000034",
                "Brahmaputra Basin Flood Shelter", "Elevated embankment compound with cattle protection killa",
                "MoDMR Relief Wing", true, "2026-02-20", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(35, "Roumari Jamuna Sandbar Flood Refuge",
                "রৌমারী যমুনা চর বন্যা আশ্রয়কেন্দ্র",
                "Kurigram", "Roumari", "Jadurchar Union, Brahmaputra Chars",
                25.5667, 89.8500, 1200, 0, "01714-000035",
                "Sandbar Community Refuge", "Raised earthen mound with double-story shelter, water tank",
                "MoDMR Relief Wing", true, "2026-02-22", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(36, "Fulchhari Brahmaputra Embankment Shelter",
                "ফুলছড়ি ব্রহ্মপুত্র বাঁধ আশ্রয়কেন্দ্র",
                "Gaibandha", "Fulchhari", "Gazaria Union, Brahmaputra Flood Wall",
                25.1833, 89.6167, 1150, 0, "01724-000036",
                "Riverine Embankment Shelter", "Livestock enclosure, solar lamps, emergency rations depot",
                "District Relief Office", true, "2026-02-16", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(37, "Hatibandha Teesta Barrage Flood Shelter",
                "হাতীবান্ধা তিস্তা ব্যারেজ বন্যা আশ্রয়কেন্দ্র",
                "Lalmonirhat", "Hatibandha", "Goddimari, Teesta Barrage Bypass",
                26.1833, 89.1333, 1000, 0, "01725-000037",
                "Teesta Basin Flash Shelter", "High elevation platform, potable water tubewell",
                "BWDB / MoDMR", true, "2026-02-19", System.currentTimeMillis(), System.currentTimeMillis()));

        // --- SIRAJGANJ & BOGURA (JAMUNA RIVER) ---
        list.add(new Shelter(38, "Kazipur Jamuna Embankment Flood Shelter",
                "কাজীপুর যমুনা বাঁধ বন্যা আশ্রয়কেন্দ্র",
                "Sirajganj", "Kazipur", "Natuarpara Char, Kazipur",
                24.6667, 89.6500, 1300, 0, "01726-000038",
                "Jamuna River Flood Shelter", "Elevated reinforced structure with community livestock enclosure",
                "MoDMR Relief Wing", true, "2026-02-07", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(39, "Sariakandi Jamuna Basin Flood Refuge",
                "সারিয়াকান্দি যমুনা অববাহিকা বন্যা আশ্রয়কেন্দ্র",
                "Bogura", "Sariakandi", "Kornibari Union, Jamuna Embankment",
                24.8833, 89.5667, 1200, 0, "01727-000039",
                "Char Disaster Refuge", "Two-tier concrete shelter, medical and sanitation blocks",
                "District Relief Office", true, "2026-02-11", System.currentTimeMillis(), System.currentTimeMillis()));

        // --- DHAKA, CHANDPUR & BARISAL (CENTRAL RIVERINE) ---
        list.add(new Shelter(40, "Dohar Padma Embankment Disaster Shelter",
                "দোহার পদ্মা বাঁধ দুর্যোগ আশ্রয়কেন্দ্র",
                "Dhaka", "Dohar", "Mirkamari, Padma Riverfront, Dohar",
                23.5833, 90.1333, 1000, 0, "01728-000040",
                "Riverine Multi-Hazard Shelter", "Elevated structure above 100-year Padma flood level",
                "MoDMR / Dhaka Relief Wing", true, "2026-02-25", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(41, "Dhamrai Bangshi River Flood Refuge",
                "ধামরাই বংশী নদী বন্যা আশ্রয়কেন্দ্র",
                "Dhaka", "Dhamrai", "Kushura Union, Bangshi Basin, Dhamrai",
                23.9167, 90.2167, 850, 0, "01728-000041",
                "River Flood Refuge Center", "Concrete multi-use facility with emergency generator",
                "MoDMR Relief Wing", true, "2026-02-28", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(42, "Haimchar Meghna River Erosion Shelter",
                "হাইমচর মেঘনা নদী ভাঙন আশ্রয়কেন্দ্র",
                "Chandpur", "Haimchar", "Char Bhairabi Coast, Haimchar",
                23.0667, 90.6333, 1100, 0, "01729-000042",
                "River Erosion Community Shelter", "Heavy reinforced foundation, solar power, medical triage room",
                "District Relief Office", true, "2026-02-03", System.currentTimeMillis(), System.currentTimeMillis()));

        list.add(new Shelter(43, "Mehendiganj Meghna Estuary Flood Shelter",
                "মেহেন্দিগঞ্জ মেঘনা মোহনা বন্যা আশ্রয়কেন্দ্র",
                "Barisal", "Mehendiganj", "Ulania Union, Mehendiganj",
                22.8167, 90.5333, 1250, 0, "01730-000043",
                "Estuary Flood & Surge Shelter", "Stilts design, cattle killa, fresh water harvesting tank",
                "MoDMR / CPP Bangladesh", true, "2026-01-23", System.currentTimeMillis(), System.currentTimeMillis()));

        return list;
    }

    private List<PreparednessChecklist> getSeedChecklists() {
        List<PreparednessChecklist> list = new ArrayList<>();
        list.add(new PreparednessChecklist(1, "GRAB_BAG", "Emergency Grab-Bag (72-Hour Survival)", "জরুরি সারভাইভাল ব্যাগ (৭২ ঘণ্টার জন্য)",
                "72-hour survival essentials for immediate evacuation", "জরুরি সময়ে অবিলম্বে আশ্রয়ে যাওয়ার জন্য প্রয়োজনীয় সরঞ্জাম", "ic_checklist", 1));

        list.add(new PreparednessChecklist(2, "DOCUMENTS", "Critical Documents & Valuables", "প্রয়োজনীয় নথিপত্র ও নগদ অর্থ",
                "Protection for legal and citizenship records in waterproof pouches", "জাতীয় পরিচয়পত্র ও নথিপত্র পানিতে ভেজা থেকে সুরক্ষা", "ic_shield_logo", 2));

        list.add(new PreparednessChecklist(3, "MEDICAL", "First Aid & Family Health", "ফার্স্ট এইড ও পারিবারিক ওষুধ",
                "Water purification tablets, ORS, and essential chronic prescriptions", "পানি বিশুদ্ধকরণ ট্যাবলেট, খাবার স্যালাইন ও জরুরি প্রেসক্রিপশন ওষুধ", "ic_heat", 3));

        list.add(new PreparednessChecklist(4, "HOME", "Home & Livestock Protection", "ঘরবাড়ি ও গবাদিপশু সুরক্ষা",
                "Safeguards for property, electrical hazards, and domestic animals", "ঘরের বিদ্যুৎ বিচ্ছিন্নকরণ ও গবাদিপশু নিরাপদ স্থানে স্থানান্তর", "ic_evacuation", 4));

        return list;
    }

    private List<ChecklistItem> filterSeedChecklistItems(long checklistId) {
        List<ChecklistItem> all = new ArrayList<>();
        // GRAB_BAG (1)
        all.add(new ChecklistItem(1, 1, "Drinking water (minimum 3 liters per person for 3 days)", "নিরাপদ খাবার পানি (জনপ্রতি প্রতিদিন ৩ লিটার করে ৩ দিনের জন্য)", false, true, System.currentTimeMillis()));
        all.add(new ChecklistItem(2, 1, "Non-perishable dry food (Chira, Muri, Gur, biscuits)", "শুকনো খাবার (চিঁড়া, মুড়ি, গুড়, বিস্কুট)", false, true, System.currentTimeMillis()));
        all.add(new ChecklistItem(3, 1, "Waterproof LED torch / flashlight with spare batteries", "ওয়াটারপ্রুফ এলইডি টর্চলাইট ও অতিরিক্ত ব্যাটারি", false, true, System.currentTimeMillis()));
        all.add(new ChecklistItem(4, 1, "Portable radio (for FFWC/BMD broadcasts)", "ছোট পোর্টেবল রেডিও (বন্যা ও আবহাওয়া পূর্বাভাসের জন্য)", false, false, System.currentTimeMillis()));
        all.add(new ChecklistItem(5, 1, "Fully charged power bank and phone charging cable", "চার্জড পাওয়ার ব্যাংক ও মোবাইল চার্জিং ক্যাবল", false, true, System.currentTimeMillis()));
        all.add(new ChecklistItem(6, 1, "Emergency whistle for signaling search and rescue", "উদ্ধারকারীদের সংকেত দেওয়ার জন্য বাঁশি (Whistle)", false, false, System.currentTimeMillis()));

        // DOCUMENTS (2)
        all.add(new ChecklistItem(7, 2, "National ID (NID), birth certificates in waterproof pouch", "জাতীয় পরিচয়পত্র (NID) ও জন্মনিবন্ধন পলিথিন ব্যাগে মোড়ানো", false, true, System.currentTimeMillis()));
        all.add(new ChecklistItem(8, 2, "Land deeds, house papers, and educational certificates", "জমির দলিল, বাড়ির কাগজ ও প্রয়োজনীয় সনদপত্র", false, false, System.currentTimeMillis()));
        all.add(new ChecklistItem(9, 2, "Emergency cash money in small denominations", "জরুরি নগদ টাকা (ভাঙতি নোট)", false, true, System.currentTimeMillis()));

        // MEDICAL (3)
        all.add(new ChecklistItem(10, 3, "First aid kit (bandages, antiseptic ointment, cotton)", "ফার্স্ট এইড কিট (ব্যান্ডেজ, অ্যান্টিসেপটিক, তুলা)", false, true, System.currentTimeMillis()));
        all.add(new ChecklistItem(11, 3, "Oral Rehydration Salts (ORS) packets (min 10 packets)", "খাবার স্যালাইন (কমপক্ষে ১০ প্যাকেট)", false, true, System.currentTimeMillis()));
        all.add(new ChecklistItem(12, 3, "Water purification tablets (Alum / Halazone)", "পানি বিশুদ্ধকরণ ট্যাবলেট (হ্যালোজেন / ফিটকিরি)", false, true, System.currentTimeMillis()));
        all.add(new ChecklistItem(13, 3, "Essential chronic medications (insulin, BP, asthma) for 7 days", "নিয়মিত খাওয়ার জরুরি ওষুধ (প্রেসার, ডায়াবেটিস, ইনসুলিন, ইনহেলার)", false, true, System.currentTimeMillis()));

        // HOME (4)
        all.add(new ChecklistItem(14, 4, "Main electrical circuit breaker and gas cylinder valve shut-off", "ঘরের বিদ্যুৎ মেইন সুইচ ও গ্যাস সিলিন্ডার রেগুলেটর বন্ধ করা", false, true, System.currentTimeMillis()));
        all.add(new ChecklistItem(15, 4, "Livestock untied and guided to high-elevation ground (Killa)", "গবাদিপশুর দড়ি খুলে উঁচু মাটির কিল্লা বা বাঁধে নিয়ে রাখা", false, true, System.currentTimeMillis()));

        List<ChecklistItem> filtered = new ArrayList<>();
        for (ChecklistItem it : all) {
            if (it.getChecklistId() == checklistId) {
                filtered.add(it);
            }
        }
        return filtered;
    }

    private void seedChecklistsInBatch(WriteBatch batch) {
        for (PreparednessChecklist cl : getSeedChecklists()) {
            DocumentReference clRef = db.collection(DatabaseContract.PreparednessChecklistEntry.TABLE_NAME).document("checklist_" + cl.getId());
            Map<String, Object> clMap = new HashMap<>();
            clMap.put(DatabaseContract.PreparednessChecklistEntry._ID, cl.getId());
            clMap.put(DatabaseContract.PreparednessChecklistEntry.COLUMN_CATEGORY_KEY, cl.getCategoryKey());
            clMap.put(DatabaseContract.PreparednessChecklistEntry.COLUMN_TITLE_EN, cl.getTitleEn());
            clMap.put(DatabaseContract.PreparednessChecklistEntry.COLUMN_TITLE_BN, cl.getTitleBn());
            clMap.put(DatabaseContract.PreparednessChecklistEntry.COLUMN_DESCRIPTION_EN, cl.getDescriptionEn());
            clMap.put(DatabaseContract.PreparednessChecklistEntry.COLUMN_DESCRIPTION_BN, cl.getDescriptionBn());
            clMap.put(DatabaseContract.PreparednessChecklistEntry.COLUMN_ICON_NAME, cl.getIconName());
            clMap.put(DatabaseContract.PreparednessChecklistEntry.COLUMN_DISPLAY_ORDER, cl.getDisplayOrder());
            batch.set(clRef, clMap);

            for (ChecklistItem item : filterSeedChecklistItems(cl.getId())) {
                DocumentReference itRef = db.collection(DatabaseContract.ChecklistItemEntry.TABLE_NAME).document("item_" + item.getId());
                Map<String, Object> itMap = new HashMap<>();
                itMap.put(DatabaseContract.ChecklistItemEntry._ID, item.getId());
                itMap.put(DatabaseContract.ChecklistItemEntry.COLUMN_CHECKLIST_ID, item.getChecklistId());
                itMap.put(DatabaseContract.ChecklistItemEntry.COLUMN_ITEM_EN, item.getItemEn());
                itMap.put(DatabaseContract.ChecklistItemEntry.COLUMN_ITEM_BN, item.getItemBn());
                itMap.put(DatabaseContract.ChecklistItemEntry.COLUMN_IS_COMPLETED, item.isCompleted() ? 1 : 0);
                itMap.put(DatabaseContract.ChecklistItemEntry.COLUMN_IS_ESSENTIAL, item.isEssential() ? 1 : 0);
                itMap.put(DatabaseContract.ChecklistItemEntry.COLUMN_UPDATED_AT, item.getUpdatedAt());
                batch.set(itRef, itMap);
            }
        }
    }
}
