package com.jibonshield.bd.models;

import java.io.Serializable;

/**
 * Model representing synchronization metadata for offline entities.
 */
public class SyncMetadata implements Serializable {
    private final String tableName;
    private final String sourceName;
    private final long lastSyncTimestamp;
    private final int recordsCount;
    private final String syncStatus;
    private final String syncHash;
    private final String versionTag;

    public SyncMetadata(String tableName, String sourceName, long lastSyncTimestamp,
                        int recordsCount, String syncStatus, String syncHash, String versionTag) {
        this.tableName = tableName;
        this.sourceName = sourceName != null ? sourceName : "Official Baseline";
        this.lastSyncTimestamp = lastSyncTimestamp;
        this.recordsCount = recordsCount;
        this.syncStatus = syncStatus != null ? syncStatus : "OFFLINE_CACHE";
        this.syncHash = syncHash != null ? syncHash : "";
        this.versionTag = versionTag != null ? versionTag : "1.0.0";
    }

    public String getTableName() { return tableName; }
    public String getSourceName() { return sourceName; }
    public long getLastSyncTimestamp() { return lastSyncTimestamp; }
    public int getRecordsCount() { return recordsCount; }
    public String getSyncStatus() { return syncStatus; }
    public String getSyncHash() { return syncHash; }
    public String getVersionTag() { return versionTag; }
}
