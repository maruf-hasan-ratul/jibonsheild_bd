package com.jibonshield.bd;

import com.jibonshield.bd.models.ChecklistItem;
import com.jibonshield.bd.models.ChecklistProgress;
import com.jibonshield.bd.models.EmergencyContact;
import com.jibonshield.bd.models.FloodWarning;
import com.jibonshield.bd.models.Shelter;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Day 4 Master Verification Tests:
 * Validates Flood Warning System, Shelter Directory, Emergency Contacts,
 * Educational Evacuation Guidance, and Preparedness Checklist.
 */
public class Day4FeatureTest {

    // =========================================================================
    // 1. FLOOD WARNING SAFETY & EXPIRY RULES
    // =========================================================================

    @Test
    public void testActiveWarningWithFutureExpiry() {
        FloodWarning warning = new FloodWarning(
                101L, "WARNING", "Surma Rising", "সুরমা বিপদসীমার ওপরে",
                "Sylhet", "Water rising near bridge", "ব্রিজের কাছে পানি বৃদ্ধি",
                "2026-10-15 08:00 BST", "2029-12-31 23:59 BST",
                "FFWC (BWDB)", "FFWC-BULLETIN-2026-15", true, false, System.currentTimeMillis()
        );

        assertFalse("Warning with future expiry must NOT be expired", warning.isExpired());
        assertEquals("Warning synced recently should be CURRENT", FloodWarning.WarningStatus.CURRENT, warning.getWarningStatus());
        assertEquals("Official BWDB/FFWC source must be OFFICIAL_VERIFIED", FloodWarning.VerificationStatus.OFFICIAL_VERIFIED, warning.getVerificationStatus());
    }

    @Test
    public void testPastWarningIsExpired() {
        FloodWarning warning = new FloodWarning(
                102L, "CRITICAL", "Old Flash Flood Alert", "পুরাতন আকস্মিক বন্যা",
                "Sunamganj", "Historic flash flood alert", "পুরাতন ফ্ল্যাশ ফ্লাড",
                "2021-06-01 06:00 BST", "2021-06-05 18:00 BST",
                "FFWC", "FFWC-OLD-01", true, false, System.currentTimeMillis()
        );

        assertTrue("Warning with past expiry date MUST be expired", warning.isExpired());
        assertEquals("Expired warning must return EXPIRED status", FloodWarning.WarningStatus.EXPIRED, warning.getWarningStatus());
    }

    @Test
    public void testMissingExpiryNeverTreatedAsValid() {
        // "Do not treat missing expiry information as proof that a warning remains valid"
        FloodWarning nullExpiry = new FloodWarning(
                103L, "WARNING", "No Expiry Field Alert", "মেয়াদবিহীন নোটিশ",
                "Kurigram", "Testing null expiry", "টেস্ট",
                "2026-10-15 08:00 BST", null,
                "FFWC", "FFWC-01", true, false, System.currentTimeMillis()
        );
        assertTrue("Null expiry must be treated as expired", nullExpiry.isExpired());

        FloodWarning emptyExpiry = new FloodWarning(
                104L, "WARNING", "Empty Expiry Field Alert", "খালি মেয়াদ নোটিশ",
                "Kurigram", "Testing empty expiry", "টেস্ট",
                "2026-10-15 08:00 BST", "   ",
                "FFWC", "FFWC-02", true, false, System.currentTimeMillis()
        );
        assertTrue("Whitespace expiry must be treated as expired", emptyExpiry.isExpired());
    }

    @Test
    public void testUnparseableTimestampTreatedAsExpired() {
        // "Date format cannot be validated or parsed; unverifiable timestamps cannot be treated as valid"
        FloodWarning corruptedDate = new FloodWarning(
                105L, "ADVISORY", "Corrupted Date", "অস্পষ্ট তারিখ",
                "Sirajganj", "Malformed timestamp", "টেস্ট",
                "Invalid Date", "Not-A-Timestamp",
                "FFWC", "FFWC-03", true, false, System.currentTimeMillis()
        );
        assertTrue("Unparseable timestamp must be treated as expired", corruptedDate.isExpired());
    }

    @Test
    public void testIsActiveFalseForcesExpired() {
        FloodWarning deactivatedWarning = new FloodWarning(
                106L, "NORMAL", "Deactivated Warning", "স্থগিত সতর্কতা",
                "Barisal", "Cancelled alert", "বাতিল",
                "2026-10-15 08:00 BST", "2030-01-01 12:00 BST",
                "FFWC", "FFWC-04", false, false, System.currentTimeMillis()
        );
        assertTrue("Deactivated warning (isActive=false) must be expired", deactivatedWarning.isExpired());
        assertEquals(FloodWarning.WarningStatus.EXPIRED, deactivatedWarning.getWarningStatus());
    }

