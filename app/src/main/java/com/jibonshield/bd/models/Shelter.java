package com.jibonshield.bd.models;

import java.io.Serializable;

/**
 * Model representing a designated cyclone or flood shelter in Bangladesh.
 */
public class Shelter implements Serializable {
    private final long id;
    private final String nameEn;
    private final String nameBn;
    private final String district;
    private final String upazila;
    private final double latitude;
    private final double longitude;
    private final int capacity;
    private final int currentOccupancy;
    private final String contactPhone;
    private final boolean isActive;
    private final String verifiedAt;

    public Shelter(long id, String nameEn, String nameBn, String district, String upazila,
                   double latitude, double longitude, int capacity, int currentOccupancy,
                   String contactPhone, boolean isActive, String verifiedAt) {
        this.id = id;
        this.nameEn = nameEn;
        this.nameBn = nameBn;
        this.district = district;
        this.upazila = upazila;
        this.latitude = latitude;
        this.longitude = longitude;
        this.capacity = capacity;
        this.currentOccupancy = currentOccupancy;
        this.contactPhone = contactPhone;
        this.isActive = isActive;
        this.verifiedAt = verifiedAt;
    }

    public long getId() { return id; }
    public String getNameEn() { return nameEn; }
    public String getNameBn() { return nameBn; }
    public String getDistrict() { return district; }
    public String getUpazila() { return upazila; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getCapacity() { return capacity; }
    public int getCurrentOccupancy() { return currentOccupancy; }
    public String getContactPhone() { return contactPhone; }
    public boolean isActive() { return isActive; }
    public String getVerifiedAt() { return verifiedAt; }
}
