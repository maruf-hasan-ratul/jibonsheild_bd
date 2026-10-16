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
     * Documented Timestamp Policy:
     * Standard timestamp format: "yyyy-MM-dd HH:mm [BST]" or ISO-8601 "yyyy-MM-dd'T'HH:mm:ss".
     * Expiry Rule:
     * A warning is considered expired if:
     * 1. isActive flag is false.
     * 2. expiresAt is null, empty, or whitespace ("Do not treat missing expiry information as proof that a warning remains valid").
     * 3. Current time is after the parsed expiresAt timestamp.
     * 4. Date format cannot be validated or parsed (unverifiable timestamps cannot be treated as valid).
     */
    public boolean isExpired() {
        if (!isActive) return true;
        if (expiresAt == null || expiresAt.trim().isEmpty()) {
            // Missing expiry information must never be treated as proof of validity
            return true;
        }

        String cleanDate = expiresAt.trim();
        if (cleanDate.endsWith(" BST")) {
            cleanDate = cleanDate.substring(0, cleanDate.length() - 4).trim();
        }

        String[] formats = new String[]{
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
        };

        for (String format : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);
                sdf.setLenient(false);
                Date expiry = sdf.parse(cleanDate);
                if (expiry != null) {
                    return expiry.before(new Date());
                }
            } catch (Exception ignored) {
            }
        }

        // If unparseable, we cannot verify validity; treat as expired
        return true;
    }

    public enum WarningStatus {
        CURRENT,
        CACHED,
        EXPIRED,
        UNAVAILABLE
    }

    public enum VerificationStatus {
        OFFICIAL_VERIFIED,
        DEVELOPMENT_SAMPLE,
        UNVERIFIED
    }

    public WarningStatus getWarningStatus() {
        if (isExpired()) {
            return WarningStatus.EXPIRED;
        }
        if (isSample) {
            return WarningStatus.CACHED;
        }
        // If synced within the last 24 hours
        long now = System.currentTimeMillis();
        if (now - lastSyncedAt <= 24 * 60 * 60 * 1000L) {
            return WarningStatus.CURRENT;
        } else {
            return WarningStatus.CACHED;
        }
    }

    public VerificationStatus getVerificationStatus() {
        if (isSample) {
            return VerificationStatus.DEVELOPMENT_SAMPLE;
        }
        if (source != null && !source.trim().isEmpty() &&
                (source.contains("FFWC") || source.contains("BWDB") || source.contains("BMD") || source.contains("MoDMR") || source.contains("Government"))) {
            return VerificationStatus.OFFICIAL_VERIFIED;
        }
        return VerificationStatus.UNVERIFIED;
    }
}
