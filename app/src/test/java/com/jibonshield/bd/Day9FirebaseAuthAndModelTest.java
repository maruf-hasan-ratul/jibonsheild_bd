package com.jibonshield.bd;

import static org.junit.Assert.*;

import com.jibonshield.bd.models.LiveEmergencyIncident;
import com.jibonshield.bd.models.UserProfile;

import org.junit.Test;

import java.util.Map;

/**
 * Day 9 Automated Unit Tests for Firebase Data Models & Authentication Objects:
 * - UserProfile creation, role distinction (Citizen vs Volunteer), and Firestore mapping
 * - LiveEmergencyIncident serialization and Realtime Database mapping
 * - Non-sensitive cloud profile field integrity
 */
public class Day9FirebaseAuthAndModelTest {

    @Test
    public void testUserProfileRolesAndMapping() {
        UserProfile citizen = new UserProfile(
                "user_abc_123",
                "citizen@jibonshield.local",
                "Karim Ahmed",
                UserProfile.ROLE_CITIZEN,
                "Kurigram",
                "01711000000",
                1700000000000L
        );

        assertEquals("user_abc_123", citizen.getUid());
        assertEquals("citizen@jibonshield.local", citizen.getEmail());
        assertEquals("Karim Ahmed", citizen.getDisplayName());
        assertEquals(UserProfile.ROLE_CITIZEN, citizen.getRole());
        assertFalse("Citizen should return false for isVolunteer", citizen.isVolunteer());
        assertEquals("Kurigram", citizen.getDistrict());

        Map<String, Object> citizenMap = citizen.toMap();
        assertEquals("user_abc_123", citizenMap.get("uid"));
        assertEquals("Karim Ahmed", citizenMap.get("displayName"));
        assertEquals(UserProfile.ROLE_CITIZEN, citizenMap.get("role"));
        assertFalse("Password must NEVER be present in profile map", citizenMap.containsKey("password"));

        UserProfile volunteer = new UserProfile(
                "vol_xyz_789",
                "volunteer@jibonshield.local",
                "Dr. Fatima Begum",
                UserProfile.ROLE_VOLUNTEER,
                "Sylhet",
                "01811000000",
                1700000000000L
        );

        assertTrue("Volunteer profile should return true for isVolunteer", volunteer.isVolunteer());
        assertEquals(UserProfile.ROLE_VOLUNTEER, volunteer.getRole());
    }

    @Test
    public void testLiveEmergencyIncidentMapping() {
        LiveEmergencyIncident incident = new LiveEmergencyIncident(
                "inc_surma_01",
                "CRITICAL",
                "Sylhet",
                "Surma river rising rapidly at Kanaighat; evacuation advised.",
                1700000000000L,
                "FFWC Bulletin 41",
                true
        );

        assertEquals("inc_surma_01", incident.getIncidentId());
        assertEquals("CRITICAL", incident.getSeverity());
        assertEquals("Sylhet", incident.getDistrict());
        assertTrue(incident.isVerified());

        Map<String, Object> map = incident.toMap();
        assertEquals("inc_surma_01", map.get("incidentId"));
        assertEquals("CRITICAL", map.get("severity"));
        assertEquals("Sylhet", map.get("district"));
        assertEquals(true, map.get("verified"));
    }
}

