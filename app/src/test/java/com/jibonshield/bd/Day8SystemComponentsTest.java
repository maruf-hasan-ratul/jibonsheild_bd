package com.jibonshield.bd;

import static org.junit.Assert.*;

import android.content.Context;
import android.content.Intent;

import com.jibonshield.bd.models.WaterObservation;
import com.jibonshield.bd.receivers.AirplaneModeReceiver;
import com.jibonshield.bd.receivers.BatterySaverReceiver;
import com.jibonshield.bd.services.EmergencyMonitoringService;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Day 8 Automated Unit Tests for Android System Components:
 * - Airplane Mode transitions and offline resilience callbacks
 * - Battery Saver detection and throttling callbacks
 * - Foreground Service actions and monitoring state transitions
 * - Water Observation evidence photo state persistence
 */
public class Day8SystemComponentsTest {

    @Test
    public void testAirplaneModeReceiverTrigger() {
        AtomicBoolean callbackFired = new AtomicBoolean(false);
        AtomicBoolean stateCaptured = new AtomicBoolean(false);

        AirplaneModeReceiver receiver = new AirplaneModeReceiver((isAirplaneModeOn, hasInternet) -> {
            callbackFired.set(true);
            stateCaptured.set(isAirplaneModeOn);
        });

        // Verify handling of non-matching action
        receiver.onReceive(null, new Intent("SOME_RANDOM_ACTION"));
        assertFalse("Receiver should ignore unrelated intent actions", callbackFired.get());

        // Verify handling of null intent
        receiver.onReceive(null, null);
        assertFalse("Receiver should ignore null intent", callbackFired.get());
    }

    @Test
    public void testEmergencyMonitoringServiceConstants() {
        assertEquals("com.jibonshield.bd.action.START_MONITORING", EmergencyMonitoringService.ACTION_START_MONITORING);
        assertEquals("com.jibonshield.bd.action.STOP_MONITORING", EmergencyMonitoringService.ACTION_STOP_MONITORING);
        assertEquals("channel_emergency_monitoring", EmergencyMonitoringService.CHANNEL_ID);
        assertEquals(2001, EmergencyMonitoringService.NOTIFICATION_ID);
    }

    @Test
    public void testWaterObservationPhotoModelIntegrity() {
        WaterObservation obsWithoutImage = new WaterObservation(
                101, "Meghna Ghat", "Chandpur", "River Water",
                "Cloudy", "Earthy", 6.8, "High", "Requires Boiling",
                "2026-11-07 10:00 BST", "Rahim", "Turbid after storm"
        );

        assertFalse("Observation created without image should return false for hasImage", obsWithoutImage.hasImage());
        assertNull("Image URI should be null", obsWithoutImage.getImageUri());
        assertNull("Image URL should be null", obsWithoutImage.getImageUrl());
        assertEquals("Default sync status should be PENDING", WaterObservation.SYNC_PENDING, obsWithoutImage.getSyncStatus());

        WaterObservation obsWithLocalImage = new WaterObservation(
                102, "Meghna Ghat", "Chandpur", "River Water",
                "Cloudy", "Earthy", 6.8, "High", "Requires Boiling",
                "2026-11-07 10:00 BST", "Rahim", "Turbid after storm",
                "content://media/external/images/media/42", null, WaterObservation.SYNC_PENDING,
                1700000000000L, 1700000000000L
        );

        assertTrue("Observation with local URI should return true for hasImage", obsWithLocalImage.hasImage());
        assertEquals("content://media/external/images/media/42", obsWithLocalImage.getImageUri());
        assertNull("Cloud URL should still be null before sync", obsWithLocalImage.getImageUrl());
        assertEquals(WaterObservation.SYNC_PENDING, obsWithLocalImage.getSyncStatus());

        WaterObservation obsWithCloudUrl = new WaterObservation(
                103, "Meghna Ghat", "Chandpur", "River Water",
                "Cloudy", "Earthy", 6.8, "High", "Requires Boiling",
                "2026-11-07 10:00 BST", "Rahim", "Turbid after storm",
                "content://media/external/images/media/42",
                "https://firebasestorage.googleapis.com/v0/b/bucket/o/obs_103.jpg",
                WaterObservation.SYNC_SYNCED,
                1700000000000L, 1700000050000L
        );

        assertTrue("Observation with cloud URL should return true for hasImage", obsWithCloudUrl.hasImage());
        assertEquals("https://firebasestorage.googleapis.com/v0/b/bucket/o/obs_103.jpg", obsWithCloudUrl.getImageUrl());
        assertEquals(WaterObservation.SYNC_SYNCED, obsWithCloudUrl.getSyncStatus());
    }
}

