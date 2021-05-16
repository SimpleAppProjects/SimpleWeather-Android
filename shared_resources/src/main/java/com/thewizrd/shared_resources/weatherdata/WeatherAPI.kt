package com.thewizrd.shared_resources.weatherdata

import androidx.annotation.StringDef
import com.thewizrd.shared_resources.BuildConfig
import com.thewizrd.shared_resources.controls.ProviderEntry

object WeatherAPI {
    // APIs
    const val YAHOO = "Yahoo"
    const val WEATHERUNDERGROUND = "WUnderground"
    const val OPENWEATHERMAP = "openweather"
    const val METNO = "Metno"
    const val HERE = "Here"
    const val NWS = "NWS"
    const val WEATHERUNLOCKED = "wunlocked"
    const val METEOFRANCE = "meteofrance"

    // Location APIs
    const val ANDROID = "android"
    const val LOCATIONIQ = "LocIQ"
    const val GOOGLE = "google"
    const val WEATHERAPI = "weatherapi"

    /**
     * Note to self: Common steps to adding a new weather provider
     * 1) Implement WeatherProviderImpl class
     * 2) Add constructor for Weather data objects
     * 3) Update LocationQueryViewModel (if needed)
     * 4) Add API to provider list below
     * 5) Add API to WeatherManager
     * 6) Add to remote_config_defaults.xml
     */
    @StringDef(HERE, YAHOO, METNO, NWS, OPENWEATHERMAP, WEATHERUNLOCKED, METEOFRANCE)
    @Retention(AnnotationRetention.SOURCE)
    annotation class WeatherProviders

    @StringDef(ANDROID, HERE, LOCATIONIQ, GOOGLE, WEATHERAPI)
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
        ProviderEntry(
            "HERE Weather",
            HERE,
            "https://www.here.com/en",
            "https://developer.here.com/?create=Freemium-Basic&keepState=true&step=account"
        ),
        ProviderEntry(
            "MET Norway", METNO,
            "https://www.met.no/en", "https://www.met.no/en"
        ),
        ProviderEntry(
            "National Weather Service (United States)", NWS,
            "https://www.weather.gov", "https://www.weather.gov"
        ),
        ProviderEntry(
            "OpenWeatherMap", OPENWEATHERMAP,
            "http://www.openweathermap.org", "https://home.openweathermap.org/users/sign_up"
        ),
        ProviderEntry(
            "WeatherUnlocked", WEATHERUNLOCKED,
            "https://developer.weatherunlocked.com/", "https://developer.weatherunlocked.com/"
        ),
        ProviderEntry(
            "Meteo France", METEOFRANCE,
            "https://meteofrance.com/", "https://meteofrance.com/"
        )
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
            ProviderEntry("Google", ANDROID,
                    "https://google.com/maps", "https://google.com/maps"),
            ProviderEntry("LocationIQ", LOCATIONIQ,
                    "https://locationiq.com", "https://locationiq.com"),
            ProviderEntry("Google", GOOGLE,
                    "https://google.com/maps", "https://google.com/maps"),
            ProviderEntry("WeatherAPI.com", WEATHERAPI,
                    "https://weatherapi.com", "https://weatherapi.com/api")
    )

    private val NonGMSLocationAPIs = listOf(
            ProviderEntry("Google", ANDROID,
                    "https://google.com/maps", "https://google.com/maps")
    )
}