package com.jibonshield.bd.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility helper for managing persistent user preferences such as selected location,
 * hometown hierarchy (Division, District, Upazila), language, and theme.
 */
public final class PreferenceUtils {

    private static final String PREF_NAME = "jibonshield_prefs";
    private static final String KEY_SELECTED_DISTRICT = "selected_district";
    private static final String KEY_LANGUAGE = "selected_language";
    private static final String KEY_THEME = "app_theme_mode";

    // Hometown Configuration Keys
    private static final String KEY_HOMETOWN_DIVISION = "hometown_division_id";
    private static final String KEY_HOMETOWN_DISTRICT = "hometown_district_id";
    private static final String KEY_HOMETOWN_UPAZILA = "hometown_upazila_id";
    private static final String KEY_HOMETOWN_CONFIGURED = "hometown_configured";

    public static final String DEFAULT_DISTRICT = "All";
    public static final String LANG_BN = "bn";
    public static final String LANG_EN = "en";

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";
    public static final String THEME_AMOLED = "amoled";

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

    public static String getThemeMode(Context context) {
        return getPrefs(context).getString(KEY_THEME, THEME_SYSTEM);
    }

    public static void setThemeMode(Context context, String themeMode) {
        getPrefs(context).edit().putString(KEY_THEME, themeMode).apply();
    }

    public static boolean isAmoledTheme(Context context) {
        return THEME_AMOLED.equalsIgnoreCase(getThemeMode(context));
    }

    // =========================================================================
    // HOMETOWN MANAGEMENT
    // =========================================================================

    public static boolean isHometownConfigured(Context context) {
        return getPrefs(context).getBoolean(KEY_HOMETOWN_CONFIGURED, false);
    }

    public static String getHometownDivision(Context context) {
        return getPrefs(context).getString(KEY_HOMETOWN_DIVISION, "");
    }

    public static String getHometownDistrict(Context context) {
        return getPrefs(context).getString(KEY_HOMETOWN_DISTRICT, "");
    }

    public static String getHometownUpazila(Context context) {
        return getPrefs(context).getString(KEY_HOMETOWN_UPAZILA, "");
    }

    public static void setHometown(Context context, String divisionId, String districtId, String upazilaId) {
        getPrefs(context).edit()
                .putString(KEY_HOMETOWN_DIVISION, divisionId != null ? divisionId.toLowerCase().trim() : "")
                .putString(KEY_HOMETOWN_DISTRICT, districtId != null ? districtId.toLowerCase().trim() : "")
                .putString(KEY_HOMETOWN_UPAZILA, upazilaId != null ? upazilaId.toLowerCase().trim() : "")
                .putBoolean(KEY_HOMETOWN_CONFIGURED, districtId != null && !districtId.trim().isEmpty())
                .apply();

        if (districtId != null && !districtId.trim().isEmpty()) {
            setSelectedDistrict(context, districtId.trim());
        }
    }

    public static void clearHometown(Context context) {
        getPrefs(context).edit()
                .remove(KEY_HOMETOWN_DIVISION)
                .remove(KEY_HOMETOWN_DISTRICT)
                .remove(KEY_HOMETOWN_UPAZILA)
                .putBoolean(KEY_HOMETOWN_CONFIGURED, false)
                .apply();
    }
}
