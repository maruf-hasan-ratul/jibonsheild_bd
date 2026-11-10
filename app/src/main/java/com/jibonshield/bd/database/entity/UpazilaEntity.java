package com.jibonshield.bd.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "upazilas", indices = {@Index("districtId")})
public class UpazilaEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String nameBn;
    private String districtId;
    private String code;
    private boolean isActive;
    private long updatedAt;

    public UpazilaEntity(@NonNull String id, String name, String nameBn, String districtId, String code, boolean isActive, long updatedAt) {
        this.id = id;
        this.name = name;
        this.nameBn = nameBn;
        this.districtId = districtId;
        this.code = code;
        this.isActive = isActive;
        this.updatedAt = updatedAt;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameBn() {
        return nameBn;
    }

    public void setNameBn(String nameBn) {
        this.nameBn = nameBn;
    }

    public String getDistrictId() {
        return districtId;
    }

    public void setDistrictId(String districtId) {
        this.districtId = districtId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}

