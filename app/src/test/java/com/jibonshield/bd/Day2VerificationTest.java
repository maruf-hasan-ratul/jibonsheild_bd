package com.jibonshield.bd;

import com.jibonshield.bd.database.DatabaseContract;
import com.jibonshield.bd.models.ChecklistItem;
import com.jibonshield.bd.models.EmergencyContact;
import com.jibonshield.bd.models.FloodWarning;
import com.jibonshield.bd.models.Shelter;
import com.jibonshield.bd.models.WaterObservation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Automated Unit Test verifying Day 2 data models, schemas, and contract rules.
 */
public class Day2VerificationTest {

    @Test
    public void testDatabaseContractDefinitions() {
        assertEquals("emergency_contacts", DatabaseContract.EmergencyContactEntry.TABLE_NAME);
        assertEquals("flood_warnings", DatabaseContract.FloodWarningEntry.TABLE_NAME);
        assertEquals("shelters", DatabaseContract.ShelterEntry.TABLE_NAME);
        assertEquals("water_observations", DatabaseContract.WaterObservationEntry.TABLE_NAME);
        assertEquals("checklist_items", DatabaseContract.ChecklistItemEntry.TABLE_NAME);
        assertEquals("preparedness_checklists", DatabaseContract.PreparednessChecklistEntry.TABLE_NAME);
    }

    @Test
    public void testFloodWarningModel() {
        FloodWarning warning = new FloodWarning(
                1L, "WARNING", "Surma High Water Level", "সুরমা নদীর পানির উচ্চতা",
                "Sylhet", "River water level near danger mark", "বিপদসীমার কাছে পানি",
                "2026-10-12 08:00 BST", "2026-10-14 18:00 BST",
                "FFWC (BWDB)", true, false
        );
        assertEquals("WARNING", warning.getWarningLevel());
        assertEquals("Sylhet", warning.getRegion());
        assertTrue(warning.isActive());
        assertFalse(warning.isSample());
    }

    @Test
    public void testShelterModel() {
        Shelter shelter = new Shelter(
                1L, "Kutubdia Model Shelter", "কুতুবদিয়া আশ্রয়কেন্দ্র",
                "Cox's Bazar", "Kutubdia",
                21.8155, 91.8542, 1200, 0,
                "01711-000001", true, "2026-01-15"
        );
        assertEquals("Cox's Bazar", shelter.getDistrict());
        assertEquals(1200, shelter.getCapacity());
        assertEquals("01711-000001", shelter.getContactPhone());
    }

    @Test
    public void testEmergencyContactModel() {
        EmergencyContact contact = new EmergencyContact(
                1L, "National Emergency Service", "জাতীয় জরুরি সেবা",
                "999", "NATIONAL", "24/7", true, 1
        );
        assertEquals("999", contact.getPhoneNumber());
        assertTrue(contact.isTollFree());
        assertEquals(1, contact.getPriorityOrder());
    }

    @Test
    public void testWaterObservationModel() {
        WaterObservation obs = new WaterObservation(
                1L, "Ward 4 Tubewell", "Chandpur", "Deep Tube-well",
                "Clear", "None", 7.2, "Low", "Appears Safe",
                "2026-10-13 14:00 BST", "Volunteer", "Tested normal"
        );
        assertEquals("Ward 4 Tubewell", obs.getLocationName());
        assertEquals(7.2, obs.getPhLevel(), 0.001);
        assertEquals("Appears Safe", obs.getSafetyStatus());
    }

    @Test
    public void testChecklistItemModel() {
        ChecklistItem item = new ChecklistItem(
                1L, 1L, "Drinking water (3L/day)", "নিরাপদ খাবার পানি", false, true
        );
        assertFalse(item.isCompleted());
        assertTrue(item.isEssential());
        item.setCompleted(true);
        assertTrue(item.isCompleted());
    }
}
