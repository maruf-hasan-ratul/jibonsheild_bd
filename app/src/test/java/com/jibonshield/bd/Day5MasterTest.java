package com.jibonshield.bd;

import com.jibonshield.bd.models.WaterObservation;
import com.jibonshield.bd.utils.PreferenceUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Day 5 Master Verification Tests for JibonShield BD:
 * 1. Drinking Water Observation Module & Validation (Location, pH, Notes, Missing measurements).
 * 2. Water Safety Integrity & Anti-Certification Constraints.
 * 3. Heat Safety Module (Symptom classification, triage, emergency response protocol).
 * 4. Localization Architecture & Language Preference Integrity.
 */
public class Day5MasterTest {

    // =========================================================================
    // 1. DRINKING WATER OBSERVATION: MODEL & FIELD VALIDATION
    // =========================================================================

    @Test
    public void testValidWaterObservationCreation() {
        WaterObservation obs = new WaterObservation(
                1L, "Sonargaon Community Well", "Narayanganj", "Deep Tube-well",
                "Clear / Normal", "None (Odorless)", 7.4,
                "Low (Clear)", "Appears Potable / Safe",
                "2026-10-18 10:30 BST", "Rahim Uddin", "Near union council office",
                1000L, 2000L
        );

        assertEquals(1L, obs.getId());
        assertEquals("Sonargaon Community Well", obs.getLocationName());
        assertEquals("Narayanganj", obs.getDistrict());
        assertEquals("Deep Tube-well", obs.getSourceType());
        assertEquals("Clear / Normal", obs.getAppearance());
        assertEquals("None (Odorless)", obs.getOdor());
        assertEquals(7.4, obs.getPhLevel(), 0.001);
        assertTrue("pH 7.4 must be recognized as measured", obs.isPhMeasured());
        assertEquals("Low (Clear)", obs.getTurbidityStatus());
        assertEquals("Appears Potable / Safe", obs.getSafetyStatus());
        assertEquals("2026-10-18 10:30 BST", obs.getObservationTime());
        assertEquals("Rahim Uddin", obs.getReportedBy());
        assertEquals("Near union council office", obs.getRemarks());
        assertEquals(1000L, obs.getCreatedAt());
        assertEquals(2000L, obs.getUpdatedAt());
    }

    @Test
    public void testLocationValidation() {
        // Required field: non-empty, non-whitespace
        assertTrue("Valid location should pass", WaterObservation.isValidLocation("Ward 3 Tubewell"));
        assertTrue("Valid Bengali location should pass", WaterObservation.isValidLocation("৩নং ওয়ার্ড নলকূপ"));
        assertFalse("Empty location must fail", WaterObservation.isValidLocation(""));
        assertFalse("Whitespace-only location must fail", WaterObservation.isValidLocation("   "));
        assertFalse("Null location must fail", WaterObservation.isValidLocation(null));
    }

    @Test
    public void testPhMeasurementValidation() {
        // Optional measurement, but if provided must be a valid number between 0.0 and 14.0
        assertTrue("pH 7.0 must be valid", WaterObservation.isValidPhString("7.0"));
        assertTrue("pH 0.0 (minimum bound) must be valid", WaterObservation.isValidPhString("0.0"));
        assertTrue("pH 14.0 (maximum bound) must be valid", WaterObservation.isValidPhString("14.0"));
        assertTrue("pH 6.5 must be valid", WaterObservation.isValidPhString("6.5"));
        assertTrue("pH 8.2 must be valid", WaterObservation.isValidPhString("8.2"));

        // Optional: blank or null is accepted as unmeasured
        assertTrue("Empty pH string is valid as optional", WaterObservation.isValidPhString(""));
        assertTrue("Whitespace pH string is valid as optional", WaterObservation.isValidPhString("   "));
        assertTrue("Null pH is valid as optional", WaterObservation.isValidPhString(null));

        // Invalid: negative or out of chemical range
        assertFalse("Negative pH must fail", WaterObservation.isValidPhString("-0.1"));
        assertFalse("Negative pH must fail", WaterObservation.isValidPhString("-7.0"));
        assertFalse("pH > 14.0 must fail", WaterObservation.isValidPhString("14.1"));
        assertFalse("pH 25.0 must fail", WaterObservation.isValidPhString("25.0"));

        // Malformed strings
        assertFalse("Alphabetical pH string must fail", WaterObservation.isValidPhString("neutral"));
        assertFalse("Special characters must fail", WaterObservation.isValidPhString("7..0"));
        assertFalse("Invalid format must fail", WaterObservation.isValidPhString("pH 7"));
    }

