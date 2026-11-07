package com.jibonshield.bd.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

/**
 * BroadcastReceiver detecting system Battery Saver transitions.
 * When enabled, notifies the UI and background workers to conserve battery,
 * throttle non-essential network sync, and prioritize offline cached operations.
 */
public class BatterySaverReceiver extends BroadcastReceiver {

    private static final String TAG = "BatterySaverReceiver";

    public interface BatterySaverListener {
        void onBatterySaverChanged(boolean isPowerSaveMode);
    }

    private final BatterySaverListener listener;

    public BatterySaverReceiver() {
        this.listener = null;
    }

    public BatterySaverReceiver(BatterySaverListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        boolean isPowerSaveMode = isPowerSaveModeActive(context);
        Log.i(TAG, "Battery saver mode changed: isPowerSaveMode=" + isPowerSaveMode);

        if (listener != null) {
            listener.onBatterySaverChanged(isPowerSaveMode);
        }
    }

    /**
     * Checks if power save mode is currently enabled on the device.
     */
    public static boolean isPowerSaveModeActive(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isPowerSaveMode();
        }
        return false;
    }

    /**
     * Registers this receiver with a lifecycle-safe IntentFilter.
     */
    public static BatterySaverReceiver register(Context context, BatterySaverListener listener) {
        BatterySaverReceiver receiver = new BatterySaverReceiver(listener);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            IntentFilter filter = new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
            context.registerReceiver(receiver, filter);
        }
        return receiver;
    }

    /**
     * Safely unregisters this receiver.
     */
    public static void unregister(Context context, BatterySaverReceiver receiver) {
        if (context != null && receiver != null) {
            try {
                context.unregisterReceiver(receiver);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Receiver was not registered or already unregistered");
            }
        }
    }
}

