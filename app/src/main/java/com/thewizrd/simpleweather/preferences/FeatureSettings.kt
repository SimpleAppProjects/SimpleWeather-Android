package com.thewizrd.simpleweather.preferences

import com.thewizrd.shared_resources.appLib
import com.thewizrd.simpleweather.BuildConfig

object FeatureSettings {
    private val preferences = appLib.preferences

    const val KEY_FEATURE_BGIMAGE = "key_feature_bgimage"
    private const val KEY_FEATURE_FORECAST = "key_feature_forecast"
    private const val KEY_FEATURE_HRFORECAST = "key_feature_hrforecast"
    private const val KEY_FEATURE_CHARTS = "key_feature_charts"
    private const val KEY_FEATURE_SUMMARY = "key_feature_summary"
    private const val KEY_FEATURE_DETAILS = "key_feature_details"
    private const val KEY_FEATURE_UV = "key_feature_uv"
    private const val KEY_FEATURE_BEAUFORT = "key_feature_beaufort"
    private const val KEY_FEATURE_AQINDEX = "key_feature_aqindex"
    private const val KEY_FEATURE_MOONPHASE = "key_feature_moonphase"
    private const val KEY_FEATURE_SUNPHASE = "key_feature_sunphase"
    private const val KEY_FEATURE_RADAR = "key_feature_radar"
    private const val KEY_FEATURE_LOCPANELIMG = "key_feature_locpanelimg"
    private const val KEY_FEATURE_POLLEN = "key_feature_pollen"

    @JvmStatic
    val isBackgroundImageEnabled: Boolean
        get() = if (BuildConfig.IS_NONGMS) {
            false
        } else {
            preferences.getBoolean(
                KEY_FEATURE_BGIMAGE,
                true
            )
        }

    @JvmStatic
    val isForecastEnabled: Boolean
        get() = preferences.getBoolean(KEY_FEATURE_FORECAST, true)

    @JvmStatic
    val isHourlyForecastEnabled: Boolean
        get() = preferences.getBoolean(KEY_FEATURE_HRFORECAST, true)

    @JvmStatic
    val isChartsEnabled: Boolean
        get() = preferences.getBoolean(KEY_FEATURE_CHARTS, true)

    @JvmStatic
    val isSummaryEnabled: Boolean
        get() = preferences.getBoolean(KEY_FEATURE_SUMMARY, true)

    @JvmStatic
    val isDetailsEnabled: Boolean
        get() = preferences.getBoolean(KEY_FEATURE_DETAILS, true)

    @JvmStatic
    val isUVEnabled: Boolean
        get() = preferences.getBoolean(KEY_FEATURE_UV, true)

    @JvmStatic
    val isBeaufortEnabled: Boolean
        get() = preferences.getBoolean(KEY_FEATURE_BEAUFORT, true)

    @JvmStatic
    val isAQIndexEnabled: Boolean
        get() = preferences.getBoolean(KEY_FEATURE_AQINDEX, true)

    @JvmStatic
    val isMoonPhaseEnabled: Boolean
        get() = preferences.getBoolean(KEY_FEATURE_MOONPHASE, true)

    @JvmStatic
    val isSunPhaseEnabled: Boolean
        get() = preferences.getBoolean(KEY_FEATURE_SUNPHASE, true)

    @JvmStatic
    val isRadarEnabled: Boolean
        get() = preferences.getBoolean(KEY_FEATURE_RADAR, true)

    @JvmStatic
    val isLocationPanelImageEnabled: Boolean
        get() = preferences.getBoolean(KEY_FEATURE_LOCPANELIMG, true)

    @JvmStatic
    val isPollenEnabled: Boolean
        get() = preferences.getBoolean(KEY_FEATURE_POLLEN, true)
}