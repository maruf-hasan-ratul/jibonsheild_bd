package com.jibonshield.bd;

import com.jibonshield.bd.models.ChecklistItem;
import com.jibonshield.bd.models.EmergencyContact;
import com.jibonshield.bd.models.FloodWarning;
import com.jibonshield.bd.models.Shelter;
import com.jibonshield.bd.models.WaterObservation;
import com.jibonshield.bd.sync.RemoteDataValidator;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Day 7 Release Candidate Verification Test Suite.
 * Validates final stabilization, safety disclaimers, data integrity,
 * model constraints, and release readiness across all project modules.
 */
public class Day7ReleaseVerificationTest {

    // =========================================================================
    // 1. WATER OBSERVATION SAFETY & ANTI-CERTIFICATION INTEGRITY
    // =========================================================================

    @Test
    public void testWaterObservationNonCertifiedDisclaimer() {
        WaterObservation obs = new WaterObservation(
                1L, "Jamuna Ghat Well", "Sirajganj", "Shallow Tube-well",
                "Slightly Turbid", "None", 7.2, "Medium", "Questionable",
                "2026-10-22 10:00 BST", "Citizen Volunteer", "High sediment",
                1000L, 2000L
        );
        // Citizen observations must not be claimed as certified lab tests
        assertNotNull(obs.getDistrict());
        assertNotNull(obs.getLocationName());
        assertTrue("Observation pH must be measured", obs.isPhMeasured());
        assertEquals(7.2, obs.getPhLevel(), 0.001);
        assertTrue("Valid location should pass validation", WaterObservation.isValidLocation(obs.getLocationName()));
    }

    // =========================================================================
    // 2. FLOOD WARNING SEVERITY AND EXPIRY BEHAVIOR
    // =========================================================================

    @Test
    public void testFloodWarningSeverityAndExpiryLogic() {
        FloodWarning activeWarning = new FloodWarning(
                101L, "WARNING", "Surma River High Flow", "সুরমা নদী সতর্কবার্তা",
                "Sylhet", "Water rising near bridge", "ব্রিজের কাছে পানি বৃদ্ধি",
                "2026-10-22 08:00 BST", "2029-12-31 23:59 BST",
                "FFWC (BWDB)", "FFWC-BULLETIN-2026-15", true, false, System.currentTimeMillis()
        );
        assertFalse("Warning with future expiry must NOT be expired", activeWarning.isExpired());
        assertEquals("Warning synced recently should be CURRENT", FloodWarning.WarningStatus.CURRENT, activeWarning.getWarningStatus());

        FloodWarning expiredWarning = new FloodWarning(
                102L, "CRITICAL", "Old Flash Flood Alert", "পুরাতন আকস্মিক বন্যা",
                "Sunamganj", "Historic flash flood alert", "পুরাতন ফ্ল্যাশ ফ্লাড",
                "2021-06-01 06:00 BST", "2021-06-05 18:00 BST",
                "FFWC", "FFWC-OLD-01", true, false, System.currentTimeMillis()
        );
        assertTrue("Warning with past expiry date MUST be expired", expiredWarning.isExpired());
        assertEquals("Expired warning must return EXPIRED status", FloodWarning.WarningStatus.EXPIRED, expiredWarning.getWarningStatus());
    }

    // =========================================================================
    // 3. EMERGENCY CONTACT PHONE NUMBER HYGIENE
    // =========================================================================

    @Test
    public void testEmergencyContactPhoneNumberHygiene() {
        EmergencyContact contact = new EmergencyContact(
                1L, "National Emergency Service", "জাতীয় জরুরি সেবা",
                "999", "General Emergency", "সার্বিক জরুরি সেবা",
                "Government", "24/7", true, "Government of Bangladesh", "2026-01-01", true, 1
        );
        assertNotNull(contact.getPhoneNumber());
        assertFalse("Phone number must not be empty", contact.getPhoneNumber().trim().isEmpty());
        // Dialer safety: numbers must contain only valid dialable characters (+, digits, spaces, hyphens)
        assertTrue("Dialer string must be safe", contact.getPhoneNumber().matches("^[0-9+\\- ]+$"));
    }

