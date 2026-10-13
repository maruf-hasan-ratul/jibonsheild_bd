package com.jibonshield.bd.models;

import java.io.Serializable;

/**
 * Model representing an emergency contact helpline with verification provenance.
 */
public class EmergencyContact implements Serializable {
    private final long id;
    private final String serviceNameEn;
    private final String serviceNameBn;
    private final String phoneNumber;
    private final String serviceDescriptionEn;
    private final String serviceDescriptionBn;
    private final String category;
    private final String availableHours;
    private final boolean isTollFree;
    private final String source;
    private final String verificationDate;
    private final boolean isEnabled;
    private final int priorityOrder;

    /**
     * Full constructor.
     */
    public EmergencyContact(long id, String serviceNameEn, String serviceNameBn, String phoneNumber,
                            String serviceDescriptionEn, String serviceDescriptionBn,
                            String category, String availableHours, boolean isTollFree,
                            String source, String verificationDate, boolean isEnabled, int priorityOrder) {
        this.id = id;
        this.serviceNameEn = serviceNameEn;
        this.serviceNameBn = serviceNameBn;
        this.phoneNumber = phoneNumber;
        this.serviceDescriptionEn = serviceDescriptionEn != null ? serviceDescriptionEn : "";
        this.serviceDescriptionBn = serviceDescriptionBn != null ? serviceDescriptionBn : "";
        this.category = category;
        this.availableHours = availableHours;
        this.isTollFree = isTollFree;
        this.source = source != null ? source : "Government of Bangladesh";
        this.verificationDate = verificationDate != null ? verificationDate : "2026-01-01";
        this.isEnabled = isEnabled;
        this.priorityOrder = priorityOrder;
    }

    /**
     * Backward-compatible constructor.
     */
    public EmergencyContact(long id, String serviceNameEn, String serviceNameBn, String phoneNumber,
                            String category, String availableHours, boolean isTollFree, int priorityOrder) {
        this(id, serviceNameEn, serviceNameBn, phoneNumber,
                serviceNameEn, serviceNameBn,
                category, availableHours, isTollFree,
                "Government of Bangladesh", "2026-01-01", true, priorityOrder);
    }

    public long getId() { return id; }
    public String getServiceNameEn() { return serviceNameEn; }
    public String getServiceNameBn() { return serviceNameBn; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getServiceDescriptionEn() { return serviceDescriptionEn; }
    public String getServiceDescriptionBn() { return serviceDescriptionBn; }
    public String getCategory() { return category; }
    public String getAvailableHours() { return availableHours; }
    public boolean isTollFree() { return isTollFree; }
    public String getSource() { return source; }
    public String getVerificationDate() { return verificationDate; }
    public boolean isEnabled() { return isEnabled; }
    public int getPriorityOrder() { return priorityOrder; }
}
