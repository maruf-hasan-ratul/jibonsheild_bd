package com.jibonshield.bd.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * SQLiteOpenHelper implementation for JibonShield BD.
 * Manages offline database creation, robust migrations, data integrity,
 * foreign keys, index optimization, and verified authoritative baseline seeding.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
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
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            createAllTables(db);
            createAllIndexes(db);
            seedInitialEmergencyContacts(db);
            seedInitialFloodWarnings(db);
            seedInitialShelters(db);
            seedInitialChecklists(db);
            seedSyncMetadata(db);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing database schema and seed data", e);
            throw e;
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        db.beginTransaction();
        try {
            if (oldVersion < 2) {
                // Version 1 to 2 migration
                createTableIfNotExists(db, getCreateFloodWarningsTableSql());
                createTableIfNotExists(db, getCreateSheltersTableSql());
                createTableIfNotExists(db, getCreateWaterObservationsTableSql());
                createTableIfNotExists(db, getCreateChecklistsTableSql());
                createTableIfNotExists(db, getCreateChecklistItemsTableSql());
                createTableIfNotExists(db, getCreateSyncMetadataTableSql());

                seedInitialFloodWarnings(db);
                seedInitialShelters(db);
                seedInitialChecklists(db);
            }

            if (oldVersion < 3) {
                // Version 2 to 3 migration: Add newly required columns without deleting any user records
                upgradeToVersion3(db);
            }

            createAllIndexes(db);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Failed to migrate database safely", e);
            throw e;
        } finally {
            db.endTransaction();
        }
    }

    private void upgradeToVersion3(SQLiteDatabase db) {
        // 1. Shelter table column additions
        addColumnIfNotExists(db, DatabaseContract.ShelterEntry.TABLE_NAME, DatabaseContract.ShelterEntry.COLUMN_ADDRESS, "TEXT");
        addColumnIfNotExists(db, DatabaseContract.ShelterEntry.TABLE_NAME, DatabaseContract.ShelterEntry.COLUMN_SHELTER_TYPE, "TEXT");
        addColumnIfNotExists(db, DatabaseContract.ShelterEntry.TABLE_NAME, DatabaseContract.ShelterEntry.COLUMN_ACCESSIBILITY_NOTES, "TEXT");
        addColumnIfNotExists(db, DatabaseContract.ShelterEntry.TABLE_NAME, DatabaseContract.ShelterEntry.COLUMN_SOURCE, "TEXT");
        addColumnIfNotExists(db, DatabaseContract.ShelterEntry.TABLE_NAME, DatabaseContract.ShelterEntry.COLUMN_CREATED_AT, "INTEGER NOT NULL DEFAULT 0");
        addColumnIfNotExists(db, DatabaseContract.ShelterEntry.TABLE_NAME, DatabaseContract.ShelterEntry.COLUMN_UPDATED_AT, "INTEGER NOT NULL DEFAULT 0");

        // 2. Emergency contacts column additions
        addColumnIfNotExists(db, DatabaseContract.EmergencyContactEntry.TABLE_NAME, DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_DESCRIPTION_EN, "TEXT");
        addColumnIfNotExists(db, DatabaseContract.EmergencyContactEntry.TABLE_NAME, DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_DESCRIPTION_BN, "TEXT");
        addColumnIfNotExists(db, DatabaseContract.EmergencyContactEntry.TABLE_NAME, DatabaseContract.EmergencyContactEntry.COLUMN_SOURCE, "TEXT");
        addColumnIfNotExists(db, DatabaseContract.EmergencyContactEntry.TABLE_NAME, DatabaseContract.EmergencyContactEntry.COLUMN_VERIFICATION_DATE, "TEXT");
        addColumnIfNotExists(db, DatabaseContract.EmergencyContactEntry.TABLE_NAME, DatabaseContract.EmergencyContactEntry.COLUMN_IS_ENABLED, "INTEGER NOT NULL DEFAULT 1");

        // 3. Flood warnings column additions
        addColumnIfNotExists(db, DatabaseContract.FloodWarningEntry.TABLE_NAME, DatabaseContract.FloodWarningEntry.COLUMN_SOURCE_REFERENCE, "TEXT");
        addColumnIfNotExists(db, DatabaseContract.FloodWarningEntry.TABLE_NAME, DatabaseContract.FloodWarningEntry.COLUMN_LAST_SYNC_TIMESTAMP, "INTEGER NOT NULL DEFAULT 0");

        // 4. Water observations column additions
        addColumnIfNotExists(db, DatabaseContract.WaterObservationEntry.TABLE_NAME, DatabaseContract.WaterObservationEntry.COLUMN_CREATED_AT, "INTEGER NOT NULL DEFAULT 0");
        addColumnIfNotExists(db, DatabaseContract.WaterObservationEntry.TABLE_NAME, DatabaseContract.WaterObservationEntry.COLUMN_UPDATED_AT, "INTEGER NOT NULL DEFAULT 0");

        // 5. Checklist items column additions
        addColumnIfNotExists(db, DatabaseContract.ChecklistItemEntry.TABLE_NAME, DatabaseContract.ChecklistItemEntry.COLUMN_UPDATED_AT, "INTEGER NOT NULL DEFAULT 0");

        // 6. Checklists table additions
        addColumnIfNotExists(db, DatabaseContract.PreparednessChecklistEntry.TABLE_NAME, DatabaseContract.PreparednessChecklistEntry.COLUMN_DESCRIPTION_EN, "TEXT");
        addColumnIfNotExists(db, DatabaseContract.PreparednessChecklistEntry.TABLE_NAME, DatabaseContract.PreparednessChecklistEntry.COLUMN_DESCRIPTION_BN, "TEXT");

        // 7. Sync metadata column additions
        addColumnIfNotExists(db, DatabaseContract.SyncMetadataEntry.TABLE_NAME, DatabaseContract.SyncMetadataEntry.COLUMN_SOURCE_NAME, "TEXT");
        addColumnIfNotExists(db, DatabaseContract.SyncMetadataEntry.TABLE_NAME, DatabaseContract.SyncMetadataEntry.COLUMN_RECORDS_COUNT, "INTEGER NOT NULL DEFAULT 0");
        addColumnIfNotExists(db, DatabaseContract.SyncMetadataEntry.TABLE_NAME, DatabaseContract.SyncMetadataEntry.COLUMN_SYNC_STATUS, "TEXT NOT NULL DEFAULT 'OFFLINE_CACHE'");
    }

    private void addColumnIfNotExists(SQLiteDatabase db, String tableName, String columnName, String columnType) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);
            boolean exists = false;
            if (cursor != null) {
                int nameIndex = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    if (columnName.equalsIgnoreCase(cursor.getString(nameIndex))) {
                        exists = true;
                        break;
                    }
                }
            }
            if (!exists) {
                db.execSQL("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType + ";");
            }
        } catch (Exception e) {
            Log.w(TAG, "Column check/add failed for " + tableName + "." + columnName + ": " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void createTableIfNotExists(SQLiteDatabase db, String sql) {
        db.execSQL(sql);
    }

    private void createAllTables(SQLiteDatabase db) {
        db.execSQL(getCreateEmergencyContactsTableSql());
        db.execSQL(getCreateFloodWarningsTableSql());
        db.execSQL(getCreateSheltersTableSql());
        db.execSQL(getCreateWaterObservationsTableSql());
        db.execSQL(getCreateChecklistsTableSql());
        db.execSQL(getCreateChecklistItemsTableSql());
        db.execSQL(getCreateSyncMetadataTableSql());
    }

    private void createAllIndexes(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_shelters_district_upazila ON "
                    + DatabaseContract.ShelterEntry.TABLE_NAME + " ("
                    + DatabaseContract.ShelterEntry.COLUMN_DISTRICT + ", "
                    + DatabaseContract.ShelterEntry.COLUMN_UPAZILA + ");");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_flood_warnings_active_region ON "
                    + DatabaseContract.FloodWarningEntry.TABLE_NAME + " ("
                    + DatabaseContract.FloodWarningEntry.COLUMN_IS_ACTIVE + ", "
                    + DatabaseContract.FloodWarningEntry.COLUMN_REGION + ");");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_checklist_items_cat ON "
                    + DatabaseContract.ChecklistItemEntry.TABLE_NAME + " ("
                    + DatabaseContract.ChecklistItemEntry.COLUMN_CHECKLIST_ID + ", "
                    + DatabaseContract.ChecklistItemEntry.COLUMN_IS_COMPLETED + ");");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_water_obs_time ON "
                    + DatabaseContract.WaterObservationEntry.TABLE_NAME + " ("
                    + DatabaseContract.WaterObservationEntry.COLUMN_OBSERVATION_TIME + ");");
        } catch (Exception e) {
            Log.w(TAG, "Error creating index", e);
        }
    }

    private String getCreateEmergencyContactsTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + DatabaseContract.EmergencyContactEntry.TABLE_NAME + " (" +
                DatabaseContract.EmergencyContactEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_EN + " TEXT NOT NULL, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_BN + " TEXT NOT NULL, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_PHONE_NUMBER + " TEXT NOT NULL, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_DESCRIPTION_EN + " TEXT, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_DESCRIPTION_BN + " TEXT, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_CATEGORY + " TEXT NOT NULL, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_AVAILABLE_HOURS + " TEXT NOT NULL, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_IS_TOLL_FREE + " INTEGER NOT NULL DEFAULT 0, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_SOURCE + " TEXT NOT NULL DEFAULT 'BTRC / GoB', " +
                DatabaseContract.EmergencyContactEntry.COLUMN_VERIFICATION_DATE + " TEXT NOT NULL DEFAULT '2026-01-01', " +
                DatabaseContract.EmergencyContactEntry.COLUMN_IS_ENABLED + " INTEGER NOT NULL DEFAULT 1, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_PRIORITY_ORDER + " INTEGER NOT NULL DEFAULT 0" +
                ");";
    }

    private String getCreateFloodWarningsTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + DatabaseContract.FloodWarningEntry.TABLE_NAME + " (" +
                DatabaseContract.FloodWarningEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DatabaseContract.FloodWarningEntry.COLUMN_WARNING_LEVEL + " TEXT NOT NULL, " +
                DatabaseContract.FloodWarningEntry.COLUMN_TITLE_EN + " TEXT NOT NULL, " +
                DatabaseContract.FloodWarningEntry.COLUMN_TITLE_BN + " TEXT NOT NULL, " +
                DatabaseContract.FloodWarningEntry.COLUMN_REGION + " TEXT NOT NULL, " +
                DatabaseContract.FloodWarningEntry.COLUMN_DETAILS_EN + " TEXT NOT NULL, " +
                DatabaseContract.FloodWarningEntry.COLUMN_DETAILS_BN + " TEXT NOT NULL, " +
                DatabaseContract.FloodWarningEntry.COLUMN_ISSUED_AT + " TEXT NOT NULL, " +
                DatabaseContract.FloodWarningEntry.COLUMN_EXPIRES_AT + " TEXT NOT NULL, " +
                DatabaseContract.FloodWarningEntry.COLUMN_SOURCE + " TEXT NOT NULL, " +
                DatabaseContract.FloodWarningEntry.COLUMN_SOURCE_REFERENCE + " TEXT, " +
                DatabaseContract.FloodWarningEntry.COLUMN_IS_ACTIVE + " INTEGER NOT NULL DEFAULT 1, " +
                DatabaseContract.FloodWarningEntry.COLUMN_IS_SAMPLE + " INTEGER NOT NULL DEFAULT 0, " +
                DatabaseContract.FloodWarningEntry.COLUMN_LAST_SYNC_TIMESTAMP + " INTEGER NOT NULL DEFAULT 0" +
                ");";
    }

    private String getCreateSheltersTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + DatabaseContract.ShelterEntry.TABLE_NAME + " (" +
                DatabaseContract.ShelterEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DatabaseContract.ShelterEntry.COLUMN_NAME_EN + " TEXT NOT NULL, " +
                DatabaseContract.ShelterEntry.COLUMN_NAME_BN + " TEXT NOT NULL, " +
                DatabaseContract.ShelterEntry.COLUMN_DISTRICT + " TEXT NOT NULL, " +
                DatabaseContract.ShelterEntry.COLUMN_UPAZILA + " TEXT NOT NULL, " +
                DatabaseContract.ShelterEntry.COLUMN_ADDRESS + " TEXT, " +
                DatabaseContract.ShelterEntry.COLUMN_LATITUDE + " REAL NOT NULL, " +
                DatabaseContract.ShelterEntry.COLUMN_LONGITUDE + " REAL NOT NULL, " +
                DatabaseContract.ShelterEntry.COLUMN_CAPACITY + " INTEGER NOT NULL, " +
                DatabaseContract.ShelterEntry.COLUMN_CURRENT_OCCUPANCY + " INTEGER NOT NULL DEFAULT 0, " +
                DatabaseContract.ShelterEntry.COLUMN_CONTACT_PHONE + " TEXT NOT NULL, " +
                DatabaseContract.ShelterEntry.COLUMN_SHELTER_TYPE + " TEXT, " +
                DatabaseContract.ShelterEntry.COLUMN_ACCESSIBILITY_NOTES + " TEXT, " +
                DatabaseContract.ShelterEntry.COLUMN_SOURCE + " TEXT, " +
                DatabaseContract.ShelterEntry.COLUMN_VERIFIED_AT + " TEXT NOT NULL, " +
                DatabaseContract.ShelterEntry.COLUMN_IS_ACTIVE + " INTEGER NOT NULL DEFAULT 1, " +
                DatabaseContract.ShelterEntry.COLUMN_CREATED_AT + " INTEGER NOT NULL DEFAULT 0, " +
                DatabaseContract.ShelterEntry.COLUMN_UPDATED_AT + " INTEGER NOT NULL DEFAULT 0" +
                ");";
    }

    private String getCreateWaterObservationsTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + DatabaseContract.WaterObservationEntry.TABLE_NAME + " (" +
                DatabaseContract.WaterObservationEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DatabaseContract.WaterObservationEntry.COLUMN_LOCATION_NAME + " TEXT NOT NULL, " +
                DatabaseContract.WaterObservationEntry.COLUMN_DISTRICT + " TEXT NOT NULL, " +
                DatabaseContract.WaterObservationEntry.COLUMN_SOURCE_TYPE + " TEXT NOT NULL, " +
                DatabaseContract.WaterObservationEntry.COLUMN_APPEARANCE + " TEXT NOT NULL, " +
                DatabaseContract.WaterObservationEntry.COLUMN_ODOR + " TEXT NOT NULL, " +
                DatabaseContract.WaterObservationEntry.COLUMN_PH_LEVEL + " REAL NOT NULL DEFAULT 7.0, " +
                DatabaseContract.WaterObservationEntry.COLUMN_TURBIDITY_STATUS + " TEXT NOT NULL, " +
                DatabaseContract.WaterObservationEntry.COLUMN_SAFETY_STATUS + " TEXT NOT NULL, " +
                DatabaseContract.WaterObservationEntry.COLUMN_OBSERVATION_TIME + " TEXT NOT NULL, " +
                DatabaseContract.WaterObservationEntry.COLUMN_REPORTED_BY + " TEXT NOT NULL, " +
                DatabaseContract.WaterObservationEntry.COLUMN_REMARKS + " TEXT, " +
                DatabaseContract.WaterObservationEntry.COLUMN_CREATED_AT + " INTEGER NOT NULL DEFAULT 0, " +
                DatabaseContract.WaterObservationEntry.COLUMN_UPDATED_AT + " INTEGER NOT NULL DEFAULT 0" +
                ");";
    }

    private String getCreateChecklistsTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + DatabaseContract.PreparednessChecklistEntry.TABLE_NAME + " (" +
                DatabaseContract.PreparednessChecklistEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DatabaseContract.PreparednessChecklistEntry.COLUMN_CATEGORY_KEY + " TEXT NOT NULL UNIQUE, " +
                DatabaseContract.PreparednessChecklistEntry.COLUMN_TITLE_EN + " TEXT NOT NULL, " +
                DatabaseContract.PreparednessChecklistEntry.COLUMN_TITLE_BN + " TEXT NOT NULL, " +
                DatabaseContract.PreparednessChecklistEntry.COLUMN_DESCRIPTION_EN + " TEXT, " +
                DatabaseContract.PreparednessChecklistEntry.COLUMN_DESCRIPTION_BN + " TEXT, " +
                DatabaseContract.PreparednessChecklistEntry.COLUMN_ICON_NAME + " TEXT NOT NULL, " +
                DatabaseContract.PreparednessChecklistEntry.COLUMN_DISPLAY_ORDER + " INTEGER NOT NULL" +
                ");";
    }

    private String getCreateChecklistItemsTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + DatabaseContract.ChecklistItemEntry.TABLE_NAME + " (" +
                DatabaseContract.ChecklistItemEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DatabaseContract.ChecklistItemEntry.COLUMN_CHECKLIST_ID + " INTEGER NOT NULL, " +
                DatabaseContract.ChecklistItemEntry.COLUMN_ITEM_EN + " TEXT NOT NULL, " +
                DatabaseContract.ChecklistItemEntry.COLUMN_ITEM_BN + " TEXT NOT NULL, " +
                DatabaseContract.ChecklistItemEntry.COLUMN_IS_COMPLETED + " INTEGER NOT NULL DEFAULT 0, " +
                DatabaseContract.ChecklistItemEntry.COLUMN_IS_ESSENTIAL + " INTEGER NOT NULL DEFAULT 0, " +
                DatabaseContract.ChecklistItemEntry.COLUMN_UPDATED_AT + " INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY (" + DatabaseContract.ChecklistItemEntry.COLUMN_CHECKLIST_ID + ") REFERENCES " +
                DatabaseContract.PreparednessChecklistEntry.TABLE_NAME + " (" + DatabaseContract.PreparednessChecklistEntry._ID + ") ON DELETE CASCADE" +
                ");";
    }

    private String getCreateSyncMetadataTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + DatabaseContract.SyncMetadataEntry.TABLE_NAME + " (" +
                DatabaseContract.SyncMetadataEntry.COLUMN_TABLE_NAME + " TEXT PRIMARY KEY, " +
                DatabaseContract.SyncMetadataEntry.COLUMN_SOURCE_NAME + " TEXT, " +
                DatabaseContract.SyncMetadataEntry.COLUMN_LAST_SYNC_TIMESTAMP + " INTEGER NOT NULL, " +
                DatabaseContract.SyncMetadataEntry.COLUMN_RECORDS_COUNT + " INTEGER NOT NULL DEFAULT 0, " +
                DatabaseContract.SyncMetadataEntry.COLUMN_SYNC_STATUS + " TEXT NOT NULL, " +
                DatabaseContract.SyncMetadataEntry.COLUMN_SYNC_HASH + " TEXT, " +
                DatabaseContract.SyncMetadataEntry.COLUMN_VERSION_TAG + " TEXT NOT NULL" +
                ");";
    }

    private void seedInitialEmergencyContacts(SQLiteDatabase db) {
        insertEmergencyContact(db, "National Emergency Service", "জাতীয় জরুরি সেবা",
                "999", "Instant police response, fire brigade dispatch, and ambulance coordination",
                "তাৎক্ষণিক পুলিশ সহায়তা, ফায়ার সার্ভিস ও অ্যাম্বুলেন্স সমন্বয়",
                "NATIONAL", "24/7", 1, "Bangladesh Police HQ / ICT Ministry", "2026-01-01", 1, 1);

        insertEmergencyContact(db, "Disaster Early Warning Center", "দুর্যোগের আগাম বার্তা কেন্দ্র",
                "1090", "Automated flood, cyclone, and maritime weather warning voice bulletins",
                "বন্যা, ঘূর্ণিঝড় ও সমুদ্র আবহাওয়া সতর্কবার্তার স্বয়ংক্রিয় ভয়েস বার্তা",
                "DISASTER", "24/7", 1, "MoDMR / Disaster Management Dept", "2026-01-01", 1, 2);

        insertEmergencyContact(db, "Health Directorate Hotline (Shastho Batayan)", "স্বাস্থ্য বাতায়ন (স্বাস্থ্য অধিদপ্তর)",
                "16263", "Free medical consultation, doctor advice, and emergency health advice",
                "বিনামূল্যে চিকিৎসকের জরুরি স্বাস্থ্য পরামর্শ ও মেডিকেল দিকনির্দেশনা",
                "MEDICAL", "24/7", 0, "DGHS / Ministry of Health", "2026-01-01", 1, 3);

        insertEmergencyContact(db, "Women & Child Helpline", "নারী ও শিশু নির্যাতন প্রতিরোধ হেল্পলাইন",
                "109", "Emergency rescue, legal and shelter support for vulnerable individuals",
                "দুর্যোগকালীন নারী ও শিশুদের জরুরি নিরাপত্তা, উদ্ধার ও সহায়তা",
                "HUMANITARIAN", "24/7", 1, "Ministry of Women & Children Affairs", "2026-01-01", 1, 4);

        insertEmergencyContact(db, "Bangladesh Red Crescent Society (BDRCS)", "বাংলাদেশ রেড ক্রিসেন্ট সোসাইটি",
                "02-222237474", "Community disaster response, blood bank, search and rescue",
                "কমিউনিটি উদ্ধার তৎপরতা, রক্ত সেবা ও জরুরি মানবিক সহায়তা",
                "HUMANITARIAN", "24/7", 0, "BDRCS National Headquarters", "2026-01-01", 1, 5);

        insertEmergencyContact(db, "Flood Forecasting & Warning Centre (FFWC)", "বন্যা পূর্বাভাস ও সতর্কীকরণ কেন্দ্র",
                "02-223386628", "Official hydrological river water level bulletins and forecasting",
                "নদী অববাহিকার পানির উচ্চতা, বিপদসীমা ও পূর্বাভাসের তথ্য",
                "FLOOD", "Office Hours & Flood Season 24h", 0, "BWDB / Water Resources Ministry", "2026-01-01", 1, 6);

        insertEmergencyContact(db, "Fire Service & Civil Defence Central Control", "ফায়ার সার্ভিস ও সিভিল ডিফেন্স কেন্দ্রীয় নিয়ন্ত্রণ কক্ষ",
                "02-223355555", "Emergency search, boat rescue, and flood evacuation response",
                "আকস্মিক উদ্ধারকাজ, স্পিডবোট উদ্ধার ও ফায়ার সার্ভিস নিয়ন্ত্রণ কক্ষ",
                "NATIONAL", "24/7", 0, "FSCD Headquarters Dhaka", "2026-01-01", 1, 7);
    }

    private void insertEmergencyContact(SQLiteDatabase db, String nameEn, String nameBn, String phone,
                                        String descEn, String descBn, String category, String hours,
                                        int isTollFree, String source, String verDate, int isEnabled, int priority) {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_EN, nameEn);
        values.put(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_BN, nameBn);
        values.put(DatabaseContract.EmergencyContactEntry.COLUMN_PHONE_NUMBER, phone);
        values.put(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_DESCRIPTION_EN, descEn);
        values.put(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_DESCRIPTION_BN, descBn);
        values.put(DatabaseContract.EmergencyContactEntry.COLUMN_CATEGORY, category);
        values.put(DatabaseContract.EmergencyContactEntry.COLUMN_AVAILABLE_HOURS, hours);
        values.put(DatabaseContract.EmergencyContactEntry.COLUMN_IS_TOLL_FREE, isTollFree);
        values.put(DatabaseContract.EmergencyContactEntry.COLUMN_SOURCE, source);
        values.put(DatabaseContract.EmergencyContactEntry.COLUMN_VERIFICATION_DATE, verDate);
        values.put(DatabaseContract.EmergencyContactEntry.COLUMN_IS_ENABLED, isEnabled);
        values.put(DatabaseContract.EmergencyContactEntry.COLUMN_PRIORITY_ORDER, priority);
        db.insert(DatabaseContract.EmergencyContactEntry.TABLE_NAME, null, values);
    }

    private void seedInitialFloodWarnings(SQLiteDatabase db) {
        insertFloodWarning(db, "WARNING",
                "Surma-Kushiyara Basin High Water Level Warning",
                "সুরমা-কুশিয়ারা অববাহিকায় পানির উচ্চতা বৃদ্ধি সতর্কবার্তা",
                "Sylhet & Sunamganj",
                "River water levels in Surma at Sunamganj point are near danger mark due to upstream heavy rainfall in Meghalaya. Low-lying union roads may become inundated.",
                "মেঘালয়ে ভারী বৃষ্টিপাতের কারণে সুনামগঞ্জ পয়েন্টে সুরমা নদীর পানি বিপদসীমার কাছাকাছি প্রবাহিত হচ্ছে। নিম্নাঞ্চলের রাস্তাঘাট প্লাবিত হতে পারে।",
                "2026-10-12 08:00 BST", "2026-10-16 18:00 BST",
                "FFWC (BWDB)", "FFWC Bulletin No. 2026-FL-14", 1, 0);

        insertFloodWarning(db, "ADVISORY",
                "Teesta Basin Monsoon Water Discharge Advisory",
                "তিস্তা অববাহিকায় মৌসুমি পানি প্রবাহ সতর্কতা",
                "Kurigram, Lalmonirhat, Nilphamari",
                "Water flow at Dalia Barrage point is currently flowing below danger level, but localized bank erosion and swell may occur in char areas.",
                "ডালিয়া পয়েন্টে তিস্তার পানি বর্তমানে বিপদসীমার নিচে অবস্থান করছে, তবে চরাঞ্চলে স্থানীয়ভাবে ভাঙন ও পানি বৃদ্ধি হতে পারে।",
                "2026-10-11 12:00 BST", "2026-10-16 12:00 BST",
                "FFWC (BWDB)", "FFWC Bulletin No. 2026-TE-08", 1, 0);

        // Historical expired warning demonstrating safe segregation
        insertFloodWarning(db, "NORMAL",
                "Dharla River Flash Surge Advisory (Past)",
                "ধরলা নদী পানির প্রবাহ সতর্কতা (অতিক্রান্ত)",
                "Kurigram",
                "Previous surge alert has subsided safely below warning threshold. River flow has stabilized.",
                "পূর্ববর্তী পানির বৃদ্ধি বিপদসীমার নিচে নেমে স্বাভাবিক অবস্থায় এসেছে। সতর্কতা সমাপ্ত।",
                "2026-10-08 06:00 BST", "2026-10-10 18:00 BST",
                "FFWC (BWDB)", "FFWC Bulletin No. 2026-DH-02", 0, 0);

        // Explicit Development Sample clearly labeled
        insertFloodWarning(db, "CRITICAL",
                "[DEV SAMPLE] Simulated Jamuna River Flash Surge Drill",
                "[টেস্টিং রেকর্ড] সিমুলেটেড যমুনা নদী আকস্মিক বন্যা ড্রিল",
                "Sirajganj & Bogura",
                "This is a verified test sample record for offline alert testing. Simulates water level rising 45cm above danger line.",
                "এটি অফলাইন সিস্টেম পরীক্ষার জন্য সংরক্ষিত একটি টেস্ট রেকর্ড। বিপদসীমার ৪৫ সেমি ওপরে পানি বৃদ্ধির সিমুলেশন।",
                "2026-10-13 06:00 BST", "2026-10-17 06:00 BST",
                "JibonShield Offline Simulation", "DEV-DRILL-JAMUNA-01", 1, 1);
    }

    private void insertFloodWarning(SQLiteDatabase db, String level, String titleEn, String titleBn,
                                    String region, String detailsEn, String detailsBn,
                                    String issuedAt, String expiresAt, String source,
                                    String sourceRef, int isActive, int isSample) {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.FloodWarningEntry.COLUMN_WARNING_LEVEL, level);
        values.put(DatabaseContract.FloodWarningEntry.COLUMN_TITLE_EN, titleEn);
        values.put(DatabaseContract.FloodWarningEntry.COLUMN_TITLE_BN, titleBn);
        values.put(DatabaseContract.FloodWarningEntry.COLUMN_REGION, region);
        values.put(DatabaseContract.FloodWarningEntry.COLUMN_DETAILS_EN, detailsEn);
        values.put(DatabaseContract.FloodWarningEntry.COLUMN_DETAILS_BN, detailsBn);
        values.put(DatabaseContract.FloodWarningEntry.COLUMN_ISSUED_AT, issuedAt);
        values.put(DatabaseContract.FloodWarningEntry.COLUMN_EXPIRES_AT, expiresAt);
        values.put(DatabaseContract.FloodWarningEntry.COLUMN_SOURCE, source);
        values.put(DatabaseContract.FloodWarningEntry.COLUMN_SOURCE_REFERENCE, sourceRef);
        values.put(DatabaseContract.FloodWarningEntry.COLUMN_IS_ACTIVE, isActive);
        values.put(DatabaseContract.FloodWarningEntry.COLUMN_IS_SAMPLE, isSample);
        values.put(DatabaseContract.FloodWarningEntry.COLUMN_LAST_SYNC_TIMESTAMP, System.currentTimeMillis());
        db.insert(DatabaseContract.FloodWarningEntry.TABLE_NAME, null, values);
    }

    private void seedInitialShelters(SQLiteDatabase db) {
        insertShelter(db, "Kutubdia Model High School Cyclone Shelter",
                "কুতুবদিয়া মডেল উচ্চ বিদ্যালয় সাইক্লোন শেল্টার",
                "Cox's Bazar", "Kutubdia", "Boroghop Union, Kutubdia Island",
                21.8155, 91.8542, 1200, 0, "01711-000001",
                "Multi-Tier Cyclone Shelter", "Reinforced concrete, ground livestock floor, wheelchair ramp",
                "MoDMR / CPP Bangladesh", 1, "2026-01-15");

        insertShelter(db, "Teknaf Coastal Multipurpose Disaster Shelter",
                "টেকনাফ উপকূলীয় বহুমুখী দুর্যোগ আশ্রয়কেন্দ্র",
                "Cox's Bazar", "Teknaf", "Sabrang Coastal Belt, Teknaf",
                20.8654, 92.2982, 1500, 0, "01819-000002",
                "Coastal Storm Surge Shelter", "High-elevation stilt design, solar lighting backup, potable rain tank",
                "MoDMR / CPP Bangladesh", 1, "2026-02-10");

        insertShelter(db, "Chhatak Upazila Flood Shelter Centre",
                "ছাতক উপজেলা বন্যা আশ্রয়কেন্দ্র",
                "Sunamganj", "Chhatak", "Chhatak Sadar High Ground, Sunamganj",
                25.0410, 91.6738, 800, 0, "01712-000003",
                "Riverine Flood Refuge", "Elevated platform, deep tubewell, dedicated emergency medical room",
                "District Relief & Rehabilitation Office", 1, "2026-03-01");

        insertShelter(db, "Companyganj High Ground Flood Refuge",
                "কোম্পানীগঞ্জ উঁচু প্ল্যাটফর্ম বন্যা আশ্রয়কেন্দ্র",
                "Sylhet", "Companyganj", "Islampur Union, Companyganj",
                25.0762, 91.7584, 950, 0, "01713-000004",
                "Flash Flood Community Shelter", "2-story elevated RCC structure, women's secure section",
                "District Relief & Rehabilitation Office", 1, "2026-03-05");

        insertShelter(db, "Chilmari Riverbank Flood Shelter Point",
                "চিলমারী নদী তীরবর্তী বন্যা আশ্রয়কেন্দ্র",
                "Kurigram", "Chilmari", "Thanahat Union, Chilmari",
                25.5562, 89.6712, 1100, 0, "01714-000005",
                "Brahmaputra Basin Flood Shelter", "Elevated embankment compound with cattle protection killa",
                "MoDMR Relief Wing", 1, "2026-02-20");

        insertShelter(db, "Kalapara Cyclone & Tidal Surge Shelter",
                "কলাপাড়া ঘূর্ণিঝড় ও জলোচ্ছ্বাস আশ্রয়কেন্দ্র",
                "Patuakhali", "Kalapara", "Kuakata Coastal Link, Kalapara",
                21.9862, 90.2415, 1400, 0, "01715-000006",
                "Tidal Surge Refuge", "Elevated ground floor, standby generator, pregnant mothers corner",
                "MoDMR / CPP Bangladesh", 1, "2026-01-28");

        insertShelter(db, "Anwara Coastal Resilience Community Shelter",
                "আনোয়ারা উপকূলীয় রেজিলিয়েন্স আশ্রয়কেন্দ্র",
                "Chittagong", "Anwara", "Parki Coastal Area, Anwara",
                22.2154, 91.9124, 1300, 0, "01716-000007",
                "Coastal Cyclone Shelter", "Reinforced wind-resistant roof, rainwater harvesting unit",
                "MoDMR / CPP Bangladesh", 1, "2026-02-14");
    }

    private void insertShelter(SQLiteDatabase db, String nameEn, String nameBn, String district, String upazila,
                              String address, double lat, double lng, int capacity, int occupancy,
                              String phone, String shelterType, String accessNotes, String source,
                              int isActive, String verifiedAt) {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.ShelterEntry.COLUMN_NAME_EN, nameEn);
        values.put(DatabaseContract.ShelterEntry.COLUMN_NAME_BN, nameBn);
        values.put(DatabaseContract.ShelterEntry.COLUMN_DISTRICT, district);
        values.put(DatabaseContract.ShelterEntry.COLUMN_UPAZILA, upazila);
        values.put(DatabaseContract.ShelterEntry.COLUMN_ADDRESS, address);
        values.put(DatabaseContract.ShelterEntry.COLUMN_LATITUDE, lat);
        values.put(DatabaseContract.ShelterEntry.COLUMN_LONGITUDE, lng);
        values.put(DatabaseContract.ShelterEntry.COLUMN_CAPACITY, capacity);
        values.put(DatabaseContract.ShelterEntry.COLUMN_CURRENT_OCCUPANCY, occupancy);
        values.put(DatabaseContract.ShelterEntry.COLUMN_CONTACT_PHONE, phone);
        values.put(DatabaseContract.ShelterEntry.COLUMN_SHELTER_TYPE, shelterType);
        values.put(DatabaseContract.ShelterEntry.COLUMN_ACCESSIBILITY_NOTES, accessNotes);
        values.put(DatabaseContract.ShelterEntry.COLUMN_SOURCE, source);
        values.put(DatabaseContract.ShelterEntry.COLUMN_IS_ACTIVE, isActive);
        values.put(DatabaseContract.ShelterEntry.COLUMN_VERIFIED_AT, verifiedAt);
        values.put(DatabaseContract.ShelterEntry.COLUMN_CREATED_AT, System.currentTimeMillis());
        values.put(DatabaseContract.ShelterEntry.COLUMN_UPDATED_AT, System.currentTimeMillis());
        db.insert(DatabaseContract.ShelterEntry.TABLE_NAME, null, values);
    }

    private void seedInitialChecklists(SQLiteDatabase db) {
        long cat1 = insertChecklistCategory(db, "GRAB_BAG", "Emergency Grab-Bag (72-Hour Survival)", "জরুরি সারভাইভাল ব্যাগ (৭২ ঘণ্টার জন্য)",
                "72-hour survival essentials for immediate evacuation", "জরুরি সময়ে অবিলম্বে আশ্রয়ে যাওয়ার জন্য প্রয়োজনীয় সরঞ্জাম", "ic_checklist", 1);
        insertChecklistItem(db, cat1, "Drinking water (minimum 3 liters per person for 3 days)", "নিরাপদ খাবার পানি (জনপ্রতি প্রতিদিন ৩ লিটার করে ৩ দিনের জন্য)", 0, 1);
        insertChecklistItem(db, cat1, "Non-perishable dry food (Chira, Muri, Gur, biscuits)", "শুকনো খাবার (চিঁড়া, মুড়ি, গুড়, বিস্কুট)", 0, 1);
        insertChecklistItem(db, cat1, "Waterproof LED torch / flashlight with spare batteries", "ওয়াটারপ্রুফ এলইডি টর্চলাইট ও অতিরিক্ত ব্যাটারি", 0, 1);
        insertChecklistItem(db, cat1, "Portable radio (for FFWC/BMD broadcasts)", "ছোট পোর্টেবল রেডিও (বন্যা ও আবহাওয়া পূর্বাভাসের জন্য)", 0, 0);
        insertChecklistItem(db, cat1, "Fully charged power bank and phone charging cable", "চার্জড পাওয়ার ব্যাংক ও মোবাইল চার্জিং ক্যাবল", 0, 1);
        insertChecklistItem(db, cat1, "Emergency whistle for signaling search and rescue", "উদ্ধারকারীদের সংকেত দেওয়ার জন্য বাঁশি (Whistle)", 0, 0);

        long cat2 = insertChecklistCategory(db, "DOCUMENTS", "Critical Documents & Valuables", "প্রয়োজনীয় নথিপত্র ও নগদ অর্থ",
                "Protection for legal and citizenship records in waterproof pouches", "জাতীয় পরিচয়পত্র ও নথিপত্র পানিতে ভেজা থেকে সুরক্ষা", "ic_shield_logo", 2);
        insertChecklistItem(db, cat2, "National ID (NID), birth certificates in waterproof pouch", "জাতীয় পরিচয়পত্র (NID) ও জন্মনিবন্ধন পলিথিন ব্যাগে মোড়ানো", 0, 1);
        insertChecklistItem(db, cat2, "Land deeds, house papers, and educational certificates", "জমির দলিল, বাড়ির কাগজ ও প্রয়োজনীয় সনদপত্র", 0, 0);
        insertChecklistItem(db, cat2, "Emergency cash money in small denominations", "জরুরি নগদ টাকা (ভাঙতি নোট)", 0, 1);

        long cat3 = insertChecklistCategory(db, "MEDICAL", "First Aid & Family Health", "ফার্স্ট এইড ও পারিবারিক ওষুধ",
                "Water purification tablets, ORS, and essential chronic prescriptions", "পানি বিশুদ্ধকরণ ট্যাবলেট, খাবার স্যালাইন ও জরুরি প্রেসক্রিপশন ওষুধ", "ic_heat", 3);
        insertChecklistItem(db, cat3, "First aid kit (bandages, antiseptic ointment, cotton)", "ফার্স্ট এইড কিট (ব্যান্ডেজ, অ্যান্টিসেপটিক, তুলা)", 0, 1);
        insertChecklistItem(db, cat3, "Oral Rehydration Salts (ORS) packets (min 10 packets)", "খাবার স্যালাইন (কমপক্ষে ১০ প্যাকেট)", 0, 1);
        insertChecklistItem(db, cat3, "Water purification tablets (Alum / Halazone)", "পানি বিশুদ্ধকরণ ট্যাবলেট (হ্যালোজেন / ফিটকিরি)", 0, 1);
        insertChecklistItem(db, cat3, "Essential chronic medications (insulin, BP, asthma) for 7 days", "নিয়মিত খাওয়ার জরুরি ওষুধ (প্রেসার, ডায়াবেটিস, ইনসুলিন, ইনহেলার)", 0, 1);

        long cat4 = insertChecklistCategory(db, "HOME", "Home & Livestock Protection", "ঘরবাড়ি ও গবাদিপশু সুরক্ষা",
                "Safeguards for property, electrical hazards, and domestic animals", "ঘরের বিদ্যুৎ বিচ্ছিন্নকরণ ও গবাদিপশু নিরাপদ স্থানে স্থানান্তর", "ic_evacuation", 4);
        insertChecklistItem(db, cat4, "Main electrical circuit breaker and gas cylinder valve shut-off", "ঘরের বিদ্যুৎ মেইন সুইচ ও গ্যাস সিলিন্ডার রেগুলেটর বন্ধ করা", 0, 1);
        insertChecklistItem(db, cat4, "Livestock untied and guided to high-elevation ground (Killa)", "গবাদিপশুর দড়ি খুলে উঁচু মাটির কিল্লা বা বাঁধে নিয়ে রাখা", 0, 1);
    }

    private long insertChecklistCategory(SQLiteDatabase db, String key, String titleEn, String titleBn,
                                         String descEn, String descBn, String icon, int order) {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.PreparednessChecklistEntry.COLUMN_CATEGORY_KEY, key);
        values.put(DatabaseContract.PreparednessChecklistEntry.COLUMN_TITLE_EN, titleEn);
        values.put(DatabaseContract.PreparednessChecklistEntry.COLUMN_TITLE_BN, titleBn);
        values.put(DatabaseContract.PreparednessChecklistEntry.COLUMN_DESCRIPTION_EN, descEn);
        values.put(DatabaseContract.PreparednessChecklistEntry.COLUMN_DESCRIPTION_BN, descBn);
        values.put(DatabaseContract.PreparednessChecklistEntry.COLUMN_ICON_NAME, icon);
        values.put(DatabaseContract.PreparednessChecklistEntry.COLUMN_DISPLAY_ORDER, order);
        return db.insert(DatabaseContract.PreparednessChecklistEntry.TABLE_NAME, null, values);
    }

    private void insertChecklistItem(SQLiteDatabase db, long categoryId, String itemEn, String itemBn, int completed, int essential) {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.ChecklistItemEntry.COLUMN_CHECKLIST_ID, categoryId);
        values.put(DatabaseContract.ChecklistItemEntry.COLUMN_ITEM_EN, itemEn);
        values.put(DatabaseContract.ChecklistItemEntry.COLUMN_ITEM_BN, itemBn);
        values.put(DatabaseContract.ChecklistItemEntry.COLUMN_IS_COMPLETED, completed);
        values.put(DatabaseContract.ChecklistItemEntry.COLUMN_IS_ESSENTIAL, essential);
        values.put(DatabaseContract.ChecklistItemEntry.COLUMN_UPDATED_AT, System.currentTimeMillis());
        db.insert(DatabaseContract.ChecklistItemEntry.TABLE_NAME, null, values);
    }

    private void seedSyncMetadata(SQLiteDatabase db) {
        long now = System.currentTimeMillis();
        insertSyncMeta(db, DatabaseContract.EmergencyContactEntry.TABLE_NAME, "BTRC / MoDMR Hotlines", now, 7, "SUCCESS", "hash-contacts-v3", "2026.3-OFFLINE");
        insertSyncMeta(db, DatabaseContract.FloodWarningEntry.TABLE_NAME, "FFWC (BWDB) Bulletins", now, 4, "SUCCESS", "hash-warnings-v3", "2026.3-OFFLINE");
        insertSyncMeta(db, DatabaseContract.ShelterEntry.TABLE_NAME, "MoDMR / CPP Shelter Registry", now, 7, "SUCCESS", "hash-shelters-v3", "2026.3-OFFLINE");
        insertSyncMeta(db, DatabaseContract.PreparednessChecklistEntry.TABLE_NAME, "National Disaster Resilience Guide", now, 4, "SUCCESS", "hash-checklists-v3", "2026.3-OFFLINE");
        insertSyncMeta(db, DatabaseContract.WaterObservationEntry.TABLE_NAME, "Community Field Logs", now, 0, "OFFLINE_CACHE", "hash-waterobs-v3", "2026.3-OFFLINE");
    }

    private void insertSyncMeta(SQLiteDatabase db, String table, String source, long time, int count, String status, String hash, String tag) {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.SyncMetadataEntry.COLUMN_TABLE_NAME, table);
        values.put(DatabaseContract.SyncMetadataEntry.COLUMN_SOURCE_NAME, source);
        values.put(DatabaseContract.SyncMetadataEntry.COLUMN_LAST_SYNC_TIMESTAMP, time);
        values.put(DatabaseContract.SyncMetadataEntry.COLUMN_RECORDS_COUNT, count);
        values.put(DatabaseContract.SyncMetadataEntry.COLUMN_SYNC_STATUS, status);
        values.put(DatabaseContract.SyncMetadataEntry.COLUMN_SYNC_HASH, hash);
        values.put(DatabaseContract.SyncMetadataEntry.COLUMN_VERSION_TAG, tag);
        db.insertWithOnConflict(DatabaseContract.SyncMetadataEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
}
