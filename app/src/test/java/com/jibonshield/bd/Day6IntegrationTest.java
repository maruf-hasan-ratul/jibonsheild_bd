package com.jibonshield.bd;

import com.jibonshield.bd.models.EmergencyContact;
import com.jibonshield.bd.models.FloodWarning;
import com.jibonshield.bd.models.Shelter;
import com.jibonshield.bd.sync.NetworkHttpClient;
import com.jibonshield.bd.sync.RemoteDataValidator;
import com.jibonshield.bd.sync.SyncResult;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Day 6 Integration & Verification Tests:
 * 1. Safe Network Access, HTTP Error Code Handling, and Exception Containment.
 * 2. Remote Data Validation (Flood Warnings, Shelters, Contacts, Schema Constraints).
 * 3. Geographic Bounds, Timestamp Sanity, and Expiry Verification.
 * 4. Offline and Stale-Data Status Classification (CURRENT, CACHED, EXPIRED, UNVERIFIED, SAMPLE).
 * 5. Synchronization Result Reporting & Data Preservation Guarantees.
 */
public class Day6IntegrationTest {

    // =========================================================================
    // 1. SAFE NETWORK CLIENT & ERROR CLASSIFICATION
    // =========================================================================

    @Test
    public void testNetworkClientInvalidUrlHandling() {
        NetworkHttpClient client = new NetworkHttpClient(2000, 2000);

        // Empty URL
        NetworkHttpClient.HttpResponse resp1 = client.executeGet("");
        assertFalse("Empty URL must fail safely", resp1.isSuccess());
        assertEquals("Empty URL must return code 0", 0, resp1.getStatusCode());

        // Null URL
        NetworkHttpClient.HttpResponse resp2 = client.executeGet(null);
        assertFalse("Null URL must fail safely", resp2.isSuccess());

        // Malformed non-http URL
        NetworkHttpClient.HttpResponse resp3 = client.executeGet("htp://invalid-scheme");
        assertFalse("Malformed URL must fail safely without throwing", resp3.isSuccess());
    }

    @Test
    public void testNetworkClientUnreachableHostHandling() {
        NetworkHttpClient client = new NetworkHttpClient(2000, 2000);
        // Guaranteed non-routable dummy domain
        NetworkHttpClient.HttpResponse resp = client.executeGet("https://invalid-host-jibonshield-test.internal.nonexistent");

        assertFalse("Non-existent host must not succeed", resp.isSuccess());
        assertEquals("Response code for DNS failure must be 0", 0, resp.getStatusCode());
        assertEquals("Error type must be DNS_FAILURE", NetworkHttpClient.HttpResponse.ErrorType.DNS_FAILURE, resp.getErrorType());
        assertTrue("Error message should mention DNS resolution", resp.getErrorMessage().contains("DNS"));
    }

    // =========================================================================
    // 2. REMOTE DATA VALIDATOR: FLOOD WARNINGS
    // =========================================================================

    @Test
    public void testValidFloodWarningPayloadParsing() {
        String validJson = "{\n" +
                "  \"version\": \"2026.1\",\n" +
                "  \"source\": \"FFWC (BWDB)\",\n" +
                "  \"timestamp\": 1760000000000,\n" +
                "  \"warnings\": [\n" +
                "    {\n" +
                "      \"warning_level\": \"WARNING\",\n" +
                "      \"title_en\": \"Surma River Rising Above Danger Level\",\n" +
                "      \"title_bn\": \"সুরমা নদীর পানি বিপদসীমার ওপরে\",\n" +
                "      \"region\": \"Sylhet\",\n" +
                "      \"details_en\": \"Water flowing 25 cm above danger limit at Kanaighat.\",\n" +
                "      \"details_bn\": \"কানাইঘাট পয়েন্টে পানি বিপদসীমার ২৫ সেমি ওপরে।\",\n" +
                "      \"issued_at\": \"2026-10-20 09:00 BST\",\n" +
                "      \"expires_at\": \"2026-10-25 18:00 BST\",\n" +
                "      \"source\": \"FFWC (BWDB)\",\n" +
                "      \"source_reference\": \"FFWC-2026-10-20-01\",\n" +
                "      \"is_active\": true,\n" +
                "      \"is_sample\": false\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        RemoteDataValidator.ValidationResult<FloodWarning> result = RemoteDataValidator.validateFloodWarnings(validJson);

        assertTrue("Valid JSON payload must pass validation", result.isValid());
        assertEquals("Expected 1 accepted item", 1, result.getAcceptedItems().size());
        assertTrue("Rejection list must be empty", result.getRejectionReasons().isEmpty());
        assertEquals("2026.1", result.getSchemaVersion());
        assertEquals("FFWC (BWDB)", result.getSourceName());

        FloodWarning item = result.getAcceptedItems().get(0);
        assertEquals("WARNING", item.getWarningLevel());
        assertEquals("Surma River Rising Above Danger Level", item.getTitleEn());
        assertEquals("Sylhet", item.getRegion());
        assertEquals("FFWC-2026-10-20-01", item.getSourceReference());
        assertFalse(item.isSample());
        assertTrue(item.isActive());
    }

    @Test
    public void testMalformedJsonRejection() {
        String malformedJson = "{ \"version\": \"1.0\", \"warnings\": [ { unclosed JSON ";
        RemoteDataValidator.ValidationResult<FloodWarning> result = RemoteDataValidator.validateFloodWarnings(malformedJson);

        assertFalse("Malformed JSON must fail validation", result.isValid());
        assertTrue("Accepted items must be empty", result.getAcceptedItems().isEmpty());
        assertFalse("Rejection reasons must be populated", result.getRejectionReasons().isEmpty());
    }

    @Test
    public void testEmptyOrNullPayloadRejection() {
        assertFalse("Null payload must be rejected", RemoteDataValidator.validateFloodWarnings(null).isValid());
        assertFalse("Empty payload must be rejected", RemoteDataValidator.validateFloodWarnings("").isValid());
        assertFalse("Whitespace payload must be rejected", RemoteDataValidator.validateFloodWarnings("   ").isValid());
    }

    @Test
    public void testMissingRequiredWarningFieldsRejection() {
        // Missing title_en
        String missingTitle = "{\n" +
                "  \"warnings\": [\n" +
                "    {\n" +
                "      \"warning_level\": \"WARNING\",\n" +
                "      \"region\": \"Sylhet\",\n" +
                "      \"details_en\": \"Details\",\n" +
                "      \"issued_at\": \"2026-10-20 09:00 BST\",\n" +
                "      \"expires_at\": \"2026-10-25 18:00 BST\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        RemoteDataValidator.ValidationResult<FloodWarning> res1 = RemoteDataValidator.validateFloodWarnings(missingTitle);
        assertTrue("Record missing title_en must be rejected", res1.getAcceptedItems().isEmpty());
        assertFalse("Rejection reason must be recorded", res1.getRejectionReasons().isEmpty());

        // Invalid warning_level
        String invalidLevel = "{\n" +
                "  \"warnings\": [\n" +
                "    {\n" +
                "      \"warning_level\": \"CATASTROPHIC_UNKNOWN\",\n" +
                "      \"title_en\": \"Title\",\n" +
                "      \"region\": \"Sylhet\",\n" +
                "      \"details_en\": \"Details\",\n" +
                "      \"issued_at\": \"2026-10-20 09:00 BST\",\n" +
                "      \"expires_at\": \"2026-10-25 18:00 BST\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        RemoteDataValidator.ValidationResult<FloodWarning> res2 = RemoteDataValidator.validateFloodWarnings(invalidLevel);
        assertTrue("Record with unrecognized severity level must be rejected", res2.getAcceptedItems().isEmpty());
    }

    @Test
    public void testInvalidWarningTimestampsRejection() {
        // Unparseable timestamp
        String badTimestamp = "{\n" +
                "  \"warnings\": [\n" +
                "    {\n" +
                "      \"warning_level\": \"CRITICAL\",\n" +
                "      \"title_en\": \"Severe Flood\",\n" +
                "      \"region\": \"Sunamganj\",\n" +
                "      \"details_en\": \"Flash flood\",\n" +
                "      \"issued_at\": \"Invalid Date String\",\n" +
                "      \"expires_at\": \"2026-10-25 18:00 BST\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        RemoteDataValidator.ValidationResult<FloodWarning> res = RemoteDataValidator.validateFloodWarnings(badTimestamp);
        assertTrue("Record with unparseable issued_at must be rejected", res.getAcceptedItems().isEmpty());
    }

    // =========================================================================
    // 3. REMOTE DATA VALIDATOR: SHELTERS & GEOGRAPHIC BOUNDS
    // =========================================================================

    @Test
    public void testValidShelterPayloadParsing() {
        String validJson = "{\n" +
                "  \"version\": \"2026.1\",\n" +
                "  \"source\": \"MoDMR / CPP\",\n" +
                "  \"shelters\": [\n" +
                "    {\n" +
                "      \"name_en\": \"Kutubdia High School Shelter\",\n" +
                "      \"name_bn\": \"কুতুবদিয়া হাই স্কুল আশ্রয়কেন্দ্র\",\n" +
                "      \"district\": \"Cox's Bazar\",\n" +
                "      \"upazila\": \"Kutubdia\",\n" +
                "      \"latitude\": 21.8156,\n" +
                "      \"longitude\": 91.8542,\n" +
                "      \"capacity\": 1200,\n" +
                "      \"current_occupancy\": 150,\n" +
                "      \"contact_phone\": \"01711000000\",\n" +
                "      \"shelter_type\": \"Cyclone Shelter\",\n" +
                "      \"source\": \"MoDMR / CPP\",\n" +
                "      \"verified_at\": \"2026-05-01\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        RemoteDataValidator.ValidationResult<Shelter> result = RemoteDataValidator.validateShelters(validJson);
        assertTrue("Valid shelter JSON must pass", result.isValid());
        assertEquals(1, result.getAcceptedItems().size());

        Shelter shelter = result.getAcceptedItems().get(0);
        assertEquals("Kutubdia High School Shelter", shelter.getNameEn());
        assertEquals("Cox's Bazar", shelter.getDistrict());
        assertEquals("Kutubdia", shelter.getUpazila());
        assertEquals(21.8156, shelter.getLatitude(), 0.0001);
        assertEquals(91.8542, shelter.getLongitude(), 0.0001);
        assertEquals(1200, shelter.getCapacity());
    }

    @Test
    public void testShelterGeographicBoundsRejection() {
        // Latitude out of Bangladesh bounds (e.g. 51.5°N - London)
        String outOfBoundsLat = "{\n" +
                "  \"shelters\": [\n" +
                "    {\n" +
                "      \"name_en\": \"Remote Shelter\",\n" +
                "      \"district\": \"Sylhet\",\n" +
                "      \"upazila\": \"Kanaighat\",\n" +
                "      \"latitude\": 51.5074,\n" +
                "      \"longitude\": 91.8542,\n" +
                "      \"capacity\": 500\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        RemoteDataValidator.ValidationResult<Shelter> res1 = RemoteDataValidator.validateShelters(outOfBoundsLat);
        assertTrue("Shelter with latitude outside Bangladesh bounds must be rejected", res1.getAcceptedItems().isEmpty());

        // Longitude out of Bangladesh bounds (e.g. 0.0°E - Greenwich)
        String outOfBoundsLng = "{\n" +
                "  \"shelters\": [\n" +
                "    {\n" +
                "      \"name_en\": \"Remote Shelter 2\",\n" +
                "      \"district\": \"Sylhet\",\n" +
                "      \"upazila\": \"Kanaighat\",\n" +
                "      \"latitude\": 24.5000,\n" +
                "      \"longitude\": 0.1278,\n" +
                "      \"capacity\": 500\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        RemoteDataValidator.ValidationResult<Shelter> res2 = RemoteDataValidator.validateShelters(outOfBoundsLng);
        assertTrue("Shelter with longitude outside Bangladesh bounds must be rejected", res2.getAcceptedItems().isEmpty());
    }

