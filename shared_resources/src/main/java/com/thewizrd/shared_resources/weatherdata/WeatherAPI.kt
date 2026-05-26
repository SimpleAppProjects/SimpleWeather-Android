package com.thewizrd.shared_resources.weatherdata

import androidx.annotation.StringDef
import com.thewizrd.shared_resources.BuildConfig
import com.thewizrd.shared_resources.controls.ProviderEntry
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.weatherdata.model.Weather

object WeatherAPI {
    // APIs
    const val YAHOO = "Yahoo"
    const val WEATHERUNDERGROUND = "WUnderground"
    const val OPENWEATHERMAP = "openweather"
    const val OPENWEATHERMAP_ONECALL = "openweather_onecall"
    const val METNO = "Metno"
    const val HERE = "Here"
    const val NWS = "NWS"
    const val WEATHERUNLOCKED = "wunlocked"
    const val METEOFRANCE = "meteofrance"
    const val TOMORROWIO = "tomorrowio"
    const val ACCUWEATHER = "accuweather"
    const val WEATHERBITIO = "weatherbitio"
    const val METEOMATICS = "meteomatics"
    const val APPLE = "apple"
    const val DWD = "dwd"
    const val ECCC = "eccc"

    // Location APIs
    const val ANDROID = "android"
    const val LOCATIONIQ = "LocIQ"
    const val GOOGLE = "google"
    const val WEATHERAPI = "weatherapi"

    // Radar
    const val RAINVIEWER = "rainviewer"

    // Misc
    const val GOOGLE_POLLEN = "google_pollen"

    /**
     * Note to self: Common steps to adding a new weather provider
     * 1) Implement [WeatherProvider] class
     * 2) Add constructor for [Weather] data objects
     * 3) Update [LocationQuery] (if needed)
     * 4) Add API to provider list below
     * 5) Add API to WeatherProviderManager / [com.thewizrd.weather_api.weatherdata.WeatherProviderFactoryImpl]
     * 6) Add to remote_config_defaults.xml
     */
    @StringDef(
        HERE,
        YAHOO,
        METNO,
        NWS,
        OPENWEATHERMAP,
        WEATHERAPI,
        WEATHERUNLOCKED,
        METEOFRANCE,
        TOMORROWIO,
        ACCUWEATHER,
        WEATHERBITIO,
        METEOMATICS,
        APPLE,
        DWD,
        ECCC,
        GOOGLE
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class WeatherProviders

    @StringDef(ANDROID, HERE, LOCATIONIQ, GOOGLE, WEATHERAPI, OPENWEATHERMAP, ACCUWEATHER)
    @Retention(AnnotationRetention.SOURCE)
    annotation class LocationProviders

    val APIs: Set<ProviderEntry>
        get() {
            return if (BuildConfig.IS_NONGMS) {
                if (settingsManager.isDevSettingsEnabled()) {
                    NonGMSAPIs.toMutableSet().apply {
                        plusAssign(TestingAPIs)
                    }
                } else {
                    NonGMSAPIs.toSet()
                }
            } else {
                if (settingsManager.isDevSettingsEnabled()) {
                    GMSFullAPIs.toMutableSet().apply {
                        plusAssign(TestingAPIs)
                    }
                } else {
                    GMSFullAPIs.toSet()
                }
            }
        }

    val LocationAPIs: Set<ProviderEntry>
        get() {
            return if (BuildConfig.IS_NONGMS) {
                NonGMSLocationAPIs.toSet()
            } else {
                GMSFullLocationAPIs.toSet()
            }
        }

    private val GMSFullAPIs by lazy {
        listOf(
            ProviderEntry(
                "Apple Weather", APPLE,
                "https://developer.apple.com/weatherkit/",
                "https://developer.apple.com/weatherkit/"
            ),
            ProviderEntry(
                "HERE Weather", HERE,
                "https://www.here.com/en",
                "https://developer.here.com/?create=Freemium-Basic&keepState=true&step=account"
            ),
            ProviderEntry(
                "OpenWeatherMap", OPENWEATHERMAP,
                "http://www.openweathermap.org", "https://home.openweathermap.org/users/sign_up"
            ),
            ProviderEntry(
                "WeatherAPI.com", WEATHERAPI,
                "https://weatherapi.com", "https://weatherapi.com/api"
            ),
            ProviderEntry(
                "National Weather Service (United States)", NWS,
                "https://www.weather.gov", "https://www.weather.gov"
            ),
            ProviderEntry(
                "BrightSky (DWD) [Germany]", DWD,
                "https://brightsky.dev/", "https://brightsky.dev/"
            ),
            ProviderEntry(
                "Environment and Climate Change Canada (ECCC)", ECCC,
                "https://www.weather.gc.ca/", "https://www.weather.gc.ca/canada_e.html"
            ),
            ProviderEntry(
                "Meteo France", METEOFRANCE,
                "https://meteofrance.com/", "https://meteofrance.com/"
            ),
            ProviderEntry(
                "MET Norway", METNO,
                "https://www.met.no/en", "https://www.met.no/en"
            ),
            ProviderEntry(
                "Tomorrow.io", TOMORROWIO,
                "https://www.tomorrow.io/weather-api/", "https://www.tomorrow.io/weather-api/"
            ),
            ProviderEntry(
                "Weatherbit.io", WEATHERBITIO,
                "https://www.weatherbit.io/", "https://www.weatherbit.io/pricing"
            ),
            ProviderEntry(
                "WeatherUnlocked", WEATHERUNLOCKED,
                "https://developer.weatherunlocked.com/", "https://developer.weatherunlocked.com/"
            )
        )
    }

    private val NonGMSAPIs by lazy {
        listOf(
            ProviderEntry(
                "MET Norway", METNO,
                "https://www.met.no/en", "https://www.met.no/en"
            ),
            ProviderEntry(
                "National Weather Service (United States)", NWS,
                "https://www.weather.gov", "https://www.weather.gov"
            ),
            ProviderEntry(
                "BrightSky (DWD) [Germany]", DWD,
                "https://brightsky.dev/", "https://brightsky.dev/"
            ),
            ProviderEntry(
                "Environment and Climate Change Canada (ECCC)", ECCC,
                "https://www.weather.gc.ca/", "https://www.weather.gc.ca/canada_e.html"
            ),
            ProviderEntry(
                "HERE Weather", HERE,
                "https://www.here.com/en",
                "https://developer.here.com/?create=Freemium-Basic&keepState=true&step=account"
            ),
            ProviderEntry(
                "OpenWeatherMap", OPENWEATHERMAP,
                "http://www.openweathermap.org", "https://home.openweathermap.org/users/sign_up"
            ),
            ProviderEntry(
                "WeatherAPI.com", WEATHERAPI,
                "https://weatherapi.com", "https://weatherapi.com/api"
            ),
            ProviderEntry(
                "Tomorrow.io", TOMORROWIO,
                "https://www.tomorrow.io/weather-api/", "https://www.tomorrow.io/weather-api/"
            ),
            ProviderEntry(
                "Weatherbit.io", WEATHERBITIO,
                "https://www.weatherbit.io/", "https://www.weatherbit.io/pricing"
            )
        )
    }

    private val TestingAPIs by lazy {
        listOf(
            ProviderEntry(
                "AccuWeather", ACCUWEATHER,
                "https://www.accuweather.com/", "https://developer.accuweather.com/"
            ),
            ProviderEntry(
                "Google",
                GOOGLE,
                "https://www.google.com/maps",
                "https://developers.google.com/maps/documentation/weather"
            )
        )
    }

    private val GMSFullLocationAPIs by lazy {
        listOf(
            ProviderEntry(
                "Google", ANDROID,
                "https://google.com/maps", "https://google.com/maps"
            ),
            ProviderEntry(
                "LocationIQ", LOCATIONIQ,
                "https://locationiq.com", "https://locationiq.com"
            ),
            ProviderEntry(
                "Google", GOOGLE,
                "https://google.com/maps", "https://google.com/maps"
            ),
            ProviderEntry(
                "WeatherAPI.com", WEATHERAPI,
                "https://weatherapi.com", "https://weatherapi.com/api"
            ),
            ProviderEntry(
                "Google", ACCUWEATHER,
                /* Uses AndroidLocationProvider | accuweather is used for locationid only */
                "https://google.com/maps", "https://google.com/maps"
            )
        )
    }

    private val NonGMSLocationAPIs by lazy {
        listOf(
            ProviderEntry(
                "Google", ANDROID,
                "https://google.com/maps", "https://google.com/maps"
            ),
            ProviderEntry(
                "Google", ACCUWEATHER,
                /* Uses AndroidLocationProvider | accuweather is used for locationid only */
                "https://google.com/maps", "https://google.com/maps"
            ),
            ProviderEntry(
                "OpenWeatherMap", OPENWEATHERMAP,
                "http://www.openweathermap.org", "https://home.openweathermap.org/users/sign_up"
            )
        )
    }
}