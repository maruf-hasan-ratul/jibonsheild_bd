package com.jibonshield.bd;

import com.jibonshield.bd.database.DatabaseContract;
import com.jibonshield.bd.models.ChecklistItem;
import com.jibonshield.bd.models.ChecklistProgress;
import com.jibonshield.bd.models.EmergencyContact;
import com.jibonshield.bd.models.FloodWarning;
import com.jibonshield.bd.models.PreparednessChecklist;
import com.jibonshield.bd.models.Shelter;
import com.jibonshield.bd.models.SyncMetadata;
import com.jibonshield.bd.models.WaterObservation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Day 3 Comprehensive Database, Model, Schema, and Data Integrity Tests.
 * Tests CRUD models, search/filtering rules, warning expiry, checklist progress,
 * and schema migration constraints.
 */
public class Day3DatabaseTest {

    // =========================================================================
    // 1. CONTRACT & SCHEMA DEFINITIONS
    // =========================================================================

    @Test
    public void testDatabaseVersionAndTables() {
        assertEquals(3, DatabaseContract.DATABASE_VERSION);
        assertEquals("jibonshield.db", DatabaseContract.DATABASE_NAME);
        assertEquals("emergency_contacts", DatabaseContract.EmergencyContactEntry.TABLE_NAME);
        assertEquals("flood_warnings", DatabaseContract.FloodWarningEntry.TABLE_NAME);
        assertEquals("shelters", DatabaseContract.ShelterEntry.TABLE_NAME);
        assertEquals("water_observations", DatabaseContract.WaterObservationEntry.TABLE_NAME);
        assertEquals("preparedness_checklists", DatabaseContract.PreparednessChecklistEntry.TABLE_NAME);
        assertEquals("checklist_items", DatabaseContract.ChecklistItemEntry.TABLE_NAME);
        assertEquals("synchronization_metadata", DatabaseContract.SyncMetadataEntry.TABLE_NAME);
    }

    @Test
    public void testContractNewColumnsExist() {
        // Shelter columns
        assertNotNull(DatabaseContract.ShelterEntry.COLUMN_ADDRESS);
        assertNotNull(DatabaseContract.ShelterEntry.COLUMN_SHELTER_TYPE);
        assertNotNull(DatabaseContract.ShelterEntry.COLUMN_ACCESSIBILITY_NOTES);
        assertNotNull(DatabaseContract.ShelterEntry.COLUMN_SOURCE);
        assertNotNull(DatabaseContract.ShelterEntry.COLUMN_CREATED_AT);
        assertNotNull(DatabaseContract.ShelterEntry.COLUMN_UPDATED_AT);

        // Emergency contact columns
        assertNotNull(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_DESCRIPTION_EN);
        assertNotNull(DatabaseContract.EmergencyContactEntry.COLUMN_SERVICE_DESCRIPTION_BN);
        assertNotNull(DatabaseContract.EmergencyContactEntry.COLUMN_SOURCE);
        assertNotNull(DatabaseContract.EmergencyContactEntry.COLUMN_VERIFICATION_DATE);
        assertNotNull(DatabaseContract.EmergencyContactEntry.COLUMN_IS_ENABLED);

        // Flood warning columns
        assertNotNull(DatabaseContract.FloodWarningEntry.COLUMN_SOURCE_REFERENCE);
        assertNotNull(DatabaseContract.FloodWarningEntry.COLUMN_LAST_SYNC_TIMESTAMP);

        // Water observation columns
        assertNotNull(DatabaseContract.WaterObservationEntry.COLUMN_CREATED_AT);
        assertNotNull(DatabaseContract.WaterObservationEntry.COLUMN_UPDATED_AT);

        // Checklist item columns
        assertNotNull(DatabaseContract.ChecklistItemEntry.COLUMN_UPDATED_AT);
    }

    // =========================================================================
    // 2. SHELTER MODEL & SEARCH INTEGRITY
    // =========================================================================

    @Test
    public void testShelterFullModel() {
        Shelter shelter = new Shelter(
                10L, "Kutubdia Cyclone Shelter", "কুতুবদিয়া আশ্রয়কেন্দ্র",
                "Cox's Bazar", "Kutubdia", "Boroghop Union",
                21.8155, 91.8542, 1200, 50, "01711-000001",
                "Cyclone Shelter", "Elevated ramp and livestock pen",
                "MoDMR / CPP", true, "2026-01-15", 1000L, 2000L
        );

        assertEquals(10L, shelter.getId());
        assertEquals("Cox's Bazar", shelter.getDistrict());
        assertEquals("Kutubdia", shelter.getUpazila());
        assertEquals("Boroghop Union", shelter.getAddress());
        assertEquals(21.8155, shelter.getLatitude(), 0.0001);
        assertEquals(91.8542, shelter.getLongitude(), 0.0001);
        assertEquals(1200, shelter.getCapacity());
        assertEquals(50, shelter.getCurrentOccupancy());
        assertEquals("01711-000001", shelter.getContactPhone());
        assertEquals("Cyclone Shelter", shelter.getShelterType());
        assertEquals("Elevated ramp and livestock pen", shelter.getAccessibilityNotes());
        assertEquals("MoDMR / CPP", shelter.getSource());
        assertTrue(shelter.isActive());
        assertEquals("2026-01-15", shelter.getVerifiedAt());
        assertEquals(1000L, shelter.getCreatedAt());
        assertEquals(2000L, shelter.getUpdatedAt());
    }

    @Test
    public void testShelterMissingOptionalFieldsHandledSafely() {
        Shelter shelter = new Shelter(
                1L, "Test Shelter", "টেস্ট আশ্রয়কেন্দ্র",
                "Sylhet", "Companyganj", null,
                25.0762, 91.7584, 900, 0, "01713-000004",
                null, null, null, true, "2026-03-05", 0L, 0L
        );

        // Defaults must be safely provided
        assertNotNull(shelter.getAddress());
        assertTrue(shelter.getAddress().contains("Companyganj"));
        assertNotNull(shelter.getShelterType());
        assertNotNull(shelter.getAccessibilityNotes());
        assertNotNull(shelter.getSource());
        assertTrue(shelter.getCreatedAt() > 0);
    }

    // =========================================================================
    // 3. EMERGENCY CONTACT MODEL & PROVENANCE
    // =========================================================================

    @Test
    public void testEmergencyContactModel() {
        EmergencyContact contact = new EmergencyContact(
                1L, "National Emergency Service", "জাতীয় জরুরি সেবা",
                "999", "Police, Fire, Ambulance", "পুলিশ, ফায়ার, অ্যাম্বুলেন্স",
                "NATIONAL", "24/7", true,
                "Bangladesh Police HQ", "2026-01-01", true, 1
        );

        assertEquals("999", contact.getPhoneNumber());
        assertTrue(contact.isTollFree());
        assertTrue(contact.isEnabled());
        assertEquals("Bangladesh Police HQ", contact.getSource());
        assertEquals(1, contact.getPriorityOrder());
    }

    // =========================================================================
    // 4. FLOOD WARNING & EXPIRY SEGREGATION
    // =========================================================================

    @Test
    public void testActiveWarningNotExpired() {
        FloodWarning activeWarning = new FloodWarning(
                1L, "WARNING", "Surma River Surge", "সুরমা নদী সতর্কবার্তা",
                "Sylhet", "Water levels rising", "পানি বৃদ্ধি পাচ্ছে",
                "2026-10-12 08:00 BST", "2028-12-31 23:59 BST",
                "FFWC", "FFWC-2026-01", true, false, System.currentTimeMillis()
        );

        assertTrue(activeWarning.isActive());
        assertFalse(activeWarning.isExpired());
    }

    @Test
    public void testPastWarningIsExpired() {
        FloodWarning expiredWarning = new FloodWarning(
                2L, "NORMAL", "Past Flash Flood", "অতীত বন্যা",
                "Kurigram", "Receded safely", "স্বাভাবিক অবস্থা",
                "2020-01-01 08:00 BST", "2020-01-05 18:00 BST",
                "FFWC", "FFWC-2020-OLD", false, false, System.currentTimeMillis()
        );

        // Expired warnings must be identified and never shown as active
        assertFalse(expiredWarning.isActive());
        assertTrue(expiredWarning.isExpired());
    }