    @Test
    public void testShelterCapacityConstraintRejection() {
        // Zero capacity
        String zeroCap = "{\n" +
                "  \"shelters\": [\n" +
                "    {\n" +
                "      \"name_en\": \"Zero Capacity Shelter\",\n" +
                "      \"district\": \"Sylhet\",\n" +
                "      \"upazila\": \"Kanaighat\",\n" +
                "      \"latitude\": 24.5000,\n" +
                "      \"longitude\": 91.8000,\n" +
                "      \"capacity\": 0\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        RemoteDataValidator.ValidationResult<Shelter> res = RemoteDataValidator.validateShelters(zeroCap);
        assertTrue("Shelter with zero or negative capacity must be rejected", res.getAcceptedItems().isEmpty());
    }

    // =========================================================================
    // 4. REMOTE DATA VALIDATOR: EMERGENCY CONTACTS
    // =========================================================================

    @Test
    public void testValidEmergencyContactPayloadParsing() {
        String validJson = "{\n" +
                "  \"version\": \"2026.1\",\n" +
                "  \"source\": \"BTRC / MoDMR\",\n" +
                "  \"contacts\": [\n" +
                "    {\n" +
                "      \"service_name_en\": \"National Disaster Warning Hotline\",\n" +
                "      \"service_name_bn\": \"জাতীয় দুর্যোগ আগাম বার্তা\",\n" +
                "      \"phone_number\": \"1090\",\n" +
                "      \"service_description_en\": \"Toll-free IVR flood and cyclone warning updates\",\n" +
                "      \"category\": \"DISASTER\",\n" +
                "      \"available_hours\": \"24/7\",\n" +
                "      \"is_toll_free\": true,\n" +
                "      \"source\": \"MoDMR\",\n" +
                "      \"verification_date\": \"2026-01-01\",\n" +
                "      \"priority_order\": 2\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        RemoteDataValidator.ValidationResult<EmergencyContact> result = RemoteDataValidator.validateEmergencyContacts(validJson);
        assertTrue("Valid contact JSON must pass", result.isValid());
        assertEquals(1, result.getAcceptedItems().size());

        EmergencyContact contact = result.getAcceptedItems().get(0);
        assertEquals("1090", contact.getPhoneNumber());
        assertTrue(contact.isTollFree());
        assertEquals("DISASTER", contact.getCategory());
    }

    @Test
    public void testInvalidEmergencyPhoneNumberRejection() {
        String invalidPhone = "{\n" +
                "  \"contacts\": [\n" +
                "    {\n" +
                "      \"service_name_en\": \"Corrupted Contact\",\n" +
                "      \"phone_number\": \"abc-invalid#\",\n" +
                "      \"category\": \"MEDICAL\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        RemoteDataValidator.ValidationResult<EmergencyContact> res = RemoteDataValidator.validateEmergencyContacts(invalidPhone);
        assertTrue("Contact with non-numeric phone characters must be rejected", res.getAcceptedItems().isEmpty());
    }

    // =========================================================================
    // 5. STALE DATA & OFFLINE CLASSIFICATION INTEGRITY
    // =========================================================================

