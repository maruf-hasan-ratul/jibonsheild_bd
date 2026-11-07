package com.jibonshield.bd.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.jibonshield.bd.utils.NetworkUtils;

/**
 * BroadcastReceiver monitoring system Airplane Mode transitions.
 * Ensures offline resilience: preserves local cache during flight mode and re-checks
 * actual network connectivity upon exit without assuming immediate reachability.
 */
public class AirplaneModeReceiver extends BroadcastReceiver {

    private static final String TAG = "AirplaneModeReceiver";

    public interface AirplaneModeListener {
        void onAirplaneModeChanged(boolean isAirplaneModeOn, boolean hasInternet);
    }

    private final AirplaneModeListener listener;

    public AirplaneModeReceiver() {
        this.listener = null;
    }

    public AirplaneModeReceiver(AirplaneModeListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(intent.getAction())) {
            return;
        }

        boolean isAirplaneModeOn = intent.getBooleanExtra("state", false);
        boolean hasInternet = NetworkUtils.isInternetAvailable(context);

        Log.i(TAG, "Airplane mode changed: isAirplaneModeOn=" + isAirplaneModeOn + ", hasInternet=" + hasInternet);

        if (listener != null) {
            listener.onAirplaneModeChanged(isAirplaneModeOn, hasInternet);
        }
    }

    /**
     * Registers this receiver with a lifecycle-safe IntentFilter.
     */
    public static AirplaneModeReceiver register(Context context, AirplaneModeListener listener) {
        AirplaneModeReceiver receiver = new AirplaneModeReceiver(listener);
        IntentFilter filter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        context.registerReceiver(receiver, filter);
        return receiver;
    }

    /**
     * Safely unregisters this receiver.
     */
    public static void unregister(Context context, AirplaneModeReceiver receiver) {
        if (context != null && receiver != null) {
            try {
                context.unregisterReceiver(receiver);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Receiver was not registered or already unregistered");
            }
        }
    }
}