    @Test
    public void testDevelopmentSampleExplicitlyLabeled() {
        FloodWarning devSample = new FloodWarning(
                107L, "CRITICAL", "[DEV SAMPLE] Test Drill", "[টেস্টিং রেকর্ড] টেস্ট ড্রিল",
                "Bogura", "Simulated drill for internal test", "সিমুলেশন",
                "2026-10-15 08:00 BST", "2029-10-15 08:00 BST",
                "JibonShield Offline Simulation", "DEV-SAMPLE-01", true, true, System.currentTimeMillis()
        );

        assertFalse(devSample.isExpired());
        assertEquals("Development sample must have DEVELOPMENT_SAMPLE verification status",
                FloodWarning.VerificationStatus.DEVELOPMENT_SAMPLE, devSample.getVerificationStatus());
        assertEquals("Development sample must be marked as CACHED rather than CURRENT official alert",
                FloodWarning.WarningStatus.CACHED, devSample.getWarningStatus());
    }

    // =========================================================================
    // 2. SHELTER DIRECTORY & OFFLINE RESILIENCE
    // =========================================================================

    @Test
    public void testShelterMissingCoordinatesHandledGracefully() {
        Shelter shelterWithoutCoords = new Shelter(
                201L, "Chhatak Primary School Shelter", "ছাতক প্রাথমিক বিদ্যালয় আশ্রয়কেন্দ্র",
                "Sunamganj", "Chhatak", "Near Union Parishad Complex",
                0.0, 0.0, 500, 0, "01712-000003",
                "Flood Refuge", "Approach via main bazaar pucca road",
                "MoDMR", true, "2026-02-01", 1000L, 2000L
        );

        // Coordinates must be 0.0 without crashing or invalid computations
        assertEquals(0.0, shelterWithoutCoords.getLatitude(), 0.0001);
        assertEquals(0.0, shelterWithoutCoords.getLongitude(), 0.0001);
        assertNotNull(shelterWithoutCoords.getAccessibilityNotes());
        assertTrue("Written directions must be available offline",
                shelterWithoutCoords.getAccessibilityNotes().contains("pucca road"));
    }

    @Test
    public void testShelterMissingPhoneHandledGracefully() {
        Shelter shelterWithoutPhone = new Shelter(
                202L, "Kutubdia Secondary Cyclone Shelter", "কুতুবদিয়া বহুমুখী আশ্রয়কেন্দ্র",
                "Cox's Bazar", "Kutubdia", "South Boroghop Ward 3",
                21.82, 91.85, 1000, 0, "N/A",
                "Cyclone Shelter", "Elevated concrete ramp",
                "MoDMR / CPP", true, "2026-01-10", 1000L, 2000L
        );

        assertEquals("N/A", shelterWithoutPhone.getContactPhone());
        assertEquals(1000, shelterWithoutPhone.getCapacity());
        // Live occupancy is not tracked offline
        assertEquals(0, shelterWithoutPhone.getCurrentOccupancy());
    }

    @Test
    public void testShelterOfflineFilteringSimulation() {
        List<Shelter> allShelters = new ArrayList<>();
        allShelters.add(new Shelter(1L, "Cox Shelter 1", "কক্স আশ্রয় ১", "Cox's Bazar", "Kutubdia", "Addr 1", 21.8, 91.8, 500, 0, "01700-111111", "Cyclone", "Notes", "MoDMR", true, "2026-01-01", 0L, 0L));
        allShelters.add(new Shelter(2L, "Cox Shelter 2", "কক্স আশ্রয় ২", "Cox's Bazar", "Teknaf", "Addr 2", 20.8, 92.2, 800, 0, "01700-222222", "Cyclone", "Notes", "MoDMR", true, "2026-01-01", 0L, 0L));
        allShelters.add(new Shelter(3L, "Sylhet Shelter", "সিলেট আশ্রয়", "Sylhet", "Companyganj", "Addr 3", 25.0, 91.7, 600, 0, "01700-333333", "Flood", "Notes", "MoDMR", true, "2026-01-01", 0L, 0L));

        // Filter by District: Cox's Bazar
        List<Shelter> coxShelters = new ArrayList<>();
        for (Shelter s : allShelters) {
            if ("Cox's Bazar".equals(s.getDistrict())) coxShelters.add(s);
        }
        assertEquals(2, coxShelters.size());

        // Filter by Upazila: Kutubdia
        List<Shelter> kutubdiaShelters = new ArrayList<>();
        for (Shelter s : coxShelters) {
            if ("Kutubdia".equals(s.getUpazila())) kutubdiaShelters.add(s);
        }
        assertEquals(1, kutubdiaShelters.size());
        assertEquals("Cox Shelter 1", kutubdiaShelters.get(0).getNameEn());

        // Search by query: "Teknaf"
        List<Shelter> searchResults = new ArrayList<>();
        for (Shelter s : allShelters) {
            if (s.getNameEn().toLowerCase().contains("teknaf") || s.getUpazila().toLowerCase().contains("teknaf")) {
                searchResults.add(s);
            }
        }
        assertEquals(1, searchResults.size());
        assertEquals("Cox Shelter 2", searchResults.get(0).getNameEn());
    }

    // =========================================================================
    // 3. EMERGENCY CONTACTS & SAFE DIALING VALIDATION
    // =========================================================================

    @Test
    public void testEmergencyContactRequiredFields() {
        EmergencyContact contact = new EmergencyContact(
                1L, "National Emergency Service", "জাতীয় জরুরি সেবা",
                "999", "Police, Fire, Ambulance", "পুলিশ, ফায়ার, অ্যাম্বুলেন্স",
                "NATIONAL", "24/7", true,
                "Bangladesh Police HQ", "2026-01-01", true, 1
        );

        assertNotNull("Organization name EN required", contact.getServiceNameEn());
        assertNotNull("Organization name BN required", contact.getServiceNameBn());
        assertNotNull("Phone number required", contact.getPhoneNumber());
        assertNotNull("Source required", contact.getSource());
        assertNotNull("Verification date required", contact.getVerificationDate());
        assertTrue(contact.isEnabled());
    }

    @Test
    public void testSafeDialerPhoneValidationRules() {
        // Valid helpline numbers
        String[] validPhones = {"999", "1090", "16263", "02-222237474", "01711-000001"};
        for (String phone : validPhones) {
            assertTrue("Phone " + phone + " must be considered valid",
                    phone != null && !phone.trim().isEmpty() && phone.trim().length() >= 3 && !phone.equalsIgnoreCase("N/A"));
        }

        // Invalid numbers
        String[] invalidPhones = {null, "", "  ", "N/A", "1", "12"};
        for (String phone : invalidPhones) {
            boolean isValid = phone != null && !phone.trim().isEmpty() && phone.trim().length() >= 3 && !phone.equalsIgnoreCase("N/A");
            assertFalse("Phone '" + phone + "' must be rejected as invalid", isValid);
        }
    }

    // =========================================================================
    // 4. PREPAREDNESS CHECKLIST PERSISTENCE & RESET
    // =========================================================================

    @Test
    public void testChecklistTogglingAndProgressCalculation() {
        List<ChecklistItem> items = new ArrayList<>();
        items.add(new ChecklistItem(1L, 1L, "Drinking water", "খাবার পানি", false, true));
        items.add(new ChecklistItem(2L, 1L, "Dry food", "শুকনো খাবার", false, true));
        items.add(new ChecklistItem(3L, 2L, "NID Documents", "এনআইডি", false, true));
        items.add(new ChecklistItem(4L, 3L, "Power bank", "পাওয়ার ব্যাংক", false, false));

        // Initial: 0 completed
        ChecklistProgress initialProgress = new ChecklistProgress(4, 0, 3, 0);
        assertEquals(0, initialProgress.getOverallPercentage());
        assertEquals(0, initialProgress.getEssentialPercentage());

        // Mark item 1 and item 4 complete
        items.get(0).setCompleted(true);
        items.get(3).setCompleted(true);

        int total = items.size();
        int completed = 0;
        int essTotal = 0;
        int essCompleted = 0;
        for (ChecklistItem item : items) {
            if (item.isCompleted()) completed++;
            if (item.isEssential()) {
                essTotal++;
                if (item.isCompleted()) essCompleted++;
            }
        }

        ChecklistProgress updatedProgress = new ChecklistProgress(total, completed, essTotal, essCompleted);
        assertEquals(4, updatedProgress.getTotalItems());
        assertEquals(2, updatedProgress.getCompletedItems());
        assertEquals(50, updatedProgress.getOverallPercentage());
        assertEquals(33, updatedProgress.getEssentialPercentage());

        // Reset all items
        for (ChecklistItem item : items) {
            item.setCompleted(false);
        }
        int resetCompleted = 0;
        for (ChecklistItem item : items) {
            if (item.isCompleted()) resetCompleted++;
        }
        assertEquals(0, resetCompleted);
    }
}
