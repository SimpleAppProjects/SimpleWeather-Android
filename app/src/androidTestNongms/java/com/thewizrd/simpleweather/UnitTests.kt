package com.thewizrd.simpleweather

import android.content.Context
import android.content.SharedPreferences
import android.location.Geocoder
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thewizrd.common.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.*
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.WeatherLocationProvider
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.utils.Coordinate
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherProvider
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.weather_api.aqicn.AQICNProvider
import com.thewizrd.weather_api.nws.SolCalcAstroProvider
import com.thewizrd.weather_api.nws.alerts.NWSAlertProvider
import com.thewizrd.weather_api.openweather.citydb.CityDBLocationProvider
import com.thewizrd.weather_api.openweather.location.OpenWeatherMapLocationProvider
import com.thewizrd.weather_api.smc.SunMoonCalcProvider
import com.thewizrd.weather_api.weatherModule
import com.thewizrd.weather_api.weatherapi.location.WeatherApiLocationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@RunWith(AndroidJUnit4::class)
class UnitTests {
    private lateinit var context: Context
    private var wasUsingPersonalKey = false

    @Before
    fun init() {
        // Context of the app under test.
        context = ApplicationProvider.getApplicationContext()

        appLib = object : ApplicationLib() {
            override val context: Context
                get() = this@UnitTests.context.applicationContext
            override val preferences: SharedPreferences
                get() = PreferenceManager.getDefaultSharedPreferences(context)

            override fun registerAppSharedPreferenceListener() {}
            override fun unregisterAppSharedPreferenceListener() {}
            override fun registerAppSharedPreferenceListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}
            override fun unregisterAppSharedPreferenceListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}

            override val appState: AppState
                get() = AppState.BACKGROUND
            override val isPhone = true
            override val properties = Bundle()
            override val settingsManager = SettingsManager(context)
        }

        // Needs to be called on main thread
        runBlocking(Dispatchers.Main.immediate) {
            sharedDeps = object : SharedModule() {
                override val context = this@UnitTests.context.applicationContext
            }
        }

        runBlocking {
            appLib.settingsManager.loadIfNeeded()
        }

        if (appLib.settingsManager.usePersonalKey()) {
            appLib.settingsManager.setPersonalKey(false)
            wasUsingPersonalKey = true
        }
    }

    @Throws(WeatherException::class)
    private suspend fun getWeather(
        provider: WeatherProvider,
        coordinate: Coordinate = Coordinate(
            47.6721646,
            -122.1706614
        ) /* Redmond, WA */
    ) = withContext(Dispatchers.IO) {
        val location = provider.getLocation(coordinate)
        assertNotNull(location)
        if (location!!.locationTZLong.isNullOrBlank() && location.locationLat != 0.0 && location.locationLong != 0.0) {
            val tzId =
                weatherModule.tzdbService.getTimeZone(location.locationLat, location.locationLong)
            if ("unknown" != tzId)
                location.locationTZLong = tzId
        }
        val locData = location.toLocationData()
        return@withContext provider.getWeather(locData)
    }

    @Throws(WeatherException::class)
    @Test
    fun getMetNoWeather() {
        runBlocking(Dispatchers.Default) {
            val provider = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.METNO)
            val weather = getWeather(provider)
            assertTrue(weather.isValid)
        }
    }

    @Throws(WeatherException::class)
    @Test
    fun getNWSAlerts() {
        runBlocking(Dispatchers.Default) {
            val location =
                weatherModule.weatherManager.getWeatherProvider(WeatherAPI.OPENWEATHERMAP)
                    .getLocation(Coordinate(47.6721646, -122.1706614))
            val locData = location?.toLocationData()
            assertNotNull(locData)
            val alerts = withContext(Dispatchers.IO) {
                NWSAlertProvider().getAlerts(locData!!)
            }
            assertNotNull(alerts)
        }
    }

    @Throws(WeatherException::class)
    @Test
    fun getNWSWeather() {
        runBlocking(Dispatchers.Default) {
            val provider = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.NWS)
            val weather = getWeather(provider)
            assertTrue(weather.forecast?.isNotEmpty() == true && weather.hrForecast?.isNotEmpty() == true)
            assertTrue(weather.isValid)
        }
    }

    @Throws(WeatherException::class)
    @Test
    fun getOWMWeather() {
        runBlocking(Dispatchers.Default) {
            val provider =
                weatherModule.weatherManager.getWeatherProvider(WeatherAPI.OPENWEATHERMAP)
            val weather = getWeather(provider)
            assertTrue(weather.forecast?.isNotEmpty() == true && weather.hrForecast?.isNotEmpty() == true)
            assertTrue(weather.isValid)
        }
    }

    @Throws(WeatherException::class)
    @Test
    fun getAQIData() {
        runBlocking(Dispatchers.Default) {
            val locationData = LocationData().apply {
                latitude = 47.6721646
                longitude = -122.1706614
                tzLong = "America/Los_Angeles"
            }

            val aqi = withContext(Dispatchers.IO) {
                AQICNProvider().getAirQualityData(locationData)
            }
            assertNotNull(aqi)
        }
    }

    @Test
    @Throws(WeatherException::class, IOException::class)
    fun serializationTest() {
        runBlocking(Dispatchers.Default) {
            val provider = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.NWS)

            val startTime = SystemClock.elapsedRealtimeNanos()
            val weather = getWeather(provider)
            val endTime = SystemClock.elapsedRealtimeNanos()
            val dura = Duration.ofNanos(endTime - startTime)
            Log.d("Serialzer", "JSON GetWeather Test: $dura")

            for (i in 0..29) {
                val startTime2 = SystemClock.elapsedRealtimeNanos()
                val json2 = withContext(Dispatchers.Default) {
                    JSONParser.serializer(weather, Weather::class.java)
                }
                val desW2 = withContext(Dispatchers.Default) {
                    JSONParser.deserializer(json2, Weather::class.java)
                }
                val endTime2 = SystemClock.elapsedRealtimeNanos()
                val dura2 = Duration.ofNanos(endTime2 - startTime2)

                Log.d("Serialzer", "JSON2 Test ${i + 1}: $dura2")
            }
        }
    }

    @Throws(WeatherException::class)
    @Test
    fun getSunriseSetTime() {
        runBlocking(Dispatchers.Default) {
            val date = ZonedDateTime.now()

            val locationData = LocationData().apply {
                latitude = 47.6721646
                longitude = -122.1706614
                tzLong = "America/Los_Angeles"
            }

            val astro = withContext(Dispatchers.Default) {
                SolCalcAstroProvider().getAstronomyData(locationData, date)
            }

            val fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            Log.d("SolCalc", String.format(Locale.ROOT,
                    "Sunrise: %s; Sunset: %s", astro.sunrise.format(fmt), astro.sunset.format(fmt)))
            assertTrue(astro.sunrise != DateTimeUtils.LOCALDATETIME_MIN && astro.sunset != DateTimeUtils.LOCALDATETIME_MIN)
        }
    }

    @Test
    @Throws(Exception::class)
    fun simpleAstroTest() {
        runBlocking(Dispatchers.Default) {
            val date = ZonedDateTime.now()
            val locationData = LocationData().apply {
                latitude = 47.6721646
                longitude = -122.1706614
                tzLong = "America/Los_Angeles"
            }
            val astro = withContext(Dispatchers.Default) {
                SunMoonCalcProvider().getAstronomyData(locationData, date)
            }

            val fmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            Log.d("SMC", String.format(Locale.ROOT,
                    "Sunrise: %s; Sunset: %s, Moonrise: %s; Moonset: %s", astro.sunrise.format(fmt), astro.sunset.format(fmt), astro.moonrise.format(fmt), astro.moonset.format(fmt)))
            if (astro.moonPhase != null) {
                Log.d("SMC", String.format(Locale.ROOT,
                        "Moonphase: %s", astro.moonPhase.phase.name))
            }

            assertTrue(astro.sunrise != DateTimeUtils.LOCALDATETIME_MIN && astro.sunset != DateTimeUtils.LOCALDATETIME_MIN && astro.moonrise != DateTimeUtils.LOCALDATETIME_MIN && astro.moonset != DateTimeUtils.LOCALDATETIME_MIN)
        }
    }

    @Test
    @Throws(IOException::class)
    fun androidAutoCompleteLocTest() {
        runBlocking(Dispatchers.Default) {
            assertTrue(Geocoder.isPresent())
            val geocoder = Geocoder(context, Locale.getDefault())
            val addressList = withContext(Dispatchers.IO) {
                geocoder.getFromLocationName("Redmond", 5) // Redmond
            }
            assertFalse(addressList.isNullOrEmpty())
        }
    }

    @Test
    @Throws(IOException::class)
    fun androidGeocoderTest() {
        runBlocking(Dispatchers.Default) {
            assertTrue(Geocoder.isPresent())
            val geocoder = Geocoder(context, Locale.getDefault())
            //List<Address> addressList = geocoder.getFromLocation(47.6721646, -122.1706614, 1); // Washington
            val addressList = withContext(Dispatchers.IO) {
                geocoder.getFromLocation(51.5073884, -0.1334347, 1) // London
            }
            assertFalse(addressList.isNullOrEmpty())
            val result = addressList!![0]
            assertNotNull(result)
        }
    }

    @Test
    @Throws(WeatherException::class)
    fun weatherAPILocationTest() {
        runBlocking(Dispatchers.Default) {
            val locationProvider: WeatherLocationProvider = WeatherApiLocationProvider()
            val locations = withContext(Dispatchers.IO) {
                locationProvider.getLocations("Redmond, WA", WeatherAPI.WEATHERAPI)
            }
            assertFalse(locations.isNullOrEmpty())

            val queryVM = locations.find { it.locationName?.startsWith("Redmond") == true }
            assertNotNull(queryVM)

            val locModel = if (locationProvider.needsLocationFromName()) {
                locationProvider.getLocationFromName(queryVM!!)
            } else if (locationProvider.needsLocationFromGeocoder()) {
                locationProvider.getLocation(
                    Coordinate(
                        queryVM!!.locationLat,
                        queryVM.locationLong
                    ), WeatherAPI.OPENWEATHERMAP
                )
            } else if (locationProvider.needsLocationFromID()) {
                locationProvider.getLocationFromID(queryVM!!)
            } else {
                queryVM
            }

            assertNotNull(locModel)

            if (locModel!!.locationTZLong.isNullOrBlank() && locModel.locationLat != 0.0 && locModel.locationLong != 0.0) {
                val tzId = weatherModule.tzdbService.getTimeZone(
                    locModel.locationLat,
                    locModel.locationLong
                )
                if ("unknown" != tzId)
                    locModel.locationTZLong = tzId
            }

            assertFalse(locModel.locationTZLong.isNullOrEmpty())
            assertTrue(locModel.toLocationData().isValid)
        }
    }

    @Throws(WeatherException::class)
    @Test
    fun getMeteoFranceWeather() {
        runBlocking(Dispatchers.Default) {
            val provider =
                weatherModule.weatherManager.getWeatherProvider(WeatherAPI.METEOFRANCE)
            val weather = getWeather(provider, Coordinate(48.85, 2.34))
            assertTrue(weather.isValid)
        }
    }

    @Throws(WeatherException::class)
    @Test
    fun getWeatherAPIWeather() {
        runBlocking(Dispatchers.Default) {
            val provider = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.WEATHERAPI)
            val weather = getWeather(provider)
            assertTrue(weather.isValid && WeatherNowViewModel(weather).isValid)
        }
    }

    @Throws(WeatherException::class)
    @Test
    fun getTomorrowIOWeather() {
        runBlocking(Dispatchers.Default) {
            val provider = weatherModule.weatherManager.getWeatherProvider(WeatherAPI.TOMORROWIO)
            val weather =
                getWeather(provider, Coordinate(34.0207305, -118.6919157)) // ~ Los Angeles
            assertTrue(weather.isValid && WeatherNowViewModel(weather).isValid)
        }
    }

    @Throws(WeatherException::class)
    @Test
    fun getWeatherbitIOWeather() {
        runBlocking(Dispatchers.Default) {
            val provider =
                weatherModule.weatherManager.getWeatherProvider(WeatherAPI.WEATHERBITIO)
            val weather =
                getWeather(provider, Coordinate(39.9, -105.1)) // ~ Denver, CO
            assertTrue(weather.isValid && WeatherNowViewModel(weather).isValid)
        }
    }

    @Throws(WeatherException::class)
    @Test
    fun getMeteomaticsWeather() {
        runBlocking(Dispatchers.Default) {
            val provider =
                weatherModule.weatherManager.getWeatherProvider(WeatherAPI.METEOMATICS)
            val weather =
                getWeather(provider, Coordinate(52.22, 20.97))
            assertTrue(weather.isValid && WeatherNowViewModel(weather).isValid)
        }
    }

    @Test
    @Throws(WeatherException::class)
    fun owmLocationTest() {
        runBlocking(Dispatchers.Default) {
            val locationProvider: WeatherLocationProvider = OpenWeatherMapLocationProvider()
            val locations = withContext(Dispatchers.IO) {
                locationProvider.getLocations("Redmond", WeatherAPI.OPENWEATHERMAP)
            }
            assertFalse(locations.isNullOrEmpty())

            val queryVM = locations.find { it.locationName?.startsWith("Redmond") == true }
            assertNotNull(queryVM)

            val locModel = if (locationProvider.needsLocationFromName()) {
                locationProvider.getLocationFromName(queryVM!!)
            } else if (locationProvider.needsLocationFromGeocoder()) {
                locationProvider.getLocation(
                    Coordinate(
                        queryVM!!.locationLat,
                        queryVM.locationLong
                    ), WeatherAPI.OPENWEATHERMAP
                )
            } else if (locationProvider.needsLocationFromID()) {
                locationProvider.getLocationFromID(queryVM!!)
            } else {
                queryVM
            }

            assertNotNull(locModel)

            if (locModel!!.locationTZLong.isNullOrBlank() && locModel.locationLat != 0.0 && locModel.locationLong != 0.0) {
                val tzId = weatherModule.tzdbService.getTimeZone(
                    locModel.locationLat,
                    locModel.locationLong
                )
                if ("unknown" != tzId)
                    locModel.locationTZLong = tzId
            }

            assertFalse(locModel.locationTZLong.isNullOrEmpty())
            assertTrue(locModel.toLocationData().isValid)
        }
    }

    @Test
    @Throws(WeatherException::class)
    fun owmGeocoderLocationTest() {
        runBlocking(Dispatchers.Default) {
            val locationProvider: WeatherLocationProvider = OpenWeatherMapLocationProvider()
            val location = withContext(Dispatchers.IO) {
                locationProvider.getLocation(
                    Coordinate(51.5073884, -0.1334347),
                    WeatherAPI.OPENWEATHERMAP
                ) // London
            }
            assertNotNull(location)
            assertEquals(location?.locationName?.startsWith("London"), true)

            if (location!!.locationTZLong.isNullOrBlank() && location.locationLat != 0.0 && location.locationLong != 0.0) {
                val tzId = weatherModule.tzdbService.getTimeZone(
                    location.locationLat,
                    location.locationLong
                )
                if ("unknown" != tzId)
                    location.locationTZLong = tzId
            }

            assertFalse(location.locationTZLong.isNullOrEmpty())
            assertTrue(location.toLocationData().isValid)
        }
    }

    @Test
    @Throws(WeatherException::class)
    fun cityDBLocationTest() {
        runBlocking(Dispatchers.Default) {
            val locationProvider: WeatherLocationProvider = CityDBLocationProvider()
            val locations = withContext(Dispatchers.IO) {
                locationProvider.getLocations("Redmond", WeatherAPI.OPENWEATHERMAP)
            }
            assertFalse(locations.isNullOrEmpty())

            val queryVM = locations.find { it.locationName?.startsWith("Redmond, ") == true }
            assertNotNull(queryVM)

            if (locationProvider.needsLocationFromName()) {
                val nameModel = locationProvider.getLocationFromName(queryVM!!)
                assertNotNull(nameModel)
            } else if (locationProvider.needsLocationFromGeocoder()) {
                val geoModel = locationProvider.getLocation(
                    Coordinate(
                        queryVM!!.locationLat,
                        queryVM.locationLong
                    ), WeatherAPI.OPENWEATHERMAP
                )
                assertNotNull(geoModel)
            } else if (locationProvider.needsLocationFromID()) {
                val idModel = locationProvider.getLocationFromID(queryVM!!)
                assertNotNull(idModel)
            }
        }
    }

    @Test
    @Throws(WeatherException::class)
    fun cityDBGeocoderLocationTest() {
        runBlocking(Dispatchers.Default) {
            val locationProvider: WeatherLocationProvider = CityDBLocationProvider()
            val location = withContext(Dispatchers.IO) {
                locationProvider.getLocation(
                    Coordinate(51.5073884, -0.1334347),
                    WeatherAPI.OPENWEATHERMAP
                ) // London
            }
            assertNotNull(location)
            assertEquals(location?.locationName?.startsWith("London"), true)

            if (location!!.locationTZLong.isNullOrBlank() && location.locationLat != 0.0 && location.locationLong != 0.0) {
                val tzId = weatherModule.tzdbService.getTimeZone(
                    location.locationLat,
                    location.locationLong
                )
                if ("unknown" != tzId)
                    location.locationTZLong = tzId
            }

            assertFalse(location.locationTZLong.isNullOrEmpty())
            assertTrue(location.toLocationData().isValid)
        }
    }
}