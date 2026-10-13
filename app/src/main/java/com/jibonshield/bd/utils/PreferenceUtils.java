package com.jibonshield.bd.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility helper for managing persistent user preferences such as selected location and language.
 */
public final class PreferenceUtils {

    private static final String PREF_NAME = "jibonshield_prefs";
    private static final String KEY_SELECTED_DISTRICT = "selected_district";
    private static final String KEY_LANGUAGE = "selected_language";

    public static final String DEFAULT_DISTRICT = "All";
    public static final String LANG_BN = "bn";
    public static final String LANG_EN = "en";

    private PreferenceUtils() {}

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static String getSelectedDistrict(Context context) {
        return getPrefs(context).getString(KEY_SELECTED_DISTRICT, DEFAULT_DISTRICT);
    }

    public static void setSelectedDistrict(Context context, String district) {
        getPrefs(context).edit().putString(KEY_SELECTED_DISTRICT, district).apply();
    }

    public static String getLanguage(Context context) {
        return getPrefs(context).getString(KEY_LANGUAGE, LANG_BN);
    }

    public static void setLanguage(Context context, String langCode) {
        getPrefs(context).edit().putString(KEY_LANGUAGE, langCode).apply();
    }
}
