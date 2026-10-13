package com.jibonshield.bd.models;

import java.io.Serializable;

/**
 * Model representing an emergency preparedness checklist item.
 */
public class ChecklistItem implements Serializable {
    private final long id;
    private final long checklistId;
    private final String itemEn;
    private final String itemBn;
    private boolean isCompleted;
    private final boolean isEssential;

    public ChecklistItem(long id, long checklistId, String itemEn, String itemBn,
                         boolean isCompleted, boolean isEssential) {
        this.id = id;
        this.checklistId = checklistId;
        this.itemEn = itemEn;
        this.itemBn = itemBn;
        this.isCompleted = isCompleted;
        this.isEssential = isEssential;
    }

    public long getId() { return id; }
    public long getChecklistId() { return checklistId; }
    public String getItemEn() { return itemEn; }
    public String getItemBn() { return itemBn; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    public boolean isEssential() { return isEssential; }
}
