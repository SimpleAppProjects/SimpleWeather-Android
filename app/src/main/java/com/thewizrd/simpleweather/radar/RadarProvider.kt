package com.thewizrd.simpleweather.radar

import android.content.Context
import android.os.Build
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.annotation.StringDef
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.controls.ProviderEntry
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.simpleweather.radar.nullschool.EarthWindMapViewProvider
import com.thewizrd.simpleweather.radar.openweather.OWMRadarViewProvider
import com.thewizrd.simpleweather.radar.rainviewer.RainViewerViewProvider
import com.thewizrd.weather_api.weatherModule

object RadarProvider {
    const val KEY_RADARPROVIDER = "key_radarprovider"
    const val EARTHWINDMAP = "nullschool"
    const val RAINVIEWER = "rainviewer"
    const val OPENWEATHERMAP = "openweather"

    @StringDef(EARTHWINDMAP, RAINVIEWER, OPENWEATHERMAP)
    @Retention(AnnotationRetention.SOURCE)
    annotation class RadarProviders

    fun getRadarProviders(): List<ProviderEntry> {
        val owm = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.OPENWEATHERMAP)
        return if (settingsManager.getAPI() != owm.getWeatherAPI() && owm.getAPIKey() == null) {
            FullRadarProviders.filterNot { it.value == WeatherAPI.OPENWEATHERMAP }
        } else {
            FullRadarProviders
        }
    }

    private val FullRadarProviders = listOf(
        ProviderEntry(
            "EarthWindMap Project", EARTHWINDMAP,
            "https://earth.nullschool.net/", "https://earth.nullschool.net/"
        ),
        ProviderEntry(
            "RainViewer", RAINVIEWER,
            "https://www.rainviewer.com/", "https://www.rainviewer.com/api.html"
        ),
        ProviderEntry(
            "OpenWeatherMap", OPENWEATHERMAP,
            "http://www.openweathermap.org", "https://home.openweathermap.org/users/sign_up"
        )
    )

    @JvmStatic
    @RadarProviders
    fun getRadarProvider(): String {
        val prefs = appLib.preferences
        val provider = prefs.getString(KEY_RADARPROVIDER, EARTHWINDMAP)!!

        if (provider == WeatherAPI.OPENWEATHERMAP) {
            val owm = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.OPENWEATHERMAP)
            // Fallback to default since API KEY is unavailable
            if ((settingsManager.getAPI() != owm.getWeatherAPI() && owm.getAPIKey() == null) || settingsManager.getAPIKey(
                    WeatherAPI.OPENWEATHERMAP
                ) == null
            ) {
                return EARTHWINDMAP
            }
        }

        return provider
    }

    @JvmStatic
    @RequiresApi(value = Build.VERSION_CODES.LOLLIPOP)
    fun getRadarViewProvider(context: Context, rootView: ViewGroup): RadarViewProvider {
        return if (getRadarProvider() == RAINVIEWER) {
            RainViewerViewProvider(context, rootView)
        } else if (getRadarProvider() == OPENWEATHERMAP) {
            OWMRadarViewProvider(context, rootView)
        } else {
            EarthWindMapViewProvider(context, rootView)
        }
    }
}