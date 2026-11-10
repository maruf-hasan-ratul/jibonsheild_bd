package com.jibonshield.bd;

import com.jibonshield.bd.database.ModelMapper;
import com.jibonshield.bd.database.converters.StringListConverter;
import com.jibonshield.bd.database.entity.DisasterUpdateEntity;
import com.jibonshield.bd.database.entity.DistrictEntity;
import com.jibonshield.bd.database.entity.DivisionEntity;
import com.jibonshield.bd.database.entity.ShelterEntity;
import com.jibonshield.bd.database.entity.UpazilaEntity;
import com.jibonshield.bd.database.entity.UserProfileEntity;
import com.jibonshield.bd.models.FloodWarning;
import com.jibonshield.bd.models.Shelter;
import com.jibonshield.bd.models.UserProfile;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests verifying Room database entities, converters, ModelMapper,
 * and User Profile Hometown schema.
 */
public class OfflineDatabaseAndHometownTest {

    @Test
    public void testDivisionDistrictUpazilaEntities() {
        DivisionEntity division = new DivisionEntity("khulna", "Khulna", "খুলনা", "KHU", true, 1000L);
        assertEquals("khulna", division.getId());
        assertEquals("Khulna", division.getName());
        assertTrue(division.isActive());

        DistrictEntity district = new DistrictEntity("khulna", "Khulna", "খুলনা", "khulna", "KHU_DIST", true, 1000L);
        assertEquals("khulna", district.getId());
        assertEquals("khulna", district.getDivisionId());

        UpazilaEntity upazila = new UpazilaEntity("dumuria", "Dumuria", "ডুমুরিয়া", "khulna", "DUM", true, 1000L);
        assertEquals("dumuria", upazila.getId());
        assertEquals("khulna", upazila.getDistrictId());
    }

    @Test
    public void testShelterEntityToShelterMapping() {
        ShelterEntity entity = new ShelterEntity(
                "khulna_shelter_001",
                "Khulna Government Cyclone Shelter",
                "খুলনা সরকারি সাইক্লোন আশ্রয়কেন্দ্র",
                "High ground refuge",
                "উঁচু আশ্রয়কেন্দ্র",
                "khulna",
                "khulna",
                "dumuria",
                "Dumuria, Khulna",
                "ডুমুরিয়া, খুলনা",
                22.815,
                89.550,
                500,
                120,
                "Cyclone Shelter",
                Arrays.asList("Drinking Water", "First Aid", "Toilet"),
                "01711000001",
                true,
                1000L,
                2000L
        );

        Shelter shelter = ModelMapper.toShelter(entity);
        assertNotNull(shelter);
        assertEquals("Khulna Government Cyclone Shelter", shelter.getNameEn());
        assertEquals("khulna", shelter.getDistrict());
        assertEquals("dumuria", shelter.getUpazila());
        assertEquals(500, shelter.getCapacity());
        assertEquals(120, shelter.getCurrentOccupancy());
        assertEquals("01711000001", shelter.getContactPhone());
        assertTrue(shelter.isActive());
    }

    @Test
    public void testDisasterUpdateEntityToFloodWarningMapping() {
        DisasterUpdateEntity entity = new DisasterUpdateEntity(
                "alert_flood_001",
                "Flash Flood Advisory",
                "আকস্মিক বন্যা সতর্কতা",
                "Surma river rising rapidly",
                "সুরমা নদীর পানি বৃদ্ধি পাচ্ছে",
                "Flood",
                "Critical",
                "Active",
                "sylhet",
                "sylhet",
                "gowainghat",
                Arrays.asList("Gowainghat", "Companiganj"),
                "FFWC",
                1700000000000L,
                1700100000000L,
                1700000000000L,
                true
        );

        FloodWarning warning = ModelMapper.toFloodWarning(entity);
        assertNotNull(warning);
        assertEquals("Flash Flood Advisory", warning.getTitleEn());
        assertEquals("CRITICAL", warning.getWarningLevel());
        assertEquals("gowainghat, sylhet", warning.getRegion());
        assertTrue(warning.isActive());
    }

    @Test
    public void testUserProfileHometownPersistence() {
        UserProfile profile = new UserProfile(
                "uid_12345",
                "Rahim",
                "rahim@example.com",
                "01711223344",
                "khulna",
                "khulna",
                "dumuria",
                true,
                System.currentTimeMillis()
        );

        assertEquals("uid_12345", profile.getUid());
        assertEquals("Rahim", profile.getName());
        assertEquals("khulna", profile.getDivisionId());
        assertEquals("khulna", profile.getDistrictId());
        assertEquals("dumuria", profile.getUpazilaId());
        assertTrue(profile.isHometownConfigured());

        Map<String, Object> map = profile.toMap();
        assertEquals("Rahim", map.get("name"));
        assertEquals("khulna", map.get("divisionId"));
        assertEquals("khulna", map.get("districtId"));
        assertEquals("dumuria", map.get("upazilaId"));
        assertEquals(true, map.get("hometownConfigured"));

        UserProfileEntity entity = new UserProfileEntity(
                profile.getUid(),
                profile.getName(),
                profile.getEmail(),
                profile.getPhone(),
                profile.getDivisionId(),
                profile.getDistrictId(),
                profile.getUpazilaId(),
                profile.isHometownConfigured(),
                profile.getUpdatedAt()
        );

        UserProfile restored = ModelMapper.toUserProfile(entity);
        assertNotNull(restored);
        assertEquals(profile.getUid(), restored.getUid());
        assertEquals(profile.getDivisionId(), restored.getDivisionId());
        assertEquals(profile.getDistrictId(), restored.getDistrictId());
        assertEquals(profile.getUpazilaId(), restored.getUpazilaId());
        assertTrue(restored.isHometownConfigured());
    }

    @Test
    public void testStringListConverter() {
        List<String> original = Arrays.asList("Water", "First Aid", "Electricity");
        String json = StringListConverter.fromList(original);
        assertNotNull(json);

        List<String> restored = StringListConverter.toList(json);
        assertNotNull(restored);
        assertEquals(3, restored.size());
        assertEquals("Water", restored.get(0));
        assertEquals("First Aid", restored.get(1));
        assertEquals("Electricity", restored.get(2));

        // Test empty/null handling
        assertEquals("[]", StringListConverter.fromList(null));
        assertTrue(StringListConverter.toList("[]").isEmpty());
    }
}

