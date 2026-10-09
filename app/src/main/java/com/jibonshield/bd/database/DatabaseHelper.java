package com.jibonshield.bd.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

/**
 * SQLiteOpenHelper implementation for JibonShield BD.
 * Manages offline database creation, initial emergency hotline seeding, and future migrations.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public DatabaseHelper(Context context) {
        super(context, DatabaseContract.DATABASE_NAME, null, DatabaseContract.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Table 1: Emergency Contacts
        final String SQL_CREATE_EMERGENCY_CONTACTS_TABLE = "CREATE TABLE " +
                DatabaseContract.EmergencyContactEntry.TABLE_NAME + " (" +
                DatabaseContract.EmergencyContactEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_EN + " TEXT NOT NULL, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_BN + " TEXT NOT NULL, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_PHONE_NUMBER + " TEXT NOT NULL, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_CATEGORY + " TEXT NOT NULL, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_AVAILABLE_HOURS + " TEXT NOT NULL, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_IS_TOLL_FREE + " INTEGER NOT NULL DEFAULT 0, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_PRIORITY_ORDER + " INTEGER NOT NULL DEFAULT 0" +
                ");";

        // Table 2: Synchronization Metadata
        final String SQL_CREATE_SYNC_METADATA_TABLE = "CREATE TABLE " +
                DatabaseContract.SyncMetadataEntry.TABLE_NAME + " (" +
                DatabaseContract.SyncMetadataEntry.COLUMN_TABLE_NAME + " TEXT PRIMARY KEY, " +
                DatabaseContract.SyncMetadataEntry.COLUMN_LAST_SYNC_TIMESTAMP + " INTEGER NOT NULL, " +
                DatabaseContract.SyncMetadataEntry.COLUMN_SYNC_HASH + " TEXT, " +
                DatabaseContract.SyncMetadataEntry.COLUMN_VERSION_TAG + " TEXT NOT NULL" +
                ");";

        db.execSQL(SQL_CREATE_EMERGENCY_CONTACTS_TABLE);
        db.execSQL(SQL_CREATE_SYNC_METADATA_TABLE);

        // Seed initial verified national emergency hotlines
        seedInitialEmergencyContacts(db);
        seedSyncMetadata(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Non-destructive upgrade path: preserve user records and checklist states across versions
        // In future phases, incremental ALTER TABLE statements and table migrations will be handled here.
    }

    private void seedInitialEmergencyContacts(SQLiteDatabase db) {
        insertEmergencyContact(db, "National Emergency Service (Police, Fire, Ambulance)", "জাতীয় জরুরি সেবা (পুলিশ, ফায়ার, অ্যাম্বুলেন্স)", "999", "NATIONAL", "24/7", 1, 1);
        insertEmergencyContact(db, "Disaster Early Warning Center", "দুর্যোগের আগাম বার্তা কেন্দ্র", "1090", "DISASTER", "24/7", 1, 2);
        insertEmergencyContact(db, "Health Directorate Hotline (Shastho Batayan)", "স্বাস্থ্য বাতায়ন (স্বাস্থ্য অধিদপ্তর)", "16263", "MEDICAL", "24/7", 0, 3);
        insertEmergencyContact(db, "Bangladesh Red Crescent Society (BDRCS)", "বাংলাদেশ রেড ক্রিসেন্ট সোসাইটি", "02-9357474", "HUMANITARIAN", "24/7", 0, 4);
        insertEmergencyContact(db, "Flood Forecasting and Warning Centre (FFWC)", "বন্যা পূর্বাভাস ও সতর্কীকরণ কেন্দ্র", "02-9556628", "FLOOD", "Office Hours & Flood Season", 0, 5);
    }

    private void insertEmergencyContact(SQLiteDatabase db, String nameEn, String nameBn, String phone, String category, String hours, int isTollFree, int priority) {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_EN, nameEn);
        values.put(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_BN, nameBn);
        values.put(DatabaseContract.EmergencyContactEntry.COLUMN_PHONE_NUMBER, phone);
        values.put(DatabaseContract.EmergencyContactEntry.COLUMN_CATEGORY, category);
        values.put(DatabaseContract.EmergencyContactEntry.COLUMN_AVAILABLE_HOURS, hours);
        values.put(DatabaseContract.EmergencyContactEntry.COLUMN_IS_TOLL_FREE, isTollFree);
        values.put(DatabaseContract.EmergencyContactEntry.COLUMN_PRIORITY_ORDER, priority);
        db.insert(DatabaseContract.EmergencyContactEntry.TABLE_NAME, null, values);
    }

    private void seedSyncMetadata(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.SyncMetadataEntry.COLUMN_TABLE_NAME, DatabaseContract.EmergencyContactEntry.TABLE_NAME);
        values.put(DatabaseContract.SyncMetadataEntry.COLUMN_LAST_SYNC_TIMESTAMP, System.currentTimeMillis());
        values.put(DatabaseContract.SyncMetadataEntry.COLUMN_SYNC_HASH, "initial-seed-day1");
        values.put(DatabaseContract.SyncMetadataEntry.COLUMN_VERSION_TAG, "2026.1-OFFLINE");
        db.insert(DatabaseContract.SyncMetadataEntry.TABLE_NAME, null, values);
    }
}
