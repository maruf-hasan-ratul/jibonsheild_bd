package com.jibonshield.bd;

import android.app.Application;
import android.content.Context;

import com.jibonshield.bd.utils.LocaleHelper;

/**
 * Application class for JibonShield BD.
 * Initializes persistent configuration, crash resilience, and locale management.
 */
public class JibonShieldApp extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.onAttach(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LocaleHelper.onAttach(this);
    }
}
