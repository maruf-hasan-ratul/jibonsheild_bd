package com.jibonshield.bd.database;

import android.provider.BaseColumns;

/**
 * Contract class specifying the SQLite database schema for JibonShield BD.
 * Version 3: Comprehensive persistent offline storage, models, integrity constraints,
 * data provenance tracking, and synchronization metadata.
 */
public final class DatabaseContract {

    private DatabaseContract() {
        // Prevent instantiation
    }

    public static final String DATABASE_NAME = "jibonshield.db";
    public static final int DATABASE_VERSION = 3;

    /**
     * Emergency Contacts Table
     * Stores verified national disaster, medical, and rescue helplines.
     */
    public static final class EmergencyContactEntry implements BaseColumns {
        public static final String TABLE_NAME = "emergency_contacts";
        public static final String COLUMN_SERVICE_NAME_EN = "service_name_en";
        public static final String COLUMN_SERVICE_NAME_BN = "service_name_bn";
        public static final String COLUMN_PHONE_NUMBER = "phone_number";
        public static final String COLUMN_SERVICE_DESCRIPTION_EN = "service_description_en";
        public static final String COLUMN_SERVICE_DESCRIPTION_BN = "service_description_bn";
        public static final String COLUMN_CATEGORY = "category"; // NATIONAL, DISASTER, MEDICAL, HUMANITARIAN, FLOOD
        public static final String COLUMN_AVAILABLE_HOURS = "available_hours";
        public static final String COLUMN_IS_TOLL_FREE = "is_toll_free";
        public static final String COLUMN_SOURCE = "source"; // Provenance authority
        public static final String COLUMN_VERIFICATION_DATE = "verification_date";
        public static final String COLUMN_IS_ENABLED = "is_enabled"; // 1 = enabled, 0 = disabled
        public static final String COLUMN_PRIORITY_ORDER = "priority_order";
    }

    /**
     * Flood Warnings Table
     * Stores official bulletins from FFWC/BWDB and labeled sample drill scenarios.
     */
    public static final class FloodWarningEntry implements BaseColumns {
        public static final String TABLE_NAME = "flood_warnings";
        public static final String COLUMN_WARNING_LEVEL = "warning_level"; // CRITICAL, WARNING, ADVISORY, NORMAL
        public static final String COLUMN_TITLE_EN = "title_en";
        public static final String COLUMN_TITLE_BN = "title_bn";
        public static final String COLUMN_REGION = "region"; // Affected district / basin
        public static final String COLUMN_DETAILS_EN = "details_en";
        public static final String COLUMN_DETAILS_BN = "details_bn";
        public static final String COLUMN_ISSUED_AT = "issued_at"; // ISO-8601 or BST date string
        public static final String COLUMN_EXPIRES_AT = "expires_at"; // Expiry timestamp or BST date string
        public static final String COLUMN_SOURCE = "source"; // e.g. FFWC (BWDB)
        public static final String COLUMN_SOURCE_REFERENCE = "source_reference"; // e.g. Bulletin No. / Reference
        public static final String COLUMN_IS_ACTIVE = "is_active"; // 1 = active, 0 = inactive/expired
        public static final String COLUMN_IS_SAMPLE = "is_sample"; // 1 = dev sample drill, 0 = verified bulletin
        public static final String COLUMN_LAST_SYNCED_AT = "last_synced_at"; // Epoch millis
        public static final String COLUMN_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"; // Epoch millis
    }

    /**
     * Shelters Table
     * Designated cyclone and flood shelters across vulnerable divisions.
     */
    public static final class ShelterEntry implements BaseColumns {
        public static final String TABLE_NAME = "shelters";
        public static final String COLUMN_NAME_EN = "name_en";
        public static final String COLUMN_NAME_BN = "name_bn";
        public static final String COLUMN_DISTRICT = "district";
        public static final String COLUMN_UPAZILA = "upazila";
        public static final String COLUMN_ADDRESS = "address";
        public static final String COLUMN_LATITUDE = "latitude";
        public static final String COLUMN_LONGITUDE = "longitude";
        public static final String COLUMN_CAPACITY = "capacity";
        public static final String COLUMN_CURRENT_OCCUPANCY = "current_occupancy";
        public static final String COLUMN_CONTACT_PHONE = "contact_phone";
        public static final String COLUMN_SHELTER_TYPE = "shelter_type"; // Cyclone Shelter, Flood Refuge, School, etc.
        public static final String COLUMN_ACCESSIBILITY_NOTES = "accessibility_notes"; // Elevated ramp, livestock pen, etc.
        public static final String COLUMN_SOURCE = "source"; // Provenance (e.g. MoDMR / CPP)
        public static final String COLUMN_VERIFIED_AT = "verified_at";
        public static final String COLUMN_IS_ACTIVE = "is_active";
        public static final String COLUMN_CREATED_AT = "created_at";
        public static final String COLUMN_UPDATED_AT = "updated_at";
    }

    /**
     * Water Observations Table
     * Local community water quality and potability observations recorded offline.
     */
    public static final class WaterObservationEntry implements BaseColumns {
        public static final String TABLE_NAME = "water_observations";
        public static final String COLUMN_LOCATION_NAME = "location_name";
        public static final String COLUMN_DISTRICT = "district";
        public static final String COLUMN_SOURCE_TYPE = "source_type";
        public static final String COLUMN_APPEARANCE = "appearance";
        public static final String COLUMN_ODOR = "odor";
        public static final String COLUMN_PH_LEVEL = "ph_level";
        public static final String COLUMN_TURBIDITY_STATUS = "turbidity_status";
        public static final String COLUMN_SAFETY_STATUS = "safety_status";
        public static final String COLUMN_OBSERVATION_TIME = "observation_time";
        public static final String COLUMN_REPORTED_BY = "reported_by";
        public static final String COLUMN_REMARKS = "remarks";
        public static final String COLUMN_IMAGE_URI = "image_uri"; // Local content URI or cached path
        public static final String COLUMN_IMAGE_URL = "image_url"; // Cloud Firebase Storage URL
        public static final String COLUMN_SYNC_STATUS = "sync_status"; // PENDING, SYNCED, FAILED
        public static final String COLUMN_CREATED_AT = "created_at"; // Epoch millis
        public static final String COLUMN_UPDATED_AT = "updated_at"; // Epoch millis
    }

    /**
     * Preparedness Checklists Category Table
     */
    public static final class PreparednessChecklistEntry implements BaseColumns {
        public static final String TABLE_NAME = "preparedness_checklists";
        public static final String COLUMN_CATEGORY_KEY = "category_key";
        public static final String COLUMN_TITLE_EN = "title_en";
        public static final String COLUMN_TITLE_BN = "title_bn";
        public static final String COLUMN_DESCRIPTION_EN = "description_en";
        public static final String COLUMN_DESCRIPTION_BN = "description_bn";
        public static final String COLUMN_ICON_NAME = "icon_name";
        public static final String COLUMN_DISPLAY_ORDER = "display_order";
    }

    /**
     * Preparedness Checklist Items Table
     */
    public static final class ChecklistItemEntry implements BaseColumns {
        public static final String TABLE_NAME = "checklist_items";
        public static final String COLUMN_CHECKLIST_ID = "checklist_id"; // Foreign key
        public static final String COLUMN_ITEM_EN = "item_en";
        public static final String COLUMN_ITEM_BN = "item_bn";
        public static final String COLUMN_IS_COMPLETED = "is_completed"; // 0 = unchecked, 1 = completed
        public static final String COLUMN_IS_ESSENTIAL = "is_essential"; // 1 = life critical, 0 = supplementary
        public static final String COLUMN_UPDATED_AT = "updated_at"; // Epoch millis
    }

    /**
     * Synchronization Metadata Table
     * Tracks offline data provenance, sync state, and entity revision signatures.
     */
    public static final class SyncMetadataEntry {
        public static final String TABLE_NAME = "synchronization_metadata";
        public static final String COLUMN_TABLE_NAME = "table_name";
        public static final String COLUMN_SOURCE_NAME = "source_name";
        public static final String COLUMN_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"; // Epoch millis
        public static final String COLUMN_RECORDS_COUNT = "records_count";
        public static final String COLUMN_SYNC_STATUS = "sync_status"; // SUCCESS, OFFLINE_CACHE, OUTDATED
        public static final String COLUMN_SYNC_HASH = "sync_hash";
        public static final String COLUMN_VERSION_TAG = "version_tag";
    }
}