    @Test
    public void testCurrentVsCachedVsExpiredStatus() {
        long now = System.currentTimeMillis();

        // 1. Current warning: recently synced (< 24h) and future expiry
        FloodWarning currentWarning = new FloodWarning(
                1L, "CRITICAL", "Current Flood", "চলতি বন্যা", "Sylhet",
                "Water above danger", "পানি বিপদসীমার ওপরে",
                "2026-10-21 08:00 BST", "2029-12-31 23:59 BST",
                "FFWC (BWDB)", "FFWC-CURRENT", true, false, now
        );
        assertEquals("Recently synced active warning must be CURRENT", FloodWarning.WarningStatus.CURRENT, currentWarning.getWarningStatus());

        // 2. Cached warning: synced 48 hours ago (> 24h), but still unexpired
        long twoDaysAgo = now - (48L * 60 * 60 * 1000L);
        FloodWarning cachedWarning = new FloodWarning(
                2L, "WARNING", "Cached Flood", "সংরক্ষিত বন্যা", "Sunamganj",
                "Water high", "পানি বৃদ্ধি",
                "2026-10-18 08:00 BST", "2029-12-31 23:59 BST",
                "FFWC (BWDB)", "FFWC-CACHED", true, false, twoDaysAgo
        );
        assertEquals("Unexpired warning synced > 24h ago must be CACHED", FloodWarning.WarningStatus.CACHED, cachedWarning.getWarningStatus());

        // 3. Expired warning: past expiry date
        FloodWarning expiredWarning = new FloodWarning(
                3L, "WARNING", "Past Flood", "পুরাতন বন্যা", "Kurigram",
                "Old record", "পুরাতন",
                "2022-01-01 08:00 BST", "2022-01-05 18:00 BST",
                "FFWC", "FFWC-EXPIRED", true, false, now
        );
        assertEquals("Warning with past expiry date must be EXPIRED", FloodWarning.WarningStatus.EXPIRED, expiredWarning.getWarningStatus());

        // 4. Sample warning: development drill scenario
        FloodWarning sampleWarning = new FloodWarning(
                4L, "ADVISORY", "Drill Scenario", "মহড়া সতর্কবার্তা", "Dhaka",
                "Simulation", "সিমুলেশন",
                "2026-10-20 08:00 BST", "2029-12-31 23:59 BST",
                "Simulation Core", "DRILL-01", true, true, now
        );
        assertEquals("Sample warning must return DEVELOPMENT_SAMPLE verification status",
                FloodWarning.VerificationStatus.DEVELOPMENT_SAMPLE, sampleWarning.getVerificationStatus());
    }

    // =========================================================================
    // 6. SYNCHRONIZATION RESULT & DATA SAFETY GUARANTEES
    // =========================================================================

    @Test
    public void testSyncResultFactoryMethods() {
        // Success
        SyncResult success = SyncResult.success("flood_warnings", 10, 2, 8, "Updated 10 warnings");
        assertTrue(success.isSuccess());
        assertFalse(success.isOffline());
        assertEquals(SyncResult.Status.SUCCESS, success.getStatus());
        assertEquals(10, success.getTotalRecords());
        assertEquals(2, success.getInsertedRecords());
        assertEquals(8, success.getUpdatedRecords());

        // Offline
        SyncResult offline = SyncResult.noConnectivity("all");
        assertFalse(offline.isSuccess());
        assertTrue(offline.isOffline());
        assertEquals(SyncResult.Status.NO_CONNECTIVITY, offline.getStatus());

        // Validation Error
        SyncResult valErr = SyncResult.validationError("shelters", "Missing district");
        assertFalse(valErr.isSuccess());
        assertEquals(SyncResult.Status.VALIDATION_ERROR, valErr.getStatus());
        assertEquals("Missing district", valErr.getErrorMessage());

        // Server Error
        SyncResult srvErr = SyncResult.serverError("contacts", 503, "Service Unavailable");
        assertFalse(srvErr.isSuccess());
        assertEquals(SyncResult.Status.SERVER_ERROR, srvErr.getStatus());
        assertTrue(srvErr.getMessage().contains("503"));
    }
}
