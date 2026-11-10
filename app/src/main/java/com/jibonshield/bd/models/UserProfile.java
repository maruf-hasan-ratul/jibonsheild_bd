package com.jibonshield.bd.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Model representing an authenticated user profile in JibonShield BD.
 * Supports role-based safety capabilities and hometown location hierarchy.
 */
public class UserProfile implements Serializable {

    public static final String ROLE_CITIZEN = "CITIZEN";
    public static final String ROLE_VOLUNTEER = "VOLUNTEER";

    private String uid;
    private String name;
    private String email;
    private String phone;
    private String displayName; // backward-compat alias for name
    private String role; // CITIZEN or VOLUNTEER
    private String district; // backward-compat alias
    private String phoneNumber; // backward-compat alias for phone
    private String divisionId;
    private String districtId;
    private String upazilaId;
    private boolean hometownConfigured;
    private long createdAt;
    private long updatedAt;

    public UserProfile() {
        // Required for Firestore deserialization
    }

    public UserProfile(String uid, String name, String email, String phone,
                       String divisionId, String districtId, String upazilaId,
                       boolean hometownConfigured, long updatedAt) {
        this.uid = uid;
        this.name = name;
        this.displayName = name;
        this.email = email;
        this.phone = phone;
        this.phoneNumber = phone;
        this.divisionId = divisionId;
        this.districtId = districtId;
        this.district = districtId;
        this.upazilaId = upazilaId;
        this.hometownConfigured = hometownConfigured;
        this.role = ROLE_CITIZEN;
        this.updatedAt = updatedAt > 0 ? updatedAt : System.currentTimeMillis();
        this.createdAt = this.updatedAt;
    }

    public UserProfile(String uid, String email, String displayName, String role,
                       String district, String phoneNumber, long createdAt) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.name = displayName;
        this.role = role != null ? role : ROLE_CITIZEN;
        this.district = district;
        this.districtId = district != null ? district.toLowerCase().replace(" ", "_") : "";
        this.phoneNumber = phoneNumber;
        this.phone = phoneNumber;
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.hometownConfigured = district != null && !district.isEmpty();
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name != null ? name : displayName; }
    public void setName(String name) {
        this.name = name;
        this.displayName = name;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone != null ? phone : phoneNumber; }
    public void setPhone(String phone) {
        this.phone = phone;
        this.phoneNumber = phone;
    }

    public String getDisplayName() { return displayName != null ? displayName : name; }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        this.name = displayName;
    }

    public String getRole() { return role != null ? role : ROLE_CITIZEN; }
    public void setRole(String role) { this.role = role; }

    public String getDistrict() { return district != null ? district : districtId; }
    public void setDistrict(String district) {
        this.district = district;
        if (this.districtId == null || this.districtId.isEmpty()) {
            this.districtId = district != null ? district.toLowerCase().replace(" ", "_") : "";
        }
    }

    public String getPhoneNumber() { return phoneNumber != null ? phoneNumber : phone; }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        this.phone = phoneNumber;
    }

    public String getDivisionId() { return divisionId; }
    public void setDivisionId(String divisionId) { this.divisionId = divisionId; }

    public String getDistrictId() { return districtId != null ? districtId : district; }
    public void setDistrictId(String districtId) {
        this.districtId = districtId;
        this.district = districtId;
    }

    public String getUpazilaId() { return upazilaId; }
    public void setUpazilaId(String upazilaId) { this.upazilaId = upazilaId; }

    public boolean isHometownConfigured() { return hometownConfigured; }
    public void setHometownConfigured(boolean hometownConfigured) { this.hometownConfigured = hometownConfigured; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isVolunteer() {
        return ROLE_VOLUNTEER.equalsIgnoreCase(role);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("name", getName());
        map.put("displayName", getDisplayName());
        map.put("email", email);
        map.put("phone", getPhone());
        map.put("phoneNumber", getPhoneNumber());
        map.put("role", getRole());
        map.put("district", getDistrict());
        map.put("divisionId", divisionId != null ? divisionId : "");
        map.put("districtId", districtId != null ? districtId : "");
        map.put("upazilaId", upazilaId != null ? upazilaId : "");
        map.put("hometownConfigured", hometownConfigured);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        return map;
    }
}