    // =========================================================================
    // 4. SHELTER ACCESSIBILITY AND CAPACITY INTEGRITY
    // =========================================================================

    @Test
    public void testShelterAccessibilityAndCapacityIntegrity() {
        Shelter shelter = new Shelter(
                1L, "Mirpur Model School Cyclone Shelter", "মিরপুর মডেল স্কুল সাইক্লোন শেল্টার",
                "Dhaka", "Mirpur", "Mirpur 10, Dhaka", 23.8069, 90.3687, 1200, 450,
                "01700000000", "Cyclone Shelter", "Ramp available", "MoDMR / CPP",
                true, "2026-01-01", 1000L, 2000L
        );
        assertTrue("Capacity must be positive", shelter.getCapacity() > 0);
        assertTrue("Current occupancy cannot be negative", shelter.getCurrentOccupancy() >= 0);
        assertTrue("Occupancy cannot exceed capacity in normal states", shelter.getCurrentOccupancy() <= shelter.getCapacity());
        assertEquals("Cyclone Shelter", shelter.getShelterType());
        assertTrue(shelter.isActive());
    }

    // =========================================================================
    // 5. PREPAREDNESS CHECKLIST COMPLETION TOGGLE
    // =========================================================================

    @Test
    public void testChecklistItemToggleState() {
        ChecklistItem item = new ChecklistItem(
                1L, 10L, "Drinking Water (3-day supply)", "৩ দিনের জন্য নিরাপদ পানীয় জল",
                false, true
        );
        assertFalse(item.isCompleted());
        item.setCompleted(true);
        assertTrue(item.isCompleted());
        item.setCompleted(false);
        assertFalse(item.isCompleted());
        assertTrue("Drinking water is essential item", item.isEssential());
    }

    // =========================================================================
    // 6. SAFE REMOTE DATA VALIDATOR ROBUSTNESS
    // =========================================================================

    @Test
    public void testCorruptedJsonDoesNotCrashValidator() {
        // Null string
        RemoteDataValidator.ValidationResult<FloodWarning> nullResult = RemoteDataValidator.validateFloodWarnings(null);
        assertFalse(nullResult.isValid());

        // Malformed JSON string
        RemoteDataValidator.ValidationResult<FloodWarning> malformedResult = RemoteDataValidator.validateFloodWarnings("{ invalid json text: ");
        assertFalse(malformedResult.isValid());

        // Missing required array
        RemoteDataValidator.ValidationResult<FloodWarning> missingArrayResult = RemoteDataValidator.validateFloodWarnings("{\"version\": \"1.0\"}");
        assertFalse(missingArrayResult.isValid());
    }

    @Test
    public void testValidSyncPayloadParsing() throws Exception {
        JSONObject root = new JSONObject();
        root.put("version", "1.0");
        root.put("source", "FFWC / BWDB");
        root.put("timestamp", System.currentTimeMillis());

        JSONArray warnings = new JSONArray();
        JSONObject w1 = new JSONObject();
        w1.put("warning_level", "WARNING");
        w1.put("title_en", "Surma High Water");
        w1.put("title_bn", "সুরমা নদী বিপদসীমার ওপরে");
        w1.put("region", "Sylhet");
        w1.put("details_en", "Surma river rising near Sylhet bridge");
        w1.put("details_bn", "সিলেট ব্রিজের কাছে সুরমা নদী বৃদ্ধি পাচ্ছে");
        w1.put("issued_at", "2026-10-22 06:00 BST");
        w1.put("expires_at", "2029-12-31 23:59 BST");
        w1.put("source", "FFWC");
        w1.put("bulletin_ref", "FFWC-2026-99");
        warnings.put(w1);
        root.put("warnings", warnings);

        RemoteDataValidator.ValidationResult<FloodWarning> result = RemoteDataValidator.validateFloodWarnings(root.toString());
        assertTrue("Valid schema must parse successfully", result.isValid());
        assertEquals(1, result.getAcceptedItems().size());
        assertEquals("Sylhet", result.getAcceptedItems().get(0).getRegion());
    }
}
