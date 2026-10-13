package com.jibonshield.bd.models;

import java.io.Serializable;

/**
 * Model representing an emergency contact helpline.
 */
public class EmergencyContact implements Serializable {
    private final long id;
    private final String serviceNameEn;
    private final String serviceNameBn;
    private final String phoneNumber;
    private final String category;
    private final String availableHours;
    private final boolean isTollFree;
    private final int priorityOrder;

    public EmergencyContact(long id, String serviceNameEn, String serviceNameBn, String phoneNumber,
                            String category, String availableHours, boolean isTollFree, int priorityOrder) {
        this.id = id;
        this.serviceNameEn = serviceNameEn;
        this.serviceNameBn = serviceNameBn;
        this.phoneNumber = phoneNumber;
        this.category = category;
        this.availableHours = availableHours;
        this.isTollFree = isTollFree;
        this.priorityOrder = priorityOrder;
    }

    public long getId() { return id; }
    public String getServiceNameEn() { return serviceNameEn; }
    public String getServiceNameBn() { return serviceNameBn; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getCategory() { return category; }
    public String getAvailableHours() { return availableHours; }
    public boolean isTollFree() { return isTollFree; }
    public int getPriorityOrder() { return priorityOrder; }
}
