package com.jibonshield.bd.firebase;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jibonshield.bd.models.LiveEmergencyIncident;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager for Firebase Realtime Database.
 * Powers real-time emergency broadcasts, shelter occupancy synchronization,
 * and live volunteer mobilization status across active disaster zones.
 */
public class RealtimeDatabaseManager {

    private static final String TAG = "RealtimeDbManager";
    private static RealtimeDatabaseManager instance;

    public static final String NODE_EMERGENCY_STATUS = "emergency_status";
    public static final String NODE_SHELTER_OCCUPANCY = "shelter_occupancy";
    public static final String NODE_VOLUNTEER_STATUS = "volunteer_status";

    public interface EmergencyStatusListener {
        void onStatusUpdated(List<LiveEmergencyIncident> incidents);
        void onError(String message);
    }

    public interface OccupancyListener {
        void onOccupancyUpdated(int capacity, int occupancy, long lastUpdated);
        void onError(String message);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(String error);
    }

    private final Context context;
    private FirebaseDatabase database;
    private ValueEventListener emergencyStatusListener;
    private DatabaseReference emergencyStatusRef;

    public static synchronized RealtimeDatabaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new RealtimeDatabaseManager(context.getApplicationContext());
        }
        return instance;
    }

    private RealtimeDatabaseManager(Context context) {
        this.context = context;
        try {
            if (!FirebaseApp.getApps(context).isEmpty()) {
                this.database = FirebaseDatabase.getInstance();
            }
        } catch (Exception e) {
            Log.w(TAG, "Realtime Database initialization warning: " + e.getMessage());
        }
    }

    public boolean isConfigured() {
        return database != null;
    }

    // =========================================================================
    // 1. LIVE EMERGENCY STATUS
    // =========================================================================

    public void listenToEmergencyStatus(EmergencyStatusListener listener) {
        if (!isConfigured()) {
            if (listener != null) listener.onError("Realtime Database is not configured.");
            return;
        }

        stopListeningToEmergencyStatus();

        emergencyStatusRef = database.getReference(NODE_EMERGENCY_STATUS);
        emergencyStatusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<LiveEmergencyIncident> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        LiveEmergencyIncident incident = child.getValue(LiveEmergencyIncident.class);
                        if (incident != null) {
                            if (incident.getIncidentId() == null) {
                                incident.setIncidentId(child.getKey());
                            }
                            list.add(incident);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error deserializing incident", e);
                    }
                }
                if (listener != null) listener.onStatusUpdated(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (listener != null) listener.onError(error.getMessage());
            }
        };

        emergencyStatusRef.addValueEventListener(emergencyStatusListener);
    }

    public void stopListeningToEmergencyStatus() {
        if (emergencyStatusRef != null && emergencyStatusListener != null) {
            emergencyStatusRef.removeEventListener(emergencyStatusListener);
            emergencyStatusListener = null;
        }
    }

    // =========================================================================
    // 2. SHELTER OCCUPANCY
    // =========================================================================

    public void updateShelterOccupancy(String shelterId, int capacity, int occupancy, ActionCallback callback) {
        if (!isConfigured() || shelterId == null) {
            if (callback != null) callback.onFailure("Realtime Database is not configured.");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("capacity", capacity);
        data.put("occupancy", occupancy);
        data.put("last_updated", System.currentTimeMillis());

        database.getReference(NODE_SHELTER_OCCUPANCY).child(shelterId)
                .setValue(data)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }

    public void listenToShelterOccupancy(String shelterId, OccupancyListener listener) {
        if (!isConfigured() || shelterId == null) {
            if (listener != null) listener.onError("Realtime Database is not configured.");
            return;
        }

        database.getReference(NODE_SHELTER_OCCUPANCY).child(shelterId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Long cap = snapshot.child("capacity").getValue(Long.class);
                            Long occ = snapshot.child("occupancy").getValue(Long.class);
                            Long updated = snapshot.child("last_updated").getValue(Long.class);
                            if (listener != null) {
                                listener.onOccupancyUpdated(
                                        cap != null ? cap.intValue() : 0,
                                        occ != null ? occ.intValue() : 0,
                                        updated != null ? updated : 0
                                );
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (listener != null) listener.onError(error.getMessage());
                    }
                });
    }

    // =========================================================================
    // 3. VOLUNTEER AVAILABILITY
    // =========================================================================

    public void updateVolunteerStatus(String volunteerId, String status, boolean available, ActionCallback callback) {
        if (!isConfigured() || volunteerId == null) {
            if (callback != null) callback.onFailure("Realtime Database is not configured.");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("status", status);
        data.put("availability", available);
        data.put("last_updated", System.currentTimeMillis());

        database.getReference(NODE_VOLUNTEER_STATUS).child(volunteerId)
                .setValue(data)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onFailure(e.getMessage());
                });
    }
}

