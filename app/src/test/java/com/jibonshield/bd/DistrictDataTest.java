package com.jibonshield.bd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.jibonshield.bd.utils.DistrictData;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Unit verification tests for complete coverage of all 64 districts
 * and 495 upazilas across Bangladesh in JibonShield BD.
 */
public class DistrictDataTest {

    @Test
    public void testAll64DistrictsPresentAndUnique() {
        assertEquals("Must have exactly 64 official districts", 64, DistrictData.ALL_64_DISTRICTS.length);

        Set<String> uniqueDistricts = new HashSet<>();
        for (String district : DistrictData.ALL_64_DISTRICTS) {
            assertNotNull(district);
            assertFalse(district.trim().isEmpty());
            assertTrue("Duplicate district detected: " + district, uniqueDistricts.add(district));
        }
    }

    @Test
    public void testEveryDistrictHasMappedUpazilas() {
        int totalUpazilas = 0;
        for (String district : DistrictData.ALL_64_DISTRICTS) {
            List<String> upazilas = DistrictData.getUpazilasForDistrict(district);
            assertNotNull("Upazila list for " + district + " must not be null", upazilas);
            assertFalse("Upazila list for " + district + " must not be empty", upazilas.isEmpty());
            totalUpazilas += upazilas.size();
        }

        assertTrue("Total upazilas must cover all ~495 sub-districts, found: " + totalUpazilas,
                totalUpazilas >= 490);
    }

    @Test
    public void testHelperVariants() {
        String[] withAll = DistrictData.getDistrictsWithAll();
        assertEquals(65, withAll.length);
        assertEquals("All", withAll[0]);

        String[] withAllDistricts = DistrictData.getDistrictsWithAllDistricts();
        assertEquals(65, withAllDistricts.length);
        assertEquals("All Districts", withAllDistricts[0]);

        String[] forRegistration = DistrictData.getDistrictsForRegistration();
        assertEquals(65, forRegistration.length);
        assertEquals("Other", forRegistration[forRegistration.length - 1]);
    }
}

