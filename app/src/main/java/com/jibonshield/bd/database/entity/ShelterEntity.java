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
    tableName = "shelters",
    indices = {
        @Index("divisionId"),
        @Index("districtId"),
        @Index("upazilaId"),
        @Index("isActive")
    }
)
public class ShelterEntity {

    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String nameBn;
    private String description;
    private String descriptionBn;
    private String divisionId;
    private String districtId;
    private String upazilaId;
    private String address;
    private String addressBn;
    private double latitude;
    private double longitude;
    private int capacity;
    private int currentOccupancy;
    private String shelterType;

    @TypeConverters(StringListConverter.class)
    private List<String> facilities;

    private String contactNumber;
    private boolean isActive;
    private long createdAt;
    private long updatedAt;

    public ShelterEntity(@NonNull String id, String name, String nameBn, String description,
                         String descriptionBn, String divisionId, String districtId,
                         String upazilaId, String address, String addressBn, double latitude,
                         double longitude, int capacity, int currentOccupancy, String shelterType,
                         List<String> facilities, String contactNumber, boolean isActive,
                         long createdAt, long updatedAt) {
        this.id = id;
        this.name = name;
        this.nameBn = nameBn;
        this.description = description;
        this.descriptionBn = descriptionBn;
        this.divisionId = divisionId;
        this.districtId = districtId;
        this.upazilaId = upazilaId;
        this.address = address;
        this.addressBn = addressBn;
        this.latitude = latitude;
        this.longitude = longitude;
        this.capacity = capacity;
        this.currentOccupancy = currentOccupancy;
        this.shelterType = shelterType;
        this.facilities = facilities != null ? facilities : new ArrayList<>();
        this.contactNumber = contactNumber;
        this.isActive = isActive;
        this.createdAt = createdAt;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddressBn() {
        return addressBn;
    }

    public void setAddressBn(String addressBn) {
        this.addressBn = addressBn;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getCurrentOccupancy() {
        return currentOccupancy;
    }

    public void setCurrentOccupancy(int currentOccupancy) {
        this.currentOccupancy = currentOccupancy;
    }

    public String getShelterType() {
        return shelterType;
    }

    public void setShelterType(String shelterType) {
        this.shelterType = shelterType;
    }

    public List<String> getFacilities() {
        return facilities;
    }

    public void setFacilities(List<String> facilities) {
        this.facilities = facilities;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}