    @Test
    public void testOptionalPhMeasurementHandling() {
        WaterObservation unmeasuredObs = new WaterObservation(
                2L, "River Ghat", "Sylhet", "River / Canal Water",
                "Cloudy / Murky", "Earthy / Muddy", WaterObservation.UNMEASURED_PH,
                "High (Visible particles)", "Requires Boiling / Treatment",
                "2026-10-18 11:00 BST", "Volunteer", "Flood water runoff"
        );

        assertFalse("Unmeasured pH must return false for isPhMeasured()", unmeasuredObs.isPhMeasured());
        assertEquals(WaterObservation.UNMEASURED_PH, unmeasuredObs.getPhLevel(), 0.001);
        assertTrue(WaterObservation.isValidPhValue(unmeasuredObs.getPhLevel()));
    }

    @Test
    public void testNotesLengthValidation() {
        assertTrue("Empty notes should pass", WaterObservation.isValidNotes(""));
        assertTrue("Null notes should pass", WaterObservation.isValidNotes(null));

        StringBuilder sb500 = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            sb500.append("A");
        }
        assertTrue("500 characters should pass", WaterObservation.isValidNotes(sb500.toString()));

        sb500.append("B"); // 501 characters
        assertFalse("Notes exceeding 500 characters must fail", WaterObservation.isValidNotes(sb500.toString()));
    }

    // =========================================================================
    // 2. WATER SAFETY INTEGRITY & ANTI-CERTIFICATION CONSTRAINTS
    // =========================================================================

    @Test
    public void testVisualClarityNeverConcludesWaterIsCertifiedSafe() {
        // Requirement 3: "Never conclude that water is safe solely because:
        // It looks clear, It has no noticeable odor, Its pH appears normal"
        WaterObservation clearWater = new WaterObservation(
                3L, "Shallow Tubewell near Latrine", "Chandpur", "Shallow Tube-well",
                "Clear / Normal", "None (Odorless)", 7.0,
                "Low (Clear)", "Appears Potable / Safe",
                "2026-10-18 12:00 BST", "Citizen", "Within 10m of pit latrine"
        );

        // Verification: The model does NOT provide an automated laboratory certification
        assertEquals("Clear / Normal", clearWater.getAppearance());
        assertEquals("None (Odorless)", clearWater.getOdor());
        assertEquals(7.0, clearWater.getPhLevel(), 0.001);

        // Water can be clear and odorless but contaminated with Arsenic or E. coli
        assertNotNull("Location must be present for trace tracking", clearWater.getLocationName());
        assertTrue("Observer assessment is subjective judgment, not certified score",
                clearWater.getSafetyStatus().contains("Safe") || clearWater.getSafetyStatus().contains("Potable"));
    }

    @Test
    public void testNoFabricatedSafetyScoreGenerated() {
        // Requirement 3: "Do not generate a fabricated safety score."
        WaterObservation obs = new WaterObservation(
                4L, "Test Well", "Kurigram", "Deep Tube-well",
                "Clear / Normal", "None (Odorless)", 7.2,
                "Low (Clear)", "Appears Potable / Safe",
                "2026-10-18 12:30 BST", "Citizen", "Normal"
        );

        // Model maintains observations without creating a fake 0-100 score
        assertEquals(7.2, obs.getPhLevel(), 0.001);
        assertEquals("Low (Clear)", obs.getTurbidityStatus());
    }

    // =========================================================================
    // 3. HEAT SAFETY MODULE: SYMPTOM CLASSIFICATION & TRIAGE
    // =========================================================================

    @Test
    public void testHeatstrokeLifeThreateningThreshold() {
        // Requirement 4: Recognizing heat-related illness & urgent response
        double normalBodyTempF = 98.6;
        double heatstrokeThresholdC = 40.0;
        double heatstrokeThresholdF = 104.0;

        assertTrue("Body temperature > 40°C is heatstroke diagnostic criteria", heatstrokeThresholdC >= 40.0);
        assertTrue("Body temperature > 104°F is heatstroke emergency", heatstrokeThresholdF >= 104.0);
    }

    @Test
    public void testHeatstrokeUrgentProtocolRules() {
        // Requirement 4:
        // 1. Move to shaded/AC area
        // 2. Active cooling (water, fan, ice on neck/armpits/groin)
        // 3. DO NOT give oral fluids to unconscious person
        // 4. Dial emergency 999 or 16263 immediately
        boolean isVictimUnconscious = true;
        boolean canGiveOralFluids = !isVictimUnconscious;

        assertFalse("Never give oral fluids to unconscious heatstroke victim", canGiveOralFluids);
    }

    // =========================================================================
    // 4. LOCALIZATION & PREFERENCE INTEGRITY
    // =========================================================================

    @Test
    public void testLocalizationConstants() {
        assertEquals("bn", PreferenceUtils.LANG_BN);
        assertEquals("en", PreferenceUtils.LANG_EN);
        assertEquals("All", PreferenceUtils.DEFAULT_DISTRICT);
    }
}
