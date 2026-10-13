package com.jibonshield.bd.models;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Model representing an official or simulated flood warning alert.
 */
public class FloodWarning implements Serializable {
    private final long id;
    private final String warningLevel; // "CRITICAL", "WARNING", "ADVISORY", "NORMAL"
    private final String titleEn;
    private final String titleBn;
    private final String region;
    private final String detailsEn;
    private final String detailsBn;
    private final String issuedAt;
    private final String expiresAt;
    private final String source;
    private final String sourceReference;
    private final boolean isActive;
    private final boolean isSample;
    private final long lastSyncedAt;

    /**
     * Full constructor.
     */
    public FloodWarning(long id, String warningLevel, String titleEn, String titleBn, String region,
                        String detailsEn, String detailsBn, String issuedAt, String expiresAt,
                        String source, String sourceReference, boolean isActive, boolean isSample,
                        long lastSyncedAt) {
        this.id = id;
        this.warningLevel = warningLevel;
        this.titleEn = titleEn;
        this.titleBn = titleBn;
        this.region = region;
        this.detailsEn = detailsEn;
        this.detailsBn = detailsBn;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.source = source;
        this.sourceReference = sourceReference != null ? sourceReference : "FFWC-BULLETIN";
        this.isActive = isActive;
        this.isSample = isSample;
        this.lastSyncedAt = lastSyncedAt > 0 ? lastSyncedAt : System.currentTimeMillis();
    }

    /**
     * Backward-compatible constructor.
     */
    public FloodWarning(long id, String warningLevel, String titleEn, String titleBn, String region,
                        String detailsEn, String detailsBn, String issuedAt, String expiresAt,
                        String source, boolean isActive, boolean isSample) {
        this(id, warningLevel, titleEn, titleBn, region, detailsEn, detailsBn, issuedAt, expiresAt,
                source, "FFWC-BULLETIN", isActive, isSample, System.currentTimeMillis());
    }

    public long getId() { return id; }
    public String getWarningLevel() { return warningLevel; }
    public String getTitleEn() { return titleEn; }
    public String getTitleBn() { return titleBn; }
    public String getRegion() { return region; }
    public String getDetailsEn() { return detailsEn; }
    public String getDetailsBn() { return detailsBn; }
    public String getIssuedAt() { return issuedAt; }
    public String getExpiresAt() { return expiresAt; }
    public String getSource() { return source; }
    public String getSourceReference() { return sourceReference; }
    public boolean isActive() { return isActive; }
    public boolean isSample() { return isSample; }
    public long getLastSyncedAt() { return lastSyncedAt; }

    /**
     * Helper to verify if the warning has expired by date/time or active flag.
     */
    public boolean isExpired() {
        if (!isActive) return true;
        try {
            // Attempt standard parse (e.g. "2026-10-14 18:00 BST")
            String cleanDate = expiresAt;
            if (cleanDate.endsWith(" BST")) {
                cleanDate = cleanDate.replace(" BST", "");
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
            Date expiry = sdf.parse(cleanDate);
            if (expiry != null && expiry.before(new Date())) {
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }
}
