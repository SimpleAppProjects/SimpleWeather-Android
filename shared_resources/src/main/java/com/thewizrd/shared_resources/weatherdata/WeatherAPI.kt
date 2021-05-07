package com.thewizrd.shared_resources.weatherdata

import androidx.annotation.StringDef
import com.thewizrd.shared_resources.BuildConfig
import com.thewizrd.shared_resources.controls.ProviderEntry
import java.lang.annotation.RetentionPolicy
import java.util.*

object WeatherAPI {
    // APIs
    const val YAHOO = "Yahoo"
    const val WEATHERUNDERGROUND = "WUnderground"
    const val OPENWEATHERMAP = "openweather"
    const val METNO = "Metno"
    const val HERE = "Here"
    const val NWS = "NWS"
    const val WEATHERUNLOCKED = "wunlocked"

    // Location APIs
    const val LOCATIONIQ = "LocIQ"
    const val GOOGLE = "google"
    const val WEATHERAPI = "weatherapi"

    @StringDef(HERE, YAHOO, METNO, NWS, OPENWEATHERMAP, WEATHERUNLOCKED)
    @Retention(AnnotationRetention.SOURCE)
    annotation class WeatherProviders

    @StringDef(HERE, LOCATIONIQ, GOOGLE, WEATHERAPI)
    @Retention(AnnotationRetention.SOURCE)
    annotation class LocationProviders

    val APIs: List<ProviderEntry>
        get() {
            return if (BuildConfig.IS_NONGMS) {
                NonGMSAPIs
            } else {
                GMSFullAPIs
            }
        }

    val LocationAPIs: List<ProviderEntry>
        get() {
            return if (BuildConfig.IS_NONGMS) {
                NonGMSLocationAPIs
            } else {
                GMSFullLocationAPIs
            }
        }

    private val GMSFullAPIs = listOf(
            ProviderEntry("HERE Weather", HERE,
                    "https://www.here.com/en", "https://developer.here.com/?create=Freemium-Basic&keepState=true&step=account"),
            ProviderEntry("MET Norway", METNO,
                    "https://www.met.no/en", "https://www.met.no/en"),
            ProviderEntry("National Weather Service (United States)", NWS,
                    "https://www.weather.gov", "https://www.weather.gov"),
            ProviderEntry("OpenWeatherMap", OPENWEATHERMAP,
                    "http://www.openweathermap.org", "https://home.openweathermap.org/users/sign_up"),
            ProviderEntry("WeatherUnlocked", WEATHERUNLOCKED,
                    "https://developer.weatherunlocked.com/", "https://developer.weatherunlocked.com/")
    )

    private val NonGMSAPIs = listOf(
            ProviderEntry("MET Norway", METNO,
                    "https://www.met.no/en", "https://www.met.no/en"),
            ProviderEntry("National Weather Service (United States)", NWS,
                    "https://www.weather.gov", "https://www.weather.gov"),
            ProviderEntry("OpenWeatherMap", OPENWEATHERMAP,
                    "http://www.openweathermap.org", "https://home.openweathermap.org/users/sign_up")
    )

    private val GMSFullLocationAPIs = listOf(
            ProviderEntry("LocationIQ", LOCATIONIQ,
                    "https://locationiq.com", "https://locationiq.com"),
            ProviderEntry("Google", GOOGLE,
                    "https://google.com/maps", "https://google.com/maps"),
            ProviderEntry("WeatherAPI.com", WEATHERAPI,
                    "https://weatherapi.com", "https://weatherapi.com/api")
    )

    private val NonGMSLocationAPIs = listOf(
            ProviderEntry("Google", GOOGLE,
                    "https://google.com/maps", "https://google.com/maps")
    )
}