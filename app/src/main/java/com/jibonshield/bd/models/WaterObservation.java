package com.jibonshield.bd.models;

import java.io.Serializable;

/**
 * Model representing a drinking water quality field test observation.
 */
public class WaterObservation implements Serializable {
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

    public WaterObservation(long id, String locationName, String district, String sourceType,
                            String appearance, String odor, double phLevel,
                            String turbidityStatus, String safetyStatus,
                            String observationTime, String reportedBy, String remarks) {
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
}
