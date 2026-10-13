package com.jibonshield.bd.models;

import java.io.Serializable;

/**
 * Model representing a designated cyclone or flood shelter in Bangladesh.
 * Contains location, capacity, provenance, accessibility, and contact details.
 */
public class Shelter implements Serializable {
    private final long id;
    private final String nameEn;
    private final String nameBn;
    private final String district;
    private final String upazila;
    private final String address;
    private final double latitude;
    private final double longitude;
    private final int capacity;
    private final int currentOccupancy;
    private final String contactPhone;
    private final String shelterType;
    private final String accessibilityNotes;
    private final String source;
    private final boolean isActive;
    private final String verifiedAt;
    private final long createdAt;
    private final long updatedAt;

    /**
     * Full constructor with all fields.
     */
    public Shelter(long id, String nameEn, String nameBn, String district, String upazila,
                   String address, double latitude, double longitude, int capacity, int currentOccupancy,
                   String contactPhone, String shelterType, String accessibilityNotes, String source,
                   boolean isActive, String verifiedAt, long createdAt, long updatedAt) {
        this.id = id;
        this.nameEn = nameEn;
        this.nameBn = nameBn;
        this.district = district;
        this.upazila = upazila;
        this.address = address != null ? address : (upazila + ", " + district);
        this.latitude = latitude;
        this.longitude = longitude;
        this.capacity = capacity;
        this.currentOccupancy = currentOccupancy;
        this.contactPhone = contactPhone;
        this.shelterType = shelterType != null ? shelterType : "Disaster Shelter";
        this.accessibilityNotes = accessibilityNotes != null ? accessibilityNotes : "Standard ground refuge";
        this.source = source != null ? source : "MoDMR / CPP";
        this.isActive = isActive;
        this.verifiedAt = verifiedAt;
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
        this.updatedAt = updatedAt > 0 ? updatedAt : System.currentTimeMillis();
    }

    /**
     * Backward-compatible constructor.
     */
    public Shelter(long id, String nameEn, String nameBn, String district, String upazila,
                   double latitude, double longitude, int capacity, int currentOccupancy,
                   String contactPhone, boolean isActive, String verifiedAt) {
        this(id, nameEn, nameBn, district, upazila, upazila + ", " + district,
                latitude, longitude, capacity, currentOccupancy, contactPhone,
                "Cyclone & Flood Shelter", "Elevated high-ground access",
                "MoDMR / CPP", isActive, verifiedAt, System.currentTimeMillis(), System.currentTimeMillis());
    }

    public long getId() { return id; }
    public String getNameEn() { return nameEn; }
    public String getNameBn() { return nameBn; }
    public String getDistrict() { return district; }
    public String getUpazila() { return upazila; }
    public String getAddress() { return address; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getCapacity() { return capacity; }
    public int getCurrentOccupancy() { return currentOccupancy; }
    public String getContactPhone() { return contactPhone; }
    public String getShelterType() { return shelterType; }
    public String getAccessibilityNotes() { return accessibilityNotes; }
    public String getSource() { return source; }
    public boolean isActive() { return isActive; }
    public String getVerifiedAt() { return verifiedAt; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}
