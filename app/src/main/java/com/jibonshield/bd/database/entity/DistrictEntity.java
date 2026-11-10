package com.jibonshield.bd.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "districts", indices = {@Index("divisionId")})
public class DistrictEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String nameBn;
    private String divisionId;
    private String code;
    private boolean isActive;
    private long updatedAt;

    public DistrictEntity(@NonNull String id, String name, String nameBn, String divisionId, String code, boolean isActive, long updatedAt) {
        this.id = id;
        this.name = name;
        this.nameBn = nameBn;
        this.divisionId = divisionId;
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

    public String getDivisionId() {
        return divisionId;
    }

    public void setDivisionId(String divisionId) {
        this.divisionId = divisionId;
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

