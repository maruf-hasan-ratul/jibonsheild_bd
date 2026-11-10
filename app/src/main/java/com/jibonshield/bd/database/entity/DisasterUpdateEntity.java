package com.jibonshield.bd.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.jibonshield.bd.database.converters.StringListConverter;

import java.util.ArrayList;
import java.util.List;

@Entity(
    tableName = "disaster_updates",
    indices = {
        @Index("divisionId"),
        @Index("districtId"),
        @Index("upazilaId"),
        @Index("disasterType"),
        @Index("severity"),
        @Index("isActive"),
        @Index("issuedAt")
    }
)
public class DisasterUpdateEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String title;
    private String titleBn;
    private String description;
    private String descriptionBn;
    private String disasterType;
    private String severity;
    private String status;
    private String divisionId;
    private String districtId;
    private String upazilaId;

    @TypeConverters(StringListConverter.class)
    private List<String> affectedAreas;

    private String issuedBy;
    private long issuedAt;
    private long expiresAt;
    private long updatedAt;
    private boolean isActive;

    public DisasterUpdateEntity(@NonNull String id, String title, String titleBn,
                                String description, String descriptionBn, String disasterType,
                                String severity, String status, String divisionId,
                                String districtId, String upazilaId, List<String> affectedAreas,
                                String issuedBy, long issuedAt, long expiresAt,
                                long updatedAt, boolean isActive) {
        this.id = id;
        this.title = title;
        this.titleBn = titleBn;
        this.description = description;
        this.descriptionBn = descriptionBn;
        this.disasterType = disasterType;
        this.severity = severity;
        this.status = status;
        this.divisionId = divisionId;
        this.districtId = districtId;
        this.upazilaId = upazilaId;
        this.affectedAreas = affectedAreas != null ? affectedAreas : new ArrayList<>();
        this.issuedBy = issuedBy;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.updatedAt = updatedAt;
        this.isActive = isActive;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleBn() {
        return titleBn;
    }

    public void setTitleBn(String titleBn) {
        this.titleBn = titleBn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescriptionBn() {
        return descriptionBn;
    }

    public void setDescriptionBn(String descriptionBn) {
        this.descriptionBn = descriptionBn;
    }

    public String getDisasterType() {
        return disasterType;
    }

    public void setDisasterType(String disasterType) {
        this.disasterType = disasterType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public List<String> getAffectedAreas() {
        return affectedAreas;
    }

    public void setAffectedAreas(List<String> affectedAreas) {
        this.affectedAreas = affectedAreas;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public long getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(long issuedAt) {
        this.issuedAt = issuedAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}