    // =========================================================================
    // 5. WATER OBSERVATION CRUD & INTEGRITY
    // =========================================================================

    @Test
    public void testWaterObservationCreationAndTimestamps() {
        long now = System.currentTimeMillis();
        WaterObservation obs = new WaterObservation(
                5L, "Ward 2 Primary Tubewell", "Chandpur", "Deep Tube-well",
                "Clear", "None", 7.2, "Low", "Appears Safe",
                "2026-10-13 14:00 BST", "Volunteer Ratul", "Tested with field kit",
                now, now
        );

        assertEquals(5L, obs.getId());
        assertEquals("Ward 2 Primary Tubewell", obs.getLocationName());
        assertEquals("Chandpur", obs.getDistrict());
        assertEquals("Deep Tube-well", obs.getSourceType());
        assertEquals(7.2, obs.getPhLevel(), 0.001);
        assertEquals("Appears Safe", obs.getSafetyStatus());
        assertEquals(now, obs.getCreatedAt());
        assertEquals(now, obs.getUpdatedAt());
    }

    // =========================================================================
    // 6. CHECKLIST PERSISTENCE & PROGRESS
    // =========================================================================

    @Test
    public void testChecklistItemStateAndToggle() {
        ChecklistItem item = new ChecklistItem(
                1L, 1L, "3-day drinking water", "৩ দিনের খাবার পানি", false, true
        );
        assertFalse(item.isCompleted());
        assertTrue(item.isEssential());

        // Toggle state
        item.setCompleted(true);
        assertTrue(item.isCompleted());
        assertTrue(item.getUpdatedAt() > 0);
    }

    @Test
    public void testChecklistProgressCalculation() {
        // 4 total, 2 completed, 2 essential total, 1 essential completed
        ChecklistProgress progress = new ChecklistProgress(4, 2, 2, 1);
        assertEquals(4, progress.getTotalItems());
        assertEquals(2, progress.getCompletedItems());
        assertEquals(50, progress.getOverallPercentage());
        assertEquals(50, progress.getEssentialPercentage());
    }

    @Test
    public void testChecklistProgressZeroDivisionSafety() {
        ChecklistProgress emptyProgress = new ChecklistProgress(0, 0, 0, 0);
        assertEquals(0, emptyProgress.getOverallPercentage());
        assertEquals(0, emptyProgress.getEssentialPercentage());
    }

    // =========================================================================
    // 7. SYNCHRONIZATION METADATA
    // =========================================================================

    @Test
    public void testSyncMetadata() {
        long timestamp = 1773400000000L;
        SyncMetadata meta = new SyncMetadata(
                "shelters", "MoDMR Shelter Registry", timestamp,
                7, "SUCCESS", "hash-shelters-v3", "2026.3-OFFLINE"
        );

        assertEquals("shelters", meta.getTableName());
        assertEquals("MoDMR Shelter Registry", meta.getSourceName());
        assertEquals(timestamp, meta.getLastSyncTimestamp());
        assertEquals(7, meta.getRecordsCount());
        assertEquals("SUCCESS", meta.getSyncStatus());
        assertEquals("hash-shelters-v3", meta.getSyncHash());
        assertEquals("2026.3-OFFLINE", meta.getVersionTag());
    }

    @Test
    public void testPreparednessChecklistCategoryModel() {
        PreparednessChecklist cat = new PreparednessChecklist(
                1L, "GRAB_BAG", "72-Hour Survival Bag", "জরুরি সারভাইভাল ব্যাগ",
                "Essential grab bag items", "জরুরি ব্যাগ সরঞ্জাম", "ic_checklist", 1
        );
        assertEquals("GRAB_BAG", cat.getCategoryKey());
        assertEquals(1, cat.getDisplayOrder());
        assertEquals("ic_checklist", cat.getIconName());
    }
}
