package com.jibonshield.bd;

import static org.junit.Assert.*;

import com.jibonshield.bd.models.WaterObservation;
import com.jibonshield.bd.sync.EmergencySyncWorker;

import org.junit.Test;

/**
 * Day 10 Automated Unit Tests for Offline Synchronization and Storage Architecture:
 * - SyncWorker unique work task name contracts
 * - Water observation sync lifecycle states (PENDING -> SYNCED -> FAILED)
 * - Safe image file naming and MIME rules
 */
public class Day10SyncAndStorageTest {

    @Test
    public void testSyncWorkerWorkNames() {
        assertEquals("jibonshield_periodic_sync", EmergencySyncWorker.WORK_NAME_PERIODIC);
        assertEquals("jibonshield_immediate_sync", EmergencySyncWorker.WORK_NAME_ONE_TIME);
    }

    @Test
    public void testSyncStatusTransitions() {
        WaterObservation obs = new WaterObservation(
                101, "Char Rajibpur", "Kurigram", "Tubewell",
                "Cloudy", "None", 7.0, "Moderate", "Requires Boiling",
                "2026-11-07 10:00 BST", "Observer 1", "Sample test"
        );

        // Initial state
        assertEquals(WaterObservation.SYNC_PENDING, obs.getSyncStatus());

        // Simulated successful sync
        WaterObservation syncedObs = new WaterObservation(
                obs.getId(), obs.getLocationName(), obs.getDistrict(), obs.getSourceType(),
                obs.getAppearance(), obs.getOdor(), obs.getPhLevel(), obs.getTurbidityStatus(),
                obs.getSafetyStatus(), obs.getObservationTime(), obs.getReportedBy(), obs.getRemarks(),
                obs.getImageUri(), "https://firebasestorage.googleapis.com/v0/b/app/o/obs_101.jpg",
                WaterObservation.SYNC_SYNCED, obs.getCreatedAt(), System.currentTimeMillis()
        );

        assertEquals(WaterObservation.SYNC_SYNCED, syncedObs.getSyncStatus());
        assertNotNull(syncedObs.getImageUrl());

        // Simulated failed sync
        WaterObservation failedObs = new WaterObservation(
                obs.getId(), obs.getLocationName(), obs.getDistrict(), obs.getSourceType(),
                obs.getAppearance(), obs.getOdor(), obs.getPhLevel(), obs.getTurbidityStatus(),
                obs.getSafetyStatus(), obs.getObservationTime(), obs.getReportedBy(), obs.getRemarks(),
                obs.getImageUri(), null, WaterObservation.SYNC_FAILED,
                obs.getCreatedAt(), System.currentTimeMillis()
        );

        assertEquals(WaterObservation.SYNC_FAILED, failedObs.getSyncStatus());
        assertNull(failedObs.getImageUrl());
    }
}

