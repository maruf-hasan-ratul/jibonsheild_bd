package com.jibonshield.bd.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_profiles")
public class UserProfileEntity {

    @PrimaryKey
    @NonNull
    private String uid;
    private String name;
    private String email;
    private String phone;
    private String divisionId;
    private String districtId;
    private String upazilaId;
    private boolean hometownConfigured;
    private long updatedAt;

    public UserProfileEntity(@NonNull String uid, String name, String email, String phone,
                             String divisionId, String districtId, String upazilaId,
                             boolean hometownConfigured, long updatedAt) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.divisionId = divisionId;
        this.districtId = districtId;
        this.upazilaId = upazilaId;
        this.hometownConfigured = hometownConfigured;
        this.updatedAt = updatedAt;
    }

    @NonNull
    public String getUid() {
        return uid;
    }

    public void setUid(@NonNull String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDivisionId() {
        return divisionId;
    }

    public void setDivisionId(String divisionId) {
        this.divisionId = divisionId;
    }

    public String getDistrictId() {
        return districtId;
    }

    public void setDistrictId(String districtId) {
        this.districtId = districtId;
    }

    public String getUpazilaId() {
        return upazilaId;
    }

    public void setUpazilaId(String upazilaId) {
        this.upazilaId = upazilaId;
    }

    public boolean isHometownConfigured() {
        return hometownConfigured;
    }

    public void setHometownConfigured(boolean hometownConfigured) {
        this.hometownConfigured = hometownConfigured;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}

