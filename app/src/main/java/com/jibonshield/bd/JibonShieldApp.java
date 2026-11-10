package com.jibonshield.bd;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.jibonshield.bd.sync.EmergencySyncWorker;
import com.jibonshield.bd.utils.LocaleHelper;

/**
 * Application class for JibonShield BD.
 * Initializes persistent configuration, Firebase cloud infrastructure,
 * WorkManager offline background synchronization, and locale management.
 */
public class JibonShieldApp extends Application {

    private static final String TAG = "JibonShieldApp";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LocaleHelper.onAttach(this);

        // Safe Firebase initialization
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }
        } catch (Exception e) {
            Log.w(TAG, "Firebase initialization warning: " + e.getMessage());
        }

        // Schedule WorkManager periodic synchronization
        try {
            EmergencySyncWorker.schedulePeriodicSync(this);
        } catch (Exception e) {
            Log.w(TAG, "WorkManager periodic scheduling note: " + e.getMessage());
        }
    }
}
