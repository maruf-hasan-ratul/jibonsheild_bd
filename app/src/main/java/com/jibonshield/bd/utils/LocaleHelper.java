package com.jibonshield.bd.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

/**
 * Helper class for managing runtime locale switching across Android versions (API 21 to 34).
 * Ensures seamless dynamic toggling between Bangla (বাংলা) and English.
 */
public class LocaleHelper {

    public static Context onAttach(Context context) {
        String lang = PreferenceUtils.getLanguage(context);
        return setLocale(context, lang);
    }

    public static Context setLocale(Context context, String language) {
        PreferenceUtils.setLanguage(context, language);
        return updateResources(context, language);
    }

    @SuppressWarnings("deprecation")
    public static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language != null ? language : PreferenceUtils.LANG_BN);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            configuration.setLocales(localeList);
            return context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
    }
}
