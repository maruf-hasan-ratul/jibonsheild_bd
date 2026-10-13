package com.jibonshield.bd.models;

import java.io.Serializable;

/**
 * Model representing an emergency preparedness checklist category.
 */
public class PreparednessChecklist implements Serializable {
    private final long id;
    private final String categoryKey;
    private final String titleEn;
    private final String titleBn;
    private final String descriptionEn;
    private final String descriptionBn;
    private final String iconName;
    private final int displayOrder;

    public PreparednessChecklist(long id, String categoryKey, String titleEn, String titleBn,
                                 String descriptionEn, String descriptionBn,
                                 String iconName, int displayOrder) {
        this.id = id;
        this.categoryKey = categoryKey;
        this.titleEn = titleEn;
        this.titleBn = titleBn;
        this.descriptionEn = descriptionEn != null ? descriptionEn : "";
        this.descriptionBn = descriptionBn != null ? descriptionBn : "";
        this.iconName = iconName;
        this.displayOrder = displayOrder;
    }

    public long getId() { return id; }
    public String getCategoryKey() { return categoryKey; }
    public String getTitleEn() { return titleEn; }
    public String getTitleBn() { return titleBn; }
    public String getDescriptionEn() { return descriptionEn; }
    public String getDescriptionBn() { return descriptionBn; }
    public String getIconName() { return iconName; }
    public int getDisplayOrder() { return displayOrder; }
}
