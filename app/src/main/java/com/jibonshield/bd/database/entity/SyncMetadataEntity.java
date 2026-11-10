package com.jibonshield.bd.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sync_metadata")
public class SyncMetadataEntity {

    @PrimaryKey
    @NonNull
    private String tableName;
    private long lastSyncTimestamp;
    private int recordsCount;
    private String syncStatus;

    public SyncMetadataEntity(@NonNull String tableName, long lastSyncTimestamp, int recordsCount, String syncStatus) {
        this.tableName = tableName;
        this.lastSyncTimestamp = lastSyncTimestamp;
        this.recordsCount = recordsCount;
        this.syncStatus = syncStatus;
    }

    @NonNull
    public String getTableName() {
        return tableName;
    }

    public void setTableName(@NonNull String tableName) {
        this.tableName = tableName;
    }

    public long getLastSyncTimestamp() {
        return lastSyncTimestamp;
    }

    public void setLastSyncTimestamp(long lastSyncTimestamp) {
        this.lastSyncTimestamp = lastSyncTimestamp;
    }

    public int getRecordsCount() {
        return recordsCount;
    }

    public void setRecordsCount(int recordsCount) {
        this.recordsCount = recordsCount;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }
}

