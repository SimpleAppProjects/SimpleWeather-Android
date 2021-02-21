package com.thewizrd.simpleweather.preferences;

import android.content.SharedPreferences;

import com.thewizrd.shared_resources.SimpleLibrary;

public class FeatureSettings {
    private static final SharedPreferences preferences;

    static {
        preferences = SimpleLibrary.getInstance().getApp().getPreferences();
    }

    private static final String KEY_FEATURE_BGIMAGE = "key_feature_bgimage";
    private static final String KEY_FEATURE_FORECAST = "key_feature_forecast";
    private static final String KEY_FEATURE_HRFORECAST = "key_feature_hrforecast";
    private static final String KEY_FEATURE_CHARTS = "key_feature_charts";
    private static final String KEY_FEATURE_DETAILS = "key_feature_details";
    private static final String KEY_FEATURE_UV = "key_feature_uv";
    private static final String KEY_FEATURE_BEAUFORT = "key_feature_beaufort";
    private static final String KEY_FEATURE_AQINDEX = "key_feature_aqindex";
    private static final String KEY_FEATURE_MOONPHASE = "key_feature_moonphase";
    private static final String KEY_FEATURE_SUNPHASE = "key_feature_sunphase";
    private static final String KEY_FEATURE_RADAR = "key_feature_radar";
    private static final String KEY_FEATURE_LOCPANELIMG = "key_feature_locpanelimg";

    public static boolean isBackgroundImageEnabled() {
        return preferences.getBoolean(KEY_FEATURE_BGIMAGE, true);
    }

    public static boolean isForecastEnabled() {
        return preferences.getBoolean(KEY_FEATURE_FORECAST, true);
    }

    public static boolean isHourlyForecastEnabled() {
        return preferences.getBoolean(KEY_FEATURE_HRFORECAST, true);
    }

    public static boolean isChartsEnabled() {
        return preferences.getBoolean(KEY_FEATURE_CHARTS, true);
    }

    public static boolean isDetailsEnabled() {
        return preferences.getBoolean(KEY_FEATURE_DETAILS, true);
    }

    public static boolean isUVEnabled() {
        return preferences.getBoolean(KEY_FEATURE_UV, true);
    }

    public static boolean isBeaufortEnabled() {
        return preferences.getBoolean(KEY_FEATURE_BEAUFORT, true);
    }

    public static boolean isAQIndexEnabled() {
        return preferences.getBoolean(KEY_FEATURE_AQINDEX, true);
    }

    public static boolean isMoonPhaseEnabled() {
        return preferences.getBoolean(KEY_FEATURE_MOONPHASE, true);
    }

    public static boolean isSunPhaseEnabled() {
        return preferences.getBoolean(KEY_FEATURE_SUNPHASE, true);
    }

    public static boolean isRadarEnabled() {
        return preferences.getBoolean(KEY_FEATURE_RADAR, true);
    }

    public static boolean isLocationPanelImageEnabled() {
        return preferences.getBoolean(KEY_FEATURE_LOCPANELIMG, true);
    }
}
