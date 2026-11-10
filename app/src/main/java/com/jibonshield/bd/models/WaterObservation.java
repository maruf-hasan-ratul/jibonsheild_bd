package com.jibonshield.bd.models;

import java.io.Serializable;

/**
 * Model representing a drinking water quality field test observation.
 * Follows strict environmental safety integrity: community field records
 * must NOT be treated as certified laboratory test results or absolute safety certification.
 */
public class WaterObservation implements Serializable {

    public static final double MIN_PH = 0.0;
    public static final double MAX_PH = 14.0;
    public static final int MAX_NOTES_LENGTH = 500;
    public static final double UNMEASURED_PH = -1.0;
    public static final String SYNC_PENDING = "PENDING";
    public static final String SYNC_SYNCED = "SYNCED";
    public static final String SYNC_FAILED = "FAILED";

    private final long id;
    private final String locationName;
    private final String district;
    private final String sourceType;
    private final String appearance;
    private final String odor;
    private final double phLevel;
    private final String turbidityStatus;
    private final String safetyStatus;
    private final String observationTime;
    private final String reportedBy;
    private final String remarks;
    private final String imageUri;
    private final String imageUrl;
    private final String syncStatus;
    private final String userId;
    private final long createdAt;
    private final long updatedAt;

    /**
     * Master constructor with image, sync, and user parameters.
     */
    public WaterObservation(long id, String locationName, String district, String sourceType,
                            String appearance, String odor, double phLevel,
                            String turbidityStatus, String safetyStatus,
                            String observationTime, String reportedBy, String remarks,
                            String imageUri, String imageUrl, String syncStatus,
                            String userId,
                            long createdAt, long updatedAt) {
        this.id = id;
        this.locationName = locationName;
        this.district = district;
        this.sourceType = sourceType;
        this.appearance = appearance;
        this.odor = odor;
        this.phLevel = phLevel;
        this.turbidityStatus = turbidityStatus;
        this.safetyStatus = safetyStatus;
        this.observationTime = observationTime;
        this.reportedBy = reportedBy;
        this.remarks = remarks;
        this.imageUri = imageUri;
        this.imageUrl = imageUrl;
        this.syncStatus = syncStatus != null ? syncStatus : SYNC_PENDING;
        this.userId = userId != null ? userId : "";
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
        this.updatedAt = updatedAt > 0 ? updatedAt : System.currentTimeMillis();
    }

    /**
     * Constructor with image and sync parameters without user ID.
     */
    public WaterObservation(long id, String locationName, String district, String sourceType,
                            String appearance, String odor, double phLevel,
                            String turbidityStatus, String safetyStatus,
                            String observationTime, String reportedBy, String remarks,
                            String imageUri, String imageUrl, String syncStatus,
                            long createdAt, long updatedAt) {
        this(id, locationName, district, sourceType, appearance, odor, phLevel,
                turbidityStatus, safetyStatus, observationTime, reportedBy, remarks,
                imageUri, imageUrl, syncStatus, "", createdAt, updatedAt);
    }

    /**
     * Constructor without image and sync metadata.
     */
    public WaterObservation(long id, String locationName, String district, String sourceType,
                            String appearance, String odor, double phLevel,
                            String turbidityStatus, String safetyStatus,
                            String observationTime, String reportedBy, String remarks,
                            long createdAt, long updatedAt) {
        this(id, locationName, district, sourceType, appearance, odor, phLevel,
                turbidityStatus, safetyStatus, observationTime, reportedBy, remarks,
                null, null, SYNC_PENDING, createdAt, updatedAt);
    }

    /**
     * Backward-compatible constructor.
     */
    public WaterObservation(long id, String locationName, String district, String sourceType,
                            String appearance, String odor, double phLevel,
                            String turbidityStatus, String safetyStatus,
                            String observationTime, String reportedBy, String remarks) {
        this(id, locationName, district, sourceType, appearance, odor, phLevel,
                turbidityStatus, safetyStatus, observationTime, reportedBy, remarks,
                null, null, SYNC_PENDING,
                System.currentTimeMillis(), System.currentTimeMillis());
    }

    public long getId() { return id; }
    public String getLocationName() { return locationName; }
    public String getDistrict() { return district; }
    public String getSourceType() { return sourceType; }
    public String getAppearance() { return appearance; }
    public String getOdor() { return odor; }
    public double getPhLevel() { return phLevel; }
    public String getTurbidityStatus() { return turbidityStatus; }
    public String getSafetyStatus() { return safetyStatus; }
    public String getObservationTime() { return observationTime; }
    public String getReportedBy() { return reportedBy; }
    public String getRemarks() { return remarks; }
    public String getImageUri() { return imageUri; }
    public String getImageUrl() { return imageUrl; }
    public String getSyncStatus() { return syncStatus; }
    public boolean hasImage() { return (imageUri != null && !imageUri.trim().isEmpty()) || (imageUrl != null && !imageUrl.trim().isEmpty()); }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public String getUserId() { return userId != null ? userId : ""; }

    /**
     * Strict ownership check: Only the person who created this observation can edit or delete it.
     */
    public boolean isCreatedBy(String currentUserId, String currentUserName, String currentUserEmail) {
        // 1. If observation has a specific userId attached, it MUST match currentUserId strictly!
        if (userId != null && !userId.trim().isEmpty()) {
            return currentUserId != null && !currentUserId.trim().isEmpty()
                    && userId.trim().equalsIgnoreCase(currentUserId.trim());
        }

        // 2. If legacy observation has no userId, match strictly against creator's email or exact name
        if (reportedBy != null && !reportedBy.trim().isEmpty() && !"Citizen".equalsIgnoreCase(reportedBy.trim())) {
            if (currentUserEmail != null && !currentUserEmail.trim().isEmpty()
                    && reportedBy.trim().equalsIgnoreCase(currentUserEmail.trim())) {
                return true;
            }
            if (currentUserName != null && !currentUserName.trim().isEmpty()
                    && !currentUserName.equalsIgnoreCase("User")
                    && !currentUserName.equalsIgnoreCase("Citizen")
                    && !currentUserName.equalsIgnoreCase("JibonShield User")
                    && reportedBy.trim().equalsIgnoreCase(currentUserName.trim())) {
                return true;
            }
        }
        return false;
    }

    public boolean isPhMeasured() {
        return phLevel >= MIN_PH && phLevel <= MAX_PH;
    }

    // =========================================================================
    // FIELD VALIDATION RULES (Tested in Unit Tests)
    // =========================================================================

    public static boolean isValidLocation(String location) {
        return location != null && !location.trim().isEmpty();
    }

    public static boolean isValidNotes(String notes) {
        return notes == null || notes.trim().length() <= MAX_NOTES_LENGTH;
    }

    public static boolean isValidPhString(String phStr) {
        if (phStr == null || phStr.trim().isEmpty()) {
            return true; // Optional measurement
        }
        try {
            double val = Double.parseDouble(phStr.trim());
            return val >= MIN_PH && val <= MAX_PH;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidPhValue(double ph) {
        return (ph >= MIN_PH && ph <= MAX_PH) || ph == UNMEASURED_PH;
    }
}
