package com.jibonshield.bd.models;

import java.io.Serializable;

/**
 * Model representing a verified or sample flood warning alert.
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
    private final boolean isActive;
    private final boolean isSample;

    public FloodWarning(long id, String warningLevel, String titleEn, String titleBn, String region,
                        String detailsEn, String detailsBn, String issuedAt, String expiresAt,
                        String source, boolean isActive, boolean isSample) {
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
        this.isActive = isActive;
        this.isSample = isSample;
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
    public boolean isActive() { return isActive; }
    public boolean isSample() { return isSample; }
}
