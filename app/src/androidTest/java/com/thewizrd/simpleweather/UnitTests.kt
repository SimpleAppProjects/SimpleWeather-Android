package com.thewizrd.simpleweather

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.base.Stopwatch
import com.google.common.collect.Iterables
import com.thewizrd.shared_resources.AppState
import com.thewizrd.shared_resources.ApplicationLib
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationProviderImpl
import com.thewizrd.shared_resources.locationdata.google.GoogleLocationProvider
import com.thewizrd.shared_resources.locationdata.weatherapi.WeatherApiLocationProvider
import com.thewizrd.shared_resources.tzdb.TimeZoneProvider
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.here.HEREOAuthUtils
import com.thewizrd.shared_resources.weatherdata.*
import com.thewizrd.shared_resources.weatherdata.aqicn.AQICNProvider
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.nws.SolCalcAstroProvider
import com.thewizrd.shared_resources.weatherdata.nws.alerts.NWSAlertProvider
import com.thewizrd.shared_resources.weatherdata.smc.SunMoonCalcProvider
import com.thewizrd.simpleweather.images.ImageDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ExecutionException

@RunWith(AndroidJUnit4::class)
class UnitTests {
    private lateinit var context: Context

    @Before
    fun init() {
        // Context of the app under test.
        context = ApplicationProvider.getApplicationContext()

        val app = object : ApplicationLib {
            override fun getAppContext(): Context {
                return context.applicationContext
            }

            override fun getPreferences(): SharedPreferences {
                return PreferenceManager.getDefaultSharedPreferences(appContext)
            }

            override fun registerAppSharedPreferenceListener() {}
            override fun unregisterAppSharedPreferenceListener() {}
            override fun registerAppSharedPreferenceListener(listener: OnSharedPreferenceChangeListener) {}
            override fun unregisterAppSharedPreferenceListener(listener: OnSharedPreferenceChangeListener) {}

            override fun getAppState(): AppState {
                return AppState.BACKGROUND
            }

            override fun isPhone(): Boolean {
                return true
            }

            override fun getProperties(): Bundle {
                return Bundle()
            }

            override fun getSettingsManager(): SettingsManager {
                return SettingsManager(appContext.applicationContext)
            }
        }

        // Needs to be called on main thread
        runBlocking(Dispatchers.Main.immediate) {
            SimpleLibrary.initialize(app)
        }

        // Start logger
        Logger.init(app.appContext)
        app.settingsManager.loadIfNeededSync()
    }

    @Throws(WeatherException::class)
    private suspend fun getWeather(
        providerImpl: WeatherProviderImpl,
        coordinate: Coordinate = Coordinate(
            47.6721646,
            -122.1706614
        ) /* Redmond, WA */
    ) = withContext(Dispatchers.IO) {
        val location = providerImpl.getLocation(coordinate)
        val locData = LocationData(location!!)
        return@withContext providerImpl.getWeather(locData)
    }

    @Throws(WeatherException::class)
    @Test
    fun getHEREWeather() {
        runBlocking(Dispatchers.Default) {
            val provider = WeatherManager.getProvider(WeatherAPI.HERE)
            val weather = getWeather(provider)
            Assert.assertTrue(weather.isValid)
        }
    }

    @Throws(WeatherException::class)
    @Test
    fun getMetNoWeather() {
        runBlocking(Dispatchers.Default) {
            val provider = WeatherManager.getProvider(WeatherAPI.METNO)
            val weather = getWeather(provider)
            Assert.assertTrue(weather.isValid)
        }
    }

    @Throws(WeatherException::class)
    @Test
    fun getNWSAlerts() {
        runBlocking(Dispatchers.Default) {
            val location = WeatherManager.getProvider(WeatherAPI.OPENWEATHERMAP)
                    .getLocation(Coordinate(47.6721646, -122.1706614))
            val locData = LocationData(location!!)
            val alerts = withContext(Dispatchers.IO) {
                NWSAlertProvider().getAlerts(locData)
            }
            Assert.assertNotNull(alerts)
        }
    }

    @Throws(WeatherException::class)
    @Test
    fun getNWSWeather() {
        runBlocking(Dispatchers.Default) {
            val provider = WeatherManager.getProvider(WeatherAPI.NWS)
            val weather = getWeather(provider)
            Assert.assertTrue(weather.forecast?.isNotEmpty() == true && weather.hrForecast?.isNotEmpty() == true)
            Assert.assertTrue(weather.isValid)
        }
    }

    @Throws(WeatherException::class)
    @Test
    fun getOWMWeather() {
        runBlocking(Dispatchers.Default) {
            val provider = WeatherManager.getProvider(WeatherAPI.OPENWEATHERMAP)
            val weather = getWeather(provider)
            Assert.assertTrue(weather.isValid)
        }
    }

    @Throws(WeatherException::class)
    @Test
    fun getWUnlockedWeather() {
        runBlocking(Dispatchers.Default) {
            val provider = WeatherManager.getProvider(WeatherAPI.WEATHERUNLOCKED)
            val weather = getWeather(provider)
            Assert.assertTrue(!weather.forecast.isNullOrEmpty() && !weather.hrForecast.isNullOrEmpty())
            Assert.assertTrue(weather.isValid)
        }
    }

    @Test
    fun getHEREOAuthToken() {
        runBlocking(Dispatchers.Default) {
            val token = withContext(Dispatchers.IO) {
                HEREOAuthUtils.getBearerToken(true)
            }
            Assert.assertFalse(token.isNullOrEmpty())
        }
    }

    @Test
    fun getTimeZone() {
        runBlocking(Dispatchers.Default) {
            val tz = withContext(Dispatchers.IO) {
                TimeZoneProvider().getTimeZone(0.0, 0.0)
            }
            Log.d("TZTest", "tz = $tz")
            Assert.assertFalse(tz.isNullOrEmpty())
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
            Assert.assertNotNull(aqi)
        }
    }

    @Test
    @Throws(WeatherException::class, IOException::class)
    fun serializationTest() {
        runBlocking(Dispatchers.Default) {
            val provider = WeatherManager.getProvider(WeatherAPI.NWS)

            val s = Stopwatch.createStarted()
            val weather = getWeather(provider)
            s.stop()
            Log.d("Serialzer", "JSON GetWeather Test: $s")

            for (i in 0..29) {
                val s2 = Stopwatch.createStarted()
                val json2 = withContext(Dispatchers.Default) {
                    JSONParser.serializer(weather, Weather::class.java)
                }
                val desW2 = withContext(Dispatchers.Default) {
                    JSONParser.deserializer(json2, Weather::class.java)
                }
                s2.stop()

                Log.d("Serialzer", "JSON2 Test " + (i + 1) + ": " + s2.toString())
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
            Assert.assertTrue(astro.sunrise !== LocalDateTime.MIN && astro.sunset !== LocalDateTime.MIN)
        }
    }

    @Test
    @Throws(ExecutionException::class, InterruptedException::class)
    fun firebaseDBTest() {
        runBlocking(Dispatchers.Default) {
            val updateTime = ImageDatabase.getLastUpdateTime()
            Assert.assertTrue(updateTime > 0)
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

            Assert.assertTrue(astro.sunrise !== LocalDateTime.MIN && astro.sunset !== LocalDateTime.MIN && astro.moonrise !== LocalDateTime.MIN && astro.moonset !== LocalDateTime.MIN)
        }
    }

    @Test
    @Throws(IOException::class)
    fun androidAutoCompleteLocTest() {
        runBlocking(Dispatchers.Default) {
            Assert.assertTrue(Geocoder.isPresent())
            val geocoder = Geocoder(context, Locale.getDefault())
            val addressList = withContext(Dispatchers.IO) {
                geocoder.getFromLocationName("Redmond", 5) // Redmond
            }
            Assert.assertFalse(addressList.isNullOrEmpty())
        }
    }

    @Test
    @Throws(IOException::class)
    fun androidGeocoderTest() {
        runBlocking(Dispatchers.Default) {
            Assert.assertTrue(Geocoder.isPresent())
            val geocoder = Geocoder(context, Locale.getDefault())
            //List<Address> addressList = geocoder.getFromLocation(47.6721646, -122.1706614, 1); // Washington
            val addressList = withContext(Dispatchers.IO) {
                geocoder.getFromLocation(51.5073884, -0.1334347, 1) // London
            }
            Assert.assertFalse(addressList.isNullOrEmpty())
            val result = addressList!![0]
            Assert.assertNotNull(result)
        }
    }

    @Test
    @Throws(WeatherException::class)
    fun placesAPITest() {
        runBlocking(Dispatchers.Default) {
            val locationProvider = GoogleLocationProvider()
            val locations = withContext(Dispatchers.IO) {
                locationProvider.getLocations("Berlin, Germany", null)
            }
            Assert.assertFalse(locations.isNullOrEmpty())

            val queryVM = Iterables.getFirst(locations, null)
            Assert.assertNotNull(queryVM)

            val idModel = locationProvider.getLocationFromID(queryVM!!)
            /*
            LocationQueryViewModel nameModel = locationProvider.getLocationFromName(queryVM);
             */
            Assert.assertNotNull(idModel)
            //Assert.assertNotNull(nameModel);
        }
    }

    @Test
    @Throws(WeatherException::class)
    fun weatherAPILocationTest() {
        runBlocking(Dispatchers.Default) {
            val locationProvider: LocationProviderImpl = WeatherApiLocationProvider()
            val locations = withContext(Dispatchers.IO) {
                locationProvider.getLocations("Redmond, WA", null)
            }
            Assert.assertFalse(locations.isNullOrEmpty())

            val queryVM = locations.find { it.locationName.startsWith("Redmond, ") }
            Assert.assertNotNull(queryVM)

            val nameModel = locationProvider.getLocationFromName(queryVM!!)
            Assert.assertNotNull(nameModel)
        }
    }

    @Throws(WeatherException::class)
    @Test
    fun getMeteoFranceWeather() {
        runBlocking(Dispatchers.Default) {
            val provider = WeatherManager.getProvider(WeatherAPI.METEOFRANCE)
            val weather = getWeather(provider, Coordinate(48.85, 2.34))
            Assert.assertTrue(weather.isValid)
        }
    }
}