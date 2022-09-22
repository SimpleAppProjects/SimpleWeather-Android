package com.thewizrd.weather_api.utils

import android.os.Bundle
import android.util.Log
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.StringUtils.escapeUnicode
import com.thewizrd.weather_api.BuildConfig
import com.thewizrd.weather_api.weatherdata.WeatherProviderImpl

internal fun WeatherProviderImpl.logMissingIcon(icon: String?) {
    val encoded = icon?.escapeUnicode()

    AnalyticsLogger.logEvent("W_UnknownIcon", Bundle().apply {
        putString("provider", getWeatherAPI())
        putString("icon", icon)
        putString("encoded", encoded)
    })
    if (BuildConfig.DEBUG) {
        Logger.writeLine(
            Log.INFO,
            "WeatherProvider: Unknown Icon provided - icon ($icon), encoded ($encoded), provider (${getWeatherAPI()})"
        )
    }
}