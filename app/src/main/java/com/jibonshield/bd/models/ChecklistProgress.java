package com.jibonshield.bd.models;

import java.io.Serializable;

/**
 * Encapsulates checklist progress calculation.
 */
public class ChecklistProgress implements Serializable {
    private final int totalItems;
    private final int completedItems;
    private final int essentialTotal;
    private final int essentialCompleted;

    public ChecklistProgress(int totalItems, int completedItems, int essentialTotal, int essentialCompleted) {
        this.totalItems = totalItems;
        this.completedItems = completedItems;
        this.essentialTotal = essentialTotal;
        this.essentialCompleted = essentialCompleted;
    }

    public int getTotalItems() { return totalItems; }
    public int getCompletedItems() { return completedItems; }
    public int getEssentialTotal() { return essentialTotal; }
    public int getEssentialCompleted() { return essentialCompleted; }

    public int getOverallPercentage() {
        return totalItems > 0 ? (completedItems * 100) / totalItems : 0;
    }

    public int getEssentialPercentage() {
        return essentialTotal > 0 ? (essentialCompleted * 100) / essentialTotal : 0;
    }
}
