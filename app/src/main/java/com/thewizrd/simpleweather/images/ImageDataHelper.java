package com.thewizrd.simpleweather.images;

import android.content.SharedPreferences;

import com.thewizrd.shared_resources.SimpleLibrary;

public class ImageDataHelper {
    private static ImageDataHelperImpl sImageDataHelper;
    private static final SharedPreferences preferences;

    static {
        preferences = SimpleLibrary.getInstance().getApp().getPreferences();
    }

    public static ImageDataHelperImpl getImageDataHelper() {
        if (sImageDataHelper == null)
            sImageDataHelper = new ImageDataHelperImplApp();

        return sImageDataHelper;
    }

    public static long getImageDBUpdateTime() {
        return Long.parseLong(preferences.getString("ImageDB_LastUpdated", "0"));
    }

    public static void setImageDBUpdateTime(long value) {
        preferences.edit().putString("ImageDB_LastUpdated", Long.toString(value))
                .apply();
    }

    public static boolean shouldInvalidateCache() {
        return preferences.getBoolean("ImageDB_Invalidate", false);
    }

    public static void invalidateCache(boolean value) {
        preferences.edit().putBoolean("ImageDB_Invalidate", value).apply();
    }
}