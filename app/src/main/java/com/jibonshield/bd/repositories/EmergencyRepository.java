package com.jibonshield.bd.repositories;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;

import com.jibonshield.bd.database.DatabaseContract;
import com.jibonshield.bd.database.DatabaseHelper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository layer abstracting database operations and executing them on a background worker thread.
 */
public class EmergencyRepository {

    public interface DatabaseInitCallback {
        void onDatabaseReady(int contactCount);
        void onError(Exception error);
    }

    private final DatabaseHelper dbHelper;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public EmergencyRepository(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Ensures the database is initialized and seed data is verified asynchronously.
     */
    public void ensureDatabaseInitialized(DatabaseInitCallback callback) {
        executorService.execute(() -> {
            try {
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseContract.EmergencyContactEntry.TABLE_NAME, null);
                int count = 0;
                if (cursor != null && cursor.moveToFirst()) {
                    count = cursor.getInt(0);
                    cursor.close();
                }
                final int totalCount = count;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onDatabaseReady(totalCount);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
            }
        });
    }
}
