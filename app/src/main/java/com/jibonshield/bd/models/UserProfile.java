package com.jibonshield.bd.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Model representing an authenticated user profile in JibonShield BD.
 * Supports role-based safety capabilities (Citizen vs Emergency Volunteer).
 */
public class UserProfile implements Serializable {

    public static final String ROLE_CITIZEN = "CITIZEN";
    public static final String ROLE_VOLUNTEER = "VOLUNTEER";

    private String uid;
    private String email;
    private String displayName;
    private String role; // CITIZEN or VOLUNTEER
    private String district;
    private String phoneNumber;
    private long createdAt;

    public UserProfile() {
        // Required for Firestore deserialization
    }

    public UserProfile(String uid, String email, String displayName, String role,
                       String district, String phoneNumber, long createdAt) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.role = role != null ? role : ROLE_CITIZEN;
        this.district = district;
        this.phoneNumber = phoneNumber;
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getRole() { return role != null ? role : ROLE_CITIZEN; }
    public void setRole(String role) { this.role = role; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isVolunteer() {
        return ROLE_VOLUNTEER.equalsIgnoreCase(role);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("email", email);
        map.put("displayName", displayName);
        map.put("role", getRole());
        map.put("district", district);
        map.put("phoneNumber", phoneNumber);
        map.put("createdAt", createdAt);
        return map;
    }
}

