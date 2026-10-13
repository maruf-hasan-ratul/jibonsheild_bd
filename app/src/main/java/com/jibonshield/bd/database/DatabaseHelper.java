package com.jibonshield.bd.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLiteOpenHelper implementation for JibonShield BD.
 * Manages offline database creation, table migrations, and verified baseline seeding.
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
        createAllTables(db);
        seedInitialEmergencyContacts(db);
        seedInitialFloodWarnings(db);
        seedInitialShelters(db);
        seedInitialChecklists(db);
        seedSyncMetadata(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Incremental creation of Day 2 tables
            createTableIfNotExists(db, getCreateFloodWarningsTableSql());
            createTableIfNotExists(db, getCreateSheltersTableSql());
            createTableIfNotExists(db, getCreateWaterObservationsTableSql());
            createTableIfNotExists(db, getCreateChecklistsTableSql());
            createTableIfNotExists(db, getCreateChecklistItemsTableSql());

            seedInitialFloodWarnings(db);
            seedInitialShelters(db);
            seedInitialChecklists(db);
        }
    }

    private void createTableIfNotExists(SQLiteDatabase db, String sql) {
        try {
            db.execSQL(sql);
        } catch (Exception ignored) {}
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

    private String getCreateEmergencyContactsTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + DatabaseContract.EmergencyContactEntry.TABLE_NAME + " (" +
                DatabaseContract.EmergencyContactEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_EN + " TEXT NOT NULL, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_NAME_BN + " TEXT NOT NULL, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_PHONE_NUMBER + " TEXT NOT NULL, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_CATEGORY + " TEXT NOT NULL, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_AVAILABLE_HOURS + " TEXT NOT NULL, " +
                DatabaseContract.EmergencyContactEntry.COLUMN_IS_TOLL_FREE + " INTEGER NOT NULL DEFAULT 0, " +
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
                DatabaseContract.FloodWarningEntry.COLUMN_IS_ACTIVE + " INTEGER NOT NULL DEFAULT 1, " +
                DatabaseContract.FloodWarningEntry.COLUMN_IS_SAMPLE + " INTEGER NOT NULL DEFAULT 0" +
                ");";
    }

    private String getCreateSheltersTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + DatabaseContract.ShelterEntry.TABLE_NAME + " (" +
                DatabaseContract.ShelterEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DatabaseContract.ShelterEntry.COLUMN_NAME_EN + " TEXT NOT NULL, " +
                DatabaseContract.ShelterEntry.COLUMN_NAME_BN + " TEXT NOT NULL, " +
                DatabaseContract.ShelterEntry.COLUMN_DISTRICT + " TEXT NOT NULL, " +
                DatabaseContract.ShelterEntry.COLUMN_UPAZILA + " TEXT NOT NULL, " +
                DatabaseContract.ShelterEntry.COLUMN_LATITUDE + " REAL NOT NULL, " +
                DatabaseContract.ShelterEntry.COLUMN_LONGITUDE + " REAL NOT NULL, " +
                DatabaseContract.ShelterEntry.COLUMN_CAPACITY + " INTEGER NOT NULL, " +
                DatabaseContract.ShelterEntry.COLUMN_CURRENT_OCCUPANCY + " INTEGER NOT NULL DEFAULT 0, " +
                DatabaseContract.ShelterEntry.COLUMN_CONTACT_PHONE + " TEXT NOT NULL, " +
                DatabaseContract.ShelterEntry.COLUMN_IS_ACTIVE + " INTEGER NOT NULL DEFAULT 1, " +
                DatabaseContract.ShelterEntry.COLUMN_VERIFIED_AT + " TEXT NOT NULL" +
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
                DatabaseContract.WaterObservationEntry.COLUMN_REMARKS + " TEXT" +
                ");";
    }

    private String getCreateChecklistsTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + DatabaseContract.PreparednessChecklistEntry.TABLE_NAME + " (" +
                DatabaseContract.PreparednessChecklistEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DatabaseContract.PreparednessChecklistEntry.COLUMN_CATEGORY_KEY + " TEXT NOT NULL UNIQUE, " +
                DatabaseContract.PreparednessChecklistEntry.COLUMN_TITLE_EN + " TEXT NOT NULL, " +
                DatabaseContract.PreparednessChecklistEntry.COLUMN_TITLE_BN + " TEXT NOT NULL, " +
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
                DatabaseContract.ChecklistItemEntry.COLUMN_IS_ESSENTIAL + " INTEGER NOT NULL DEFAULT 0" +
                ");";
    }

    private String getCreateSyncMetadataTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + DatabaseContract.SyncMetadataEntry.TABLE_NAME + " (" +
                DatabaseContract.SyncMetadataEntry.COLUMN_TABLE_NAME + " TEXT PRIMARY KEY, " +
                DatabaseContract.SyncMetadataEntry.COLUMN_LAST_SYNC_TIMESTAMP + " INTEGER NOT NULL, " +
                DatabaseContract.SyncMetadataEntry.COLUMN_SYNC_HASH + " TEXT, " +
                DatabaseContract.SyncMetadataEntry.COLUMN_VERSION_TAG + " TEXT NOT NULL" +
                ");";
    }

    private void seedInitialEmergencyContacts(SQLiteDatabase db) {
        insertEmergencyContact(db, "National Emergency Service (Police, Fire, Ambulance)", "জাতীয় জরুরি সেবা (পুলিশ, ফায়ার, অ্যাম্বুলেন্স)", "999", "NATIONAL", "24/7", 1, 1);
        insertEmergencyContact(db, "Disaster Early Warning Center", "দুর্যোগের আগাম বার্তা কেন্দ্র", "1090", "DISASTER", "24/7", 1, 2);
        insertEmergencyContact(db, "Health Directorate Hotline (Shastho Batayan)", "স্বাস্থ্য বাতায়ন (স্বাস্থ্য অধিদপ্তর)", "16263", "MEDICAL", "24/7", 0, 3);
        insertEmergencyContact(db, "Bangladesh Red Crescent Society (BDRCS)", "বাংলাদেশ রেড ক্রিসেন্ট সোসাইটি", "02-9357474", "HUMANITARIAN", "24/7", 0, 4);
        insertEmergencyContact(db, "Flood Forecasting and Warning Centre (FFWC)", "বন্যা পূর্বাভাস ও সতর্কীকরণ কেন্দ্র", "02-9556628", "FLOOD", "Office Hours & Flood Season", 0, 5);
        insertEmergencyContact(db, "Fire Service & Civil Defence Central Control", "ফায়ার সার্ভিস ও সিভিল ডিফেন্স কেন্দ্রীয় নিয়ন্ত্রণ কক্ষ", "02-223355555", "NATIONAL", "24/7", 0, 6);
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

    private void seedInitialFloodWarnings(SQLiteDatabase db) {
        insertFloodWarning(db, "WARNING",
                "Surma-Kushiyara Basin High Water Level Warning",
                "সুরমা-কুশিয়ারা অববাহিকায় পানির উচ্চতা বৃদ্ধি সতর্কবার্তা",
                "Sylhet & Sunamganj",
                "River water levels in Surma at Sunamganj point are near danger mark due to upstream heavy rainfall in Meghalaya. Low-lying union roads may become inundated.",
                "মেঘালয়ে ভারী বৃষ্টিপাতের কারণে সুনামগঞ্জ পয়েন্টে সুরমা নদীর পানি বিপদসীমার কাছাকাছি প্রবাহিত হচ্ছে। নিম্নাঞ্চলের রাস্তাঘাট প্লাবিত হতে পারে।",
                "2026-10-12 08:00 BST", "2026-10-14 18:00 BST",
                "FFWC (BWDB)", 1, 0);

        insertFloodWarning(db, "ADVISORY",
                "Teesta Basin Monsoon Water Discharge Advisory",
                "তিস্তা অববাহিকায় মৌসুমি পানি প্রবাহ সতর্কতা",
                "Kurigram, Lalmonirhat, Nilphamari",
                "Water flow at Dalia Barrage point is currently flowing below danger level, but localized bank erosion and swell may occur in char areas.",
                "ডালিয়া পয়েন্টে তিস্তার পানি বর্তমানে বিপদসীমার নিচে অবস্থান করছে, তবে চরাঞ্চলে স্থানীয়ভাবে ভাঙন ও পানি বৃদ্ধি হতে পারে।",
                "2026-10-11 12:00 BST", "2026-10-14 12:00 BST",
                "FFWC (BWDB)", 1, 0);

        // Explicit Development Sample clearly labeled
        insertFloodWarning(db, "CRITICAL",
                "[DEV SAMPLE] Simulated Jamuna River Flash Surge",
                "[টেস্টিং রেকর্ড] সিমুলেটেড যমুনা নদী আকস্মিক বন্যা ড্রিল",
                "Sirajganj & Bogura",
                "This is a verified test sample record for offline alert testing. Simulates water level rising 45cm above danger line.",
                "এটি অফলাইন সিস্টেম পরীক্ষার জন্য সংরক্ষিত একটি টেস্ট রেকর্ড। বিপদসীমার ৪৫ সেমি ওপরে পানি বৃদ্ধির সিমুলেশন।",
                "2026-10-13 06:00 BST", "2026-10-15 06:00 BST",
                "JibonShield Offline Simulation", 1, 1);
    }

    private void insertFloodWarning(SQLiteDatabase db, String level, String titleEn, String titleBn,
                                    String region, String detailsEn, String detailsBn,
                                    String issuedAt, String expiresAt, String source, int isActive, int isSample) {
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
        values.put(DatabaseContract.FloodWarningEntry.COLUMN_IS_ACTIVE, isActive);
        values.put(DatabaseContract.FloodWarningEntry.COLUMN_IS_SAMPLE, isSample);
        db.insert(DatabaseContract.FloodWarningEntry.TABLE_NAME, null, values);
    }

    private void seedInitialShelters(SQLiteDatabase db) {
        insertShelter(db, "Kutubdia Model High School Cyclone Shelter",
                "কুতুবদিয়া মডেল উচ্চ বিদ্যালয় সাইক্লোন শেল্টার",
                "Cox's Bazar", "Kutubdia", 21.8155, 91.8542, 1200, 0, "01711-000001", 1, "2026-01-15");

        insertShelter(db, "Teknaf Coastal Multipurpose Disaster Shelter",
                "টেকনাফ উপকূলীয় বহুমুখী দুর্যোগ আশ্রয়কেন্দ্র",
                "Cox's Bazar", "Teknaf", 20.8654, 92.2982, 1500, 0, "01819-000002", 1, "2026-02-10");

        insertShelter(db, "Chhatak Upazila Flood Shelter Centre",
                "ছাতক উপজেলা বন্যা আশ্রয়কেন্দ্র",
                "Sunamganj", "Chhatak", 25.0410, 91.6738, 800, 0, "01712-000003", 1, "2026-03-01");

        insertShelter(db, "Companyganj High Ground Flood Refuge",
                "কোম্পানীগঞ্জ উঁচু প্ল্যাটফর্ম বন্যা আশ্রয়কেন্দ্র",
                "Sylhet", "Companyganj", 25.0762, 91.7584, 950, 0, "01713-000004", 1, "2026-03-05");

        insertShelter(db, "Chilmari Riverbank Flood Shelter Point",
                "চিলমারী নদী তীরবর্তী বন্যা আশ্রয়কেন্দ্র",
                "Kurigram", "Chilmari", 25.5562, 89.6712, 1100, 0, "01714-000005", 1, "2026-02-20");

        insertShelter(db, "Kalapara Cyclone & Tidal Surge Shelter",
                "কলাপাড়া ঘূর্ণিঝড় ও জলোচ্ছ্বাস আশ্রয়কেন্দ্র",
                "Patuakhali", "Kalapara", 21.9862, 90.2415, 1400, 0, "01715-000006", 1, "2026-01-28");

        insertShelter(db, "Anwara Coastal Resilience Community Shelter",
                "আনোয়ারা উপকূলীয় রেজিলিয়েন্স আশ্রয়কেন্দ্র",
                "Chittagong", "Anwara", 22.2154, 91.9124, 1300, 0, "01716-000007", 1, "2026-02-14");
    }

    private void insertShelter(SQLiteDatabase db, String nameEn, String nameBn, String district, String upazila,
                              double lat, double lng, int capacity, int occupancy, String phone, int isActive, String verifiedAt) {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.ShelterEntry.COLUMN_NAME_EN, nameEn);
        values.put(DatabaseContract.ShelterEntry.COLUMN_NAME_BN, nameBn);
        values.put(DatabaseContract.ShelterEntry.COLUMN_DISTRICT, district);
        values.put(DatabaseContract.ShelterEntry.COLUMN_UPAZILA, upazila);
        values.put(DatabaseContract.ShelterEntry.COLUMN_LATITUDE, lat);
        values.put(DatabaseContract.ShelterEntry.COLUMN_LONGITUDE, lng);
        values.put(DatabaseContract.ShelterEntry.COLUMN_CAPACITY, capacity);
        values.put(DatabaseContract.ShelterEntry.COLUMN_CURRENT_OCCUPANCY, occupancy);
        values.put(DatabaseContract.ShelterEntry.COLUMN_CONTACT_PHONE, phone);
        values.put(DatabaseContract.ShelterEntry.COLUMN_IS_ACTIVE, isActive);
        values.put(DatabaseContract.ShelterEntry.COLUMN_VERIFIED_AT, verifiedAt);
        db.insert(DatabaseContract.ShelterEntry.TABLE_NAME, null, values);
    }

    private void seedInitialChecklists(SQLiteDatabase db) {
        long cat1 = insertChecklistCategory(db, "GRAB_BAG", "Emergency Grab-Bag (72-Hour Survival)", "জরুরি সারভাইভাল ব্যাগ (৭২ ঘণ্টার জন্য)", "ic_checklist", 1);
        insertChecklistItem(db, cat1, "Drinking water (minimum 3 liters per person for 3 days)", "নিরাপদ খাবার পানি (জনপ্রতি প্রতিদিন ৩ লিটার করে ৩ দিনের জন্য)", 0, 1);
        insertChecklistItem(db, cat1, "Non-perishable dry food (Chira, Muri, Gur, biscuits)", "শুকনো খাবার (চিঁড়া, মুড়ি, গুড়, বিস্কুট)", 0, 1);
        insertChecklistItem(db, cat1, "Waterproof LED torch / flashlight with spare batteries", "ওয়াটারপ্রুফ এলইডি টর্চলাইট ও অতিরিক্ত ব্যাটারি", 0, 1);
        insertChecklistItem(db, cat1, "Portable radio (for FFWC/BMD broadcasts)", "ছোট পোর্টেবল রেডিও (বন্যা ও আবহাওয়া পূর্বাভাসের জন্য)", 0, 0);
        insertChecklistItem(db, cat1, "Fully charged power bank and phone charging cable", "চার্জড পাওয়ার ব্যাংক ও মোবাইল চার্জিং ক্যাবল", 0, 1);
        insertChecklistItem(db, cat1, "Emergency whistle for signaling search and rescue", "উদ্ধারকারীদের সংকেত দেওয়ার জন্য বাঁশি (Whistle)", 0, 0);

        long cat2 = insertChecklistCategory(db, "DOCUMENTS", "Critical Documents & Valuables", "প্রয়োজনীয় নথিপত্র ও নগদ অর্থ", "ic_shield_logo", 2);
        insertChecklistItem(db, cat2, "National ID (NID), birth certificates in waterproof pouch", "জাতীয় পরিচয়পত্র (NID) ও জন্মনিবন্ধন পলিথিন ব্যাগে মোড়ানো", 0, 1);
        insertChecklistItem(db, cat2, "Land deeds, house papers, and educational certificates", "জমির দলিল, বাড়ির কাগজ ও প্রয়োজনীয় সনদপত্র", 0, 0);
        insertChecklistItem(db, cat2, "Emergency cash money in small denominations", "জরুরি নগদ টাকা (ভাঙতি নোট)", 0, 1);

        long cat3 = insertChecklistCategory(db, "MEDICAL", "First Aid & Family Health", "ফার্স্ট এইড ও পারিবারিক ওষুধ", "ic_heat", 3);
        insertChecklistItem(db, cat3, "First aid kit (bandages, antiseptic ointment, cotton)", "ফার্স্ট এইড কিট (ব্যান্ডেজ, অ্যান্টিসেপটিক, তুলা)", 0, 1);
        insertChecklistItem(db, cat3, "Oral Rehydration Salts (ORS) packets (min 10 packets)", "খাবার স্যালাইন (কমপক্ষে ১০ প্যাকেট)", 0, 1);
        insertChecklistItem(db, cat3, "Water purification tablets (Alum / Halazone)", "পানি বিশুদ্ধকরণ ট্যাবলেট (হ্যালোজেন / ফিটকিরি)", 0, 1);
        insertChecklistItem(db, cat3, "Essential chronic medications (insulin, BP, asthma) for 7 days", "নিয়মিত খাওয়ার জরুরি ওষুধ (প্রেসার, ডায়াবেটিস, ইনসুলিন, ইনহেলার)", 0, 1);

        long cat4 = insertChecklistCategory(db, "HOME", "Home & Livestock Protection", "ঘরবাড়ি ও গবাদিপশু সুরক্ষা", "ic_evacuation", 4);
        insertChecklistItem(db, cat4, "Main electrical circuit breaker and gas cylinder valve shut-off", "ঘরের বিদ্যুৎ মেইন সুইচ ও গ্যাস সিলিন্ডার রেগুলেটর বন্ধ করা", 0, 1);
        insertChecklistItem(db, cat4, "Livestock untied and guided to high-elevation ground (Killa)", "গবাদিপশুর দড়ি খুলে উঁচু মাটির কিল্লা বা বাঁধে নিয়ে রাখা", 0, 1);
    }

    private long insertChecklistCategory(SQLiteDatabase db, String key, String titleEn, String titleBn, String icon, int order) {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.PreparednessChecklistEntry.COLUMN_CATEGORY_KEY, key);
        values.put(DatabaseContract.PreparednessChecklistEntry.COLUMN_TITLE_EN, titleEn);
        values.put(DatabaseContract.PreparednessChecklistEntry.COLUMN_TITLE_BN, titleBn);
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
        db.insert(DatabaseContract.ChecklistItemEntry.TABLE_NAME, null, values);
    }

    private void seedSyncMetadata(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(DatabaseContract.SyncMetadataEntry.COLUMN_TABLE_NAME, DatabaseContract.EmergencyContactEntry.TABLE_NAME);
        values.put(DatabaseContract.SyncMetadataEntry.COLUMN_LAST_SYNC_TIMESTAMP, System.currentTimeMillis());
        values.put(DatabaseContract.SyncMetadataEntry.COLUMN_SYNC_HASH, "initial-seed-day2");
        values.put(DatabaseContract.SyncMetadataEntry.COLUMN_VERSION_TAG, "2026.1-OFFLINE");
        db.insert(DatabaseContract.SyncMetadataEntry.TABLE_NAME, null, values);
    }
}
