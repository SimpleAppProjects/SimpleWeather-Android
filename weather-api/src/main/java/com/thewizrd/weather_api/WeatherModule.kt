package com.thewizrd.weather_api

import android.location.Geocoder
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.weather_api.locationdata.WeatherLocationProviderFactory
import com.thewizrd.weather_api.locationdata.WeatherLocationProviderFactoryImpl
import com.thewizrd.weather_api.stag.generated.Stag
import com.thewizrd.weather_api.tzdb.TZDBService
import com.thewizrd.weather_api.tzdb.TZDBServiceImpl
import com.thewizrd.weather_api.tzdb.TimeZoneProvider
import com.thewizrd.weather_api.tzdb.TimeZoneProviderImpl
import com.thewizrd.weather_api.weatherdata.WeatherProviderFactory
import com.thewizrd.weather_api.weatherdata.WeatherProviderFactoryImpl
import com.thewizrd.weather_api.weatherdata.WeatherProviderManager

val weatherModule: WeatherModule by lazy {
    WeatherModule().apply {
        init()
    }
}

class WeatherModule internal constructor() {
    /**
     * Manages Weather providers
     */
    val weatherManager by lazy { WeatherProviderManager() }
    val weatherProviderFactory: WeatherProviderFactory by lazy { WeatherProviderFactoryImpl() }
    val locationProviderFactory: WeatherLocationProviderFactory by lazy { WeatherLocationProviderFactoryImpl() }

    val geocoder: Geocoder
        get() = Geocoder(sharedDeps.context, LocaleUtils.getLocale())

    /* Misc services */
    val tzProvider: TimeZoneProvider by lazy { TimeZoneProviderImpl() }
    val tzdbService: TZDBService by lazy { TZDBServiceImpl() }

    internal fun init() {
        JSONParser.registerTypeAdapterFactory(Stag.Factory())
    }
}