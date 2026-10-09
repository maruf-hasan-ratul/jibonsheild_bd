package com.jibonshield.bd.models;

/**
 * Model class representing an emergency preparedness module card on the Dashboard.
 */
public class DashboardCardItem {
    private final int id;
    private final int titleResId;
    private final int descriptionResId;
    private final int iconResId;
    private final int badgeResId;
    private final boolean isImplemented;

    public DashboardCardItem(int id, int titleResId, int descriptionResId, int iconResId, int badgeResId, boolean isImplemented) {
        this.id = id;
        this.titleResId = titleResId;
        this.descriptionResId = descriptionResId;
        this.iconResId = iconResId;
        this.badgeResId = badgeResId;
        this.isImplemented = isImplemented;
    }

    public int getId() {
        return id;
    }

    public int getTitleResId() {
        return titleResId;
    }

    public int getDescriptionResId() {
        return descriptionResId;
    }

    public int getIconResId() {
        return iconResId;
    }

    public int getBadgeResId() {
        return badgeResId;
    }

    public boolean isImplemented() {
        return isImplemented;
    }
}
