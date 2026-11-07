package com.jibonshield.bd.firebase;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.jibonshield.bd.models.UserProfile;
import com.jibonshield.bd.models.WaterObservation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager for Cloud Firestore operations in JibonShield BD.
 * Handles structured persistence for users, community water observations,
 * and disaster incident logs with offline persistence enabled.
 */
public class FirestoreManager {

    private static final String TAG = "FirestoreManager";
    private static FirestoreManager instance;

    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_WATER_OBSERVATIONS = "water_observations";
    public static final String COLLECTION_DISASTER_REPORTS = "disaster_reports";
    public static final String COLLECTION_VOLUNTEER_REPORTS = "volunteer_reports";
    public static final String COLLECTION_INCIDENT_HISTORY = "incident_history";

    public interface FirestoreCallback {
        void onSuccess(String documentId);
        void onFailure(String errorMessage);
    }

    public interface ProfileResultCallback {
        void onSuccess(UserProfile profile);
        void onFailure(String errorMessage);
    }

    public interface ObservationsResultCallback {
        void onSuccess(List<WaterObservation> observations);
        void onFailure(String errorMessage);
    }

    private final Context context;
    private FirebaseFirestore db;

    public static synchronized FirestoreManager getInstance(Context context) {
        if (instance == null) {
            instance = new FirestoreManager(context.getApplicationContext());
        }
        return instance;
    }

    private FirestoreManager(Context context) {
        this.context = context;
        try {
            if (!FirebaseApp.getApps(context).isEmpty()) {
                this.db = FirebaseFirestore.getInstance();
            }
        } catch (Exception e) {
            Log.w(TAG, "Firestore initialization warning: " + e.getMessage());
        }
    }

    public boolean isConfigured() {
        return db != null;
    }

    // =========================================================================
    // USER PROFILES
    // =========================================================================

    public void saveUserProfile(UserProfile profile, FirestoreCallback callback) {
        if (!isConfigured() || profile == null || profile.getUid() == null) {
            if (callback != null) callback.onFailure("Firestore is not available.");
            return;
        }

        db.collection(COLLECTION_USERS)
                .document(profile.getUid())
                .set(profile.toMap())
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess(profile.getUid());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user profile", e);
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public void getUserProfile(String uid, ProfileResultCallback callback) {
        if (!isConfigured() || uid == null) {
            if (callback != null) callback.onFailure("Firestore is not available.");
            return;
        }

        db.collection(COLLECTION_USERS)
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserProfile profile = documentSnapshot.toObject(UserProfile.class);
                        if (callback != null) callback.onSuccess(profile);
                    } else {
                        if (callback != null) callback.onFailure("User profile not found.");
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    // =========================================================================
    // COMMUNITY WATER OBSERVATIONS
    // =========================================================================

    public void saveWaterObservation(WaterObservation obs, String userId, FirestoreCallback callback) {
        if (!isConfigured() || obs == null) {
            if (callback != null) callback.onFailure("Firestore is not available.");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("localId", obs.getId());
        data.put("userId", userId != null ? userId : "anonymous_field_user");
        data.put("locationName", obs.getLocationName());
        data.put("district", obs.getDistrict());
        data.put("sourceType", obs.getSourceType());
        data.put("appearance", obs.getAppearance());
        data.put("odor", obs.getOdor());
        data.put("ph", obs.getPhLevel());
        data.put("turbidity", obs.getTurbidityStatus());
        data.put("safetyStatus", obs.getSafetyStatus());
        data.put("observationTime", obs.getObservationTime());
        data.put("reportedBy", obs.getReportedBy());
        data.put("remarks", obs.getRemarks());
        data.put("imageUrl", obs.getImageUrl());
        data.put("timestamp", obs.getCreatedAt());
        data.put("synchronizationStatus", "SYNCED");

        CollectionReference collection = db.collection(COLLECTION_WATER_OBSERVATIONS);
        String docId = obs.getId() > 0 ? "obs_" + obs.getId() : collection.document().getId();

        collection.document(docId)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess(docId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error syncing observation to Firestore", e);
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public void getCommunityWaterObservations(ObservationsResultCallback callback) {
        if (!isConfigured()) {
            if (callback != null) callback.onFailure("Firestore is not available.");
            return;
        }

        db.collection(COLLECTION_WATER_OBSERVATIONS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<WaterObservation> list = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            String loc = doc.getString("locationName");
                            String dist = doc.getString("district");
                            String src = doc.getString("sourceType");
                            String app = doc.getString("appearance");
                            String odor = doc.getString("odor");
                            Double ph = doc.getDouble("ph");
                            String turb = doc.getString("turbidity");
                            String safe = doc.getString("safetyStatus");
                            String time = doc.getString("observationTime");
                            String rep = doc.getString("reportedBy");
                            String rem = doc.getString("remarks");
                            String imgUrl = doc.getString("imageUrl");
                            Long ts = doc.getLong("timestamp");

                            WaterObservation item = new WaterObservation(
                                    0, loc, dist, src, app, odor,
                                    ph != null ? ph : -1.0,
                                    turb != null ? turb : "Moderate",
                                    safe != null ? safe : "Potable",
                                    time != null ? time : "Recent",
                                    rep != null ? rep : "Community Volunteer",
                                    rem, null, imgUrl, "SYNCED",
                                    ts != null ? ts : System.currentTimeMillis(),
                                    System.currentTimeMillis()
                            );
                            list.add(item);
                        } catch (Exception e) {
                            Log.w(TAG, "Could not parse Firestore observation", e);
                        }
                    }
                    if (callback != null) callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }
}

