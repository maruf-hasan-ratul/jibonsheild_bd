package com.jibonshield.bd.sync;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Encapsulates the verified outcome of an online synchronization attempt.
 * Enforces transparent reporting across success, offline, validation, rate-limiting,
 * and server error states without losing local data.
 */
public class SyncResult implements Serializable {

    public enum Status {
        SUCCESS,
        NO_CONNECTIVITY,
        SERVER_ERROR,
        VALIDATION_ERROR,
        RATE_LIMITED,
        AUTH_ERROR,
        NO_UPDATES,
        IO_ERROR
    }

    private final Status status;
    private final String message;
    private final String entityName;
    private final int totalRecords;
    private final int insertedRecords;
    private final int updatedRecords;
    private final long syncTimestamp;
    private final String errorMessage;

    public SyncResult(Status status, String message, String entityName,
                      int totalRecords, int insertedRecords, int updatedRecords,
                      long syncTimestamp, String errorMessage) {
        this.status = status;
        this.message = message != null ? message : "";
        this.entityName = entityName != null ? entityName : "All";
        this.totalRecords = totalRecords;
        this.insertedRecords = insertedRecords;
        this.updatedRecords = updatedRecords;
        this.syncTimestamp = syncTimestamp > 0 ? syncTimestamp : System.currentTimeMillis();
        this.errorMessage = errorMessage != null ? errorMessage : "";
    }

    public static SyncResult success(String entityName, int total, int inserted, int updated, String message) {
        return new SyncResult(Status.SUCCESS, message, entityName, total, inserted, updated, System.currentTimeMillis(), null);
    }

    public static SyncResult noConnectivity(String entityName) {
        return new SyncResult(Status.NO_CONNECTIVITY,
                "Device is offline. Local database remains active and verified.",
                entityName, 0, 0, 0, System.currentTimeMillis(), "No active internet connection");
    }

    public static SyncResult validationError(String entityName, String detail) {
        return new SyncResult(Status.VALIDATION_ERROR,
                "Remote data validation failed. Local records preserved.",
                entityName, 0, 0, 0, System.currentTimeMillis(), detail);
    }

    public static SyncResult serverError(String entityName, int httpCode, String detail) {
        return new SyncResult(Status.SERVER_ERROR,
                "Remote server returned HTTP " + httpCode + ". Local records preserved.",
                entityName, 0, 0, 0, System.currentTimeMillis(), detail);
    }

    public static SyncResult rateLimited(String entityName) {
        return new SyncResult(Status.RATE_LIMITED,
                "Provider rate limit reached. Using previously verified local data.",
                entityName, 0, 0, 0, System.currentTimeMillis(), "HTTP 429 Too Many Requests");
    }

    public static SyncResult ioError(String entityName, Exception e) {
        return new SyncResult(Status.IO_ERROR,
                "Network communication error. Offline cache preserved.",
                entityName, 0, 0, 0, System.currentTimeMillis(),
                e != null ? e.getMessage() : "Network error");
    }

    public Status getStatus() { return status; }
    public String getMessage() { return message; }
    public String getEntityName() { return entityName; }
    public int getTotalRecords() { return totalRecords; }
    public int getInsertedRecords() { return insertedRecords; }
    public int getUpdatedRecords() { return updatedRecords; }
    public long getSyncTimestamp() { return syncTimestamp; }
    public String getErrorMessage() { return errorMessage; }

    public boolean isSuccess() {
        return status == Status.SUCCESS || status == Status.NO_UPDATES;
    }

    public boolean isOffline() {
        return status == Status.NO_CONNECTIVITY;
    }

    public String getFormattedTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        return sdf.format(new Date(syncTimestamp)) + " BST";
    }

    @Override
    public String toString() {
        return "SyncResult{" +
                "status=" + status +
                ", entityName='" + entityName + '\'' +
                ", inserted=" + insertedRecords +
                ", updated=" + updatedRecords +
                ", message='" + message + '\'' +
                '}';
    }
}
