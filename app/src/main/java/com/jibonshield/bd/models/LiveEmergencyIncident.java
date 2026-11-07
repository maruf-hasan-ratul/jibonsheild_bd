package com.jibonshield.bd.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Model representing a real-time disaster emergency broadcast incident.
 * Synchronized live via Firebase Realtime Database.
 */
public class LiveEmergencyIncident implements Serializable {

    private String incidentId;
    private String severity; // CRITICAL, WARNING, ADVISORY, NORMAL
    private String district;
    private String message;
    private long timestamp;
    private String source;
    private boolean verified;

    public LiveEmergencyIncident() {
        // Default constructor for Realtime Database deserialization
    }

    public LiveEmergencyIncident(String incidentId, String severity, String district,
                                 String message, long timestamp, String source, boolean verified) {
        this.incidentId = incidentId;
        this.severity = severity;
        this.district = district;
        this.message = message;
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
        this.source = source;
        this.verified = verified;
    }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("incidentId", incidentId);
        map.put("severity", severity);
        map.put("district", district);
        map.put("message", message);
        map.put("timestamp", timestamp);
        map.put("source", source);
        map.put("verified", verified);
        return map;
    }
}

