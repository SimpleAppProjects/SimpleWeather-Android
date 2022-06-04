package com.thewizrd.weather_api.weatherdata

import com.thewizrd.shared_resources.BuildConfig
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherProvider
import com.thewizrd.weather_api.accuweather.weather.AccuWeatherProvider
import com.thewizrd.weather_api.here.weather.HEREWeatherProvider
import com.thewizrd.weather_api.meteofrance.weather.MeteoFranceProvider
import com.thewizrd.weather_api.meteomatics.weather.MeteomaticsWeatherProvider
import com.thewizrd.weather_api.metno.MetnoWeatherProvider
import com.thewizrd.weather_api.nws.NWSWeatherProvider
import com.thewizrd.weather_api.openweather.location.OpenWeatherMapLocationProvider
import com.thewizrd.weather_api.openweather.weather.OpenWeatherMapProvider
import com.thewizrd.weather_api.openweather.weather.onecall.OWMOneCallWeatherProvider
import com.thewizrd.weather_api.tomorrow.TomorrowIOWeatherProvider
import com.thewizrd.weather_api.weatherapi.weather.WeatherApiProvider
import com.thewizrd.weather_api.weatherbit.WeatherBitIOProvider
import com.thewizrd.weather_api.weatherunlocked.WeatherUnlockedProvider

class WeatherProviderFactoryImpl : WeatherProviderFactory {
    override fun getWeatherProvider(provider: String?): WeatherProvider {
        return when (provider) {
            WeatherAPI.HERE -> HEREWeatherProvider()
            WeatherAPI.OPENWEATHERMAP -> {
                val settingsMgr = appLib.settingsManager

                when {
                    BuildConfig.IS_NONGMS -> {
                        OWMOneCallWeatherProvider(OpenWeatherMapLocationProvider())
                    }
                    settingsMgr.usePersonalKey() -> {
                        OWMOneCallWeatherProvider()
                    }
                    else -> {
                        OpenWeatherMapProvider()
                    }
                }
            }
            WeatherAPI.METNO -> MetnoWeatherProvider()
            WeatherAPI.NWS -> NWSWeatherProvider()
            WeatherAPI.WEATHERAPI -> WeatherApiProvider()
            WeatherAPI.WEATHERUNLOCKED -> WeatherUnlockedProvider()
            WeatherAPI.METEOFRANCE -> MeteoFranceProvider()
            WeatherAPI.TOMORROWIO -> TomorrowIOWeatherProvider()
            WeatherAPI.ACCUWEATHER -> AccuWeatherProvider()
            WeatherAPI.WEATHERBITIO -> WeatherBitIOProvider()
            WeatherAPI.METEOMATICS -> MeteomaticsWeatherProvider()
            else -> {
                if (!BuildConfig.DEBUG) {
                    if (!BuildConfig.IS_NONGMS)
                        WeatherApiProvider()
                    else
                        MetnoWeatherProvider()
                } else {
                    throw IllegalArgumentException("Weather provider not supported ($provider)")
                }
            }
        }
    }
}