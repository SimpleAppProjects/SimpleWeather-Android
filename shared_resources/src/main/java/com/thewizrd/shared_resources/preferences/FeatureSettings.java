package com.thewizrd.shared_resources.preferences;

import android.content.SharedPreferences;

import com.thewizrd.shared_resources.SimpleLibrary;

public class FeatureSettings {
    private static final SharedPreferences preferences;

    static {
        preferences = SimpleLibrary.getInstance().getApp().getPreferences();
    }

    private static final String KEY_UPDATEAVAILABLE = "key_updateavailable";

    public static boolean isUpdateAvailable() {
        return preferences.getBoolean(KEY_UPDATEAVAILABLE, false);
    }

    public static void setUpdateAvailable(boolean value) {
        preferences.edit().putBoolean(KEY_UPDATEAVAILABLE, value).apply();
    }
}
