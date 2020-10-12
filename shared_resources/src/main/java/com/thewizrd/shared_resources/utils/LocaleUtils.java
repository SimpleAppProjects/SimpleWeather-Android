package com.thewizrd.shared_resources.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.thewizrd.shared_resources.SimpleLibrary;

import java.util.Locale;

public class LocaleUtils {
    private static final String KEY_LANGUAGE = "key_language";
    private static Locale sLocale;

    public static Context attachBaseContext(@NonNull Context context) {
        Configuration oldConfig = context.getResources().getConfiguration();
        Configuration newConfig = new Configuration(oldConfig);

        Locale locale = getLocale();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            newConfig.setLocale(locale);
        } else {
            newConfig.locale = locale;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return context.createConfigurationContext(newConfig);
        } else {
            context.getResources().updateConfiguration(newConfig, context.getResources().getDisplayMetrics());
            return context;
        }
    }

    public static String getLocaleCode() {
        return SimpleLibrary.getInstance().getApp().getPreferences().getString(KEY_LANGUAGE, "");
    }

    public static void setLocaleCode(@Nullable String localeCode) {
        SimpleLibrary.getInstance().getApp().getPreferences().edit().putString(KEY_LANGUAGE, localeCode).apply();
        updateLocale(localeCode);
    }

    @NonNull
    public static Locale getLocale() {
        if (sLocale == null) {
            updateLocale(getLocaleCode());
        }

        return sLocale;
    }

    private static void updateLocale(@Nullable String localeCode) {
        sLocale = getLocaleForCode(localeCode);
    }

    @NonNull
    public static String getLocaleDisplayName() {
        return getLocale().getDisplayName(getLocale());
    }

    private static Locale getLocaleForCode(@Nullable String localeCode) {
        if (!StringUtils.isNullOrWhitespace(localeCode)) {
            return new Locale(localeCode);
        } else {
            return Locale.getDefault();
        }
    }
}
