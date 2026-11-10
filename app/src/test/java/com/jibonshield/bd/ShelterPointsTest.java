package com.jibonshield.bd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.jibonshield.bd.models.Shelter;
import com.jibonshield.bd.repositories.EmergencyRepository;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Unit verification tests for disaster shelter points across Bangladesh.
 */
public class ShelterPointsTest {

    @Test
    public void testComprehensiveShelterPointsCoverage() throws Exception {
        EmergencyRepository repo = new EmergencyRepository();
        Method method = EmergencyRepository.class.getDeclaredMethod("getSeedShelters");
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Shelter> shelters = (List<Shelter>) method.invoke(repo);

        assertNotNull(shelters);
        assertTrue("Must have at least 40 comprehensive shelter points, found: " + shelters.size(),
                shelters.size() >= 40);

        Set<String> coveredDistricts = new HashSet<>();
        for (Shelter s : shelters) {
            assertNotNull(s.getNameEn());
            assertNotNull(s.getNameBn());
            assertNotNull(s.getDistrict());
            assertNotNull(s.getUpazila());
            assertTrue("Capacity must be positive", s.getCapacity() > 0);
            assertNotNull("Phone must not be null", s.getContactPhone());
            assertFalse("Phone must not be empty", s.getContactPhone().trim().isEmpty());
            coveredDistricts.add(s.getDistrict());
        }

        // Verify key high-risk zones are covered
        assertTrue("Must cover Cox's Bazar", coveredDistricts.contains("Cox's Bazar"));
        assertTrue("Must cover Chittagong", coveredDistricts.contains("Chittagong"));
        assertTrue("Must cover Sunamganj", coveredDistricts.contains("Sunamganj"));
        assertTrue("Must cover Sylhet", coveredDistricts.contains("Sylhet"));
        assertTrue("Must cover Kurigram", coveredDistricts.contains("Kurigram"));
        assertTrue("Must cover Patuakhali", coveredDistricts.contains("Patuakhali"));
        assertTrue("Must cover Bhola", coveredDistricts.contains("Bhola"));
        assertTrue("Must cover Bagerhat", coveredDistricts.contains("Bagerhat"));
        assertTrue("Must cover Satkhira", coveredDistricts.contains("Satkhira"));
        assertTrue("Must cover Sirajganj", coveredDistricts.contains("Sirajganj"));
    }

    @Test
    public void testFilterSheltersAllDistrictsNotIgnored() throws Exception {
        EmergencyRepository repo = new EmergencyRepository();
        Method getSeedsMethod = EmergencyRepository.class.getDeclaredMethod("getSeedShelters");
        getSeedsMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Shelter> seeds = (List<Shelter>) getSeedsMethod.invoke(repo);

        Method filterMethod = EmergencyRepository.class.getDeclaredMethod("filterShelters",
                List.class, String.class, String.class, String.class);
        filterMethod.setAccessible(true);

        // When "All Districts" is passed, it should return all shelters
        @SuppressWarnings("unchecked")
        List<Shelter> resultAllDistricts = (List<Shelter>) filterMethod.invoke(repo, seeds, "", "All Districts", "All");
        assertEquals("Filter with 'All Districts' must return all seed shelters",
                seeds.size(), resultAllDistricts.size());

        // When specific district is passed
        @SuppressWarnings("unchecked")
        List<Shelter> resultCox = (List<Shelter>) filterMethod.invoke(repo, seeds, "", "Cox's Bazar", "All");
        assertTrue("Must return Cox's Bazar shelters", resultCox.size() >= 5);
        for (Shelter s : resultCox) {
            assertEquals("Cox's Bazar", s.getDistrict());
        }
    }
}

