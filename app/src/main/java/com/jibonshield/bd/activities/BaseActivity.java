package com.jibonshield.bd.activities;

import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.jibonshield.bd.R;
import com.jibonshield.bd.utils.LocaleHelper;
import com.jibonshield.bd.utils.PreferenceUtils;

/**
 * Base activity class for JibonShield BD.
 * Injects selected user locale (Bangla/English) into the activity base context
 * and applies the Material 3 theme (Light, Dark, Low-power AMOLED).
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyAppTheme();
        super.onCreate(savedInstanceState);
    }

    protected void applyAppTheme() {
        String mode = PreferenceUtils.getThemeMode(this);
        if (PreferenceUtils.THEME_AMOLED.equalsIgnoreCase(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            setTheme(R.style.Theme_JibonShieldBD_Amoled);
        } else if (PreferenceUtils.THEME_DARK.equalsIgnoreCase(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            setTheme(R.style.Theme_JibonShieldBD);
        } else if (PreferenceUtils.THEME_LIGHT.equalsIgnoreCase(mode)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            setTheme(R.style.Theme_JibonShieldBD);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            setTheme(R.style.Theme_JibonShieldBD);
        }
    }
}
