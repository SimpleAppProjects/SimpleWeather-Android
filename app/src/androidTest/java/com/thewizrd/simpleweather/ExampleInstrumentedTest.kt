package com.thewizrd.simpleweather

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ibm.icu.text.DateFormat
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.*
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.utils.Coordinate
import com.thewizrd.shared_resources.utils.FileUtils
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlertType
import com.thewizrd.simpleweather.notifications.WeatherAlertNotificationBuilder
import com.thewizrd.simpleweather.notifications.WeatherAlertNotificationService
import com.thewizrd.simpleweather.widgets.WidgetUtils
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    private lateinit var settingsManager: SettingsManager

    @Before
    fun init() {
        // Context of the app under test.
        val appContext = ApplicationProvider.getApplicationContext<Context>()

        appLib = object : ApplicationLib() {
            override val context: Context
                get() = appContext.applicationContext
            override val preferences: SharedPreferences
                get() = PreferenceManager.getDefaultSharedPreferences(context)

            override fun registerAppSharedPreferenceListener() {}
            override fun unregisterAppSharedPreferenceListener() {}
            override fun registerAppSharedPreferenceListener(listener: OnSharedPreferenceChangeListener) {}
            override fun unregisterAppSharedPreferenceListener(listener: OnSharedPreferenceChangeListener) {}

            override val appState: AppState
                get() = AppState.BACKGROUND
            override val isPhone = true
            override val properties = Bundle()
            override val settingsManager = SettingsManager(context)
        }

        // Needs to be called on main thread
        runBlocking(Dispatchers.Main.immediate) {
            sharedDeps = object : SharedModule() {
                override val context = appContext.applicationContext
            }
        }

        settingsManager = appLib.settingsManager

        runBlocking {
            settingsManager.loadIfNeeded()
        }
    }

    @Test
    fun updateWidgetTest() {
        WidgetUtils.addWidgetId("NewYork", 10)
        WidgetUtils.addWidgetId("NewYork", 11)
        WidgetUtils.addWidgetId("NewYork", 12)
        WidgetUtils.addWidgetId("NewYork", 13)
        WidgetUtils.addWidgetId("NewYork", 14)
        WidgetUtils.addWidgetId("NewYork", 15)
        WidgetUtils.addWidgetId("NewYork", 16)
        WidgetUtils.addWidgetId("NewYork", 17)
        val loc = LocationData().apply {
            query = "OldYork"
        }
        WidgetUtils.updateWidgetIds("NewYork", loc)
    }

    // Need to get FULL location data for HERE API
    // Data provided is incomplete
    @Throws(WeatherException::class)
    @Test
    fun getWeatherTest() {
        runBlocking(Dispatchers.Default) {
            val wm = weatherModule.weatherManager
            settingsManager.setAPI(WeatherAPI.HERE)
            wm.updateAPI()

            val collection = withContext(Dispatchers.IO) {
                wm.getLocations("Houston, Texas")
            }
            val locs = ArrayList(collection)
            var loc = locs[0]

            // Need to get FULL location data for HERE API
            // Data provided is incomplete
            if (loc.locationLat == -1.0 && loc.locationLong == -1.0 && loc.locationTZLong == null && wm.getLocationProvider().needsLocationFromID()) {
                val query_vm = loc
                loc = withContext(Dispatchers.IO) {
                    wm.getLocationProvider().getLocationFromID(query_vm)
                }
            } else if (wm.getLocationProvider().needsLocationFromName()) {
                val query_vm = loc
                loc = withContext(Dispatchers.IO) {
                    wm.getLocationProvider().getLocationFromName(query_vm)
                }
            } else if (wm.getLocationProvider().needsLocationFromGeocoder()) {
                val query_vm = loc
                loc = withContext(Dispatchers.IO) {
                    wm.getLocationProvider().getLocation(Coordinate(query_vm.locationLat, query_vm.locationLong), query_vm.weatherSource)
                }
            }

            val locationData = loc.toLocationData()
            val weather = withContext(Dispatchers.IO) {
                wm.getWeather(locationData)
            }

            Assert.assertTrue(weather.isValid)
        }
    }

    @Test
    @Throws(WeatherException::class)
    fun updateLocationQueryTest() {
        runBlocking(Dispatchers.Default) {
            val wm = weatherModule.weatherManager
            settingsManager.setAPI(WeatherAPI.HERE)
            wm.updateAPI()

            val collection = withContext(Dispatchers.IO) {
                wm.getLocations("Houston, Texas")
            }
            val locs = ArrayList(collection)
            var loc = locs[0]

            // Need to get FULL location data for HERE API
            // Data provided is incomplete
            if (loc.locationLat == -1.0 && loc.locationLong == -1.0 && loc.locationTZLong == null && wm.getLocationProvider().needsLocationFromID()) {
                val query_vm = loc
                loc = withContext(Dispatchers.IO) {
                    wm.getLocationProvider().getLocationFromID(query_vm)
                }
            } else if (wm.getLocationProvider().needsLocationFromName()) {
                val query_vm = loc
                loc = withContext(Dispatchers.IO) {
                    wm.getLocationProvider().getLocationFromName(query_vm)
                }
            } else if (wm.getLocationProvider().needsLocationFromGeocoder()) {
                val query_vm = loc
                loc = withContext(Dispatchers.IO) {
                    wm.getLocationProvider().getLocation(Coordinate(query_vm.locationLat, query_vm.locationLong), query_vm.weatherSource)
                }
            }

            val locationData = loc.toLocationData()
            var weather = wm.getWeather(locationData)

            settingsManager.setAPI(WeatherAPI.WEATHERUNLOCKED)
            wm.updateAPI()

            if (weather != null && weather.source != settingsManager.getAPI() || weather == null && locationData.weatherSource != settingsManager.getAPI()) {
                // Update location query and source for new API
                val oldKey = locationData.query
                locationData.query = if (weather != null) {
                    wm.updateLocationQuery(weather)
                } else {
                    wm.updateLocationQuery(locationData)
                }
                locationData.weatherSource = settingsManager.getAPI()
            }

            weather = withContext(Dispatchers.IO) {
                wm.getWeather(locationData)
            }

            Assert.assertTrue(weather.isValid)
        }
    }

    @Test
    fun widgetCleanupTest() {
        WidgetUtils.cleanupWidgetData()
        WidgetUtils.cleanupWidgetIds()
    }

    @Test
    fun notificationTest() {
        runBlocking(Dispatchers.Default) {
            val la = ArrayList<WeatherAlert>()
            for (i in WeatherAlertType.values().indices) {
                val alert = WeatherAlert()
                alert.attribution = "Attribution"
                alert.date = ZonedDateTime.now(ZoneOffset.UTC)
                alert.expiresDate = ZonedDateTime.now(ZoneOffset.UTC).plusDays(5)
                alert.message = "Message"
                alert.title = "Title"
                alert.type = WeatherAlertType.valueOf(i)
                alert.isNotified = false
                la.add(alert)
            }

            val vm = LocationQuery().apply {
                locationCountry = "US"
                locationName = "New York, NY"
                locationQuery = "11434"
                locationTZLong = "America/New_York"
            }

            WeatherAlertNotificationBuilder.createNotifications(vm.toLocationData(), la)

            val vm2 = LocationQuery().apply {
                locationCountry = "US"
                locationName = "New York City, NY"
                locationQuery = "10007"
                locationTZLong = "America/New_York"
            }
            WeatherAlertNotificationBuilder.createNotifications(vm2.toLocationData(), la)

            while (WeatherAlertNotificationService.getNotificationsCount() > 0) {
                delay(3000)
            }
        }
    }

    @Test
    @Throws(IOException::class)
    fun logCleanupTest() {
        // Context of the app under test.
        val appContext = appLib.context
        val filePath = appContext.getExternalFilesDir(null).toString() + "/logs"
        val directory = File(filePath)
        if (!directory.exists()) {
            Assert.assertTrue(directory.mkdir())
        }
        for (i in 0..3) {
            val file = File(filePath + File.separator + "Log." + i + ".log")
            Assert.assertTrue(file.createNewFile())
        }
        runBlocking(Dispatchers.IO) {
            Assert.assertTrue(FileUtils.deleteDirectory(filePath))
        }
    }

    @Test
    fun timeIsRelative() {
        val dateTime = ZonedDateTime.of(2020, 1, 1, 18, 0, 0, 0, ZoneId.systemDefault())
        val date = Date.from(dateTime.toInstant())
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeeMMMMdd", Locale.US).format(date))
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeeMMMMdd", Locale.FRANCE).format(date))
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeeMMMMdd", Locale.JAPAN).format(date))
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeeMMMdd", Locale.US).format(date))
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeeMMMdd", Locale.FRANCE).format(date))
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeeMMMdd", Locale.JAPAN).format(date))
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeMMMMdd", Locale.US).format(date))
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeMMMMdd", Locale.FRANCE).format(date))
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeMMMMdd", Locale.JAPAN).format(date))
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeMMMdd", Locale.US).format(date))
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeMMMdd", Locale.FRANCE).format(date))
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeMMMdd", Locale.JAPAN).format(date))
        Log.d("Time", dateTime.format(DateTimeFormatter.ofPattern("EEE dd", Locale.US)))
        Log.d("Time", dateTime.format(DateTimeFormatter.ofPattern("EEE dd", Locale.FRANCE)))
        Log.d("Time", dateTime.format(DateTimeFormatter.ofPattern("EEE dd", Locale.JAPAN)))
        Log.d("Time", DateFormat.getInstanceForSkeleton("Hm", Locale.US).format(date))
        Log.d("Time", DateFormat.getInstanceForSkeleton("Hm", Locale.FRANCE).format(date))
        Log.d("Time", DateFormat.getInstanceForSkeleton("Hm", Locale.JAPAN).format(date))
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeeeHm", Locale.US).format(date))
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeeeHm", Locale.FRANCE).format(date))
        Log.d("Time", DateFormat.getInstanceForSkeleton("eeeeeHm", Locale.JAPAN).format(date))
    }

    @Test
    fun imageTest() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val file = File(ctx.cacheDir, "images")
        runBlocking(Dispatchers.IO) {
            file.listFiles()?.forEach {
                while (FileUtils.isFileLocked(it)) {
                    delay(250)
                }

                BufferedInputStream(FileInputStream(it)).use { fs ->
                    val imageType = ImageUtils.guessImageType(fs)
                    Log.d("ImageTest", "file path: ${it.path}")
                    Log.d("ImageTest", "type: $imageType")
                    Assert.assertNotEquals(imageType, ImageUtils.ImageType.UNKNOWN)
                }
            }
        }
    }
}