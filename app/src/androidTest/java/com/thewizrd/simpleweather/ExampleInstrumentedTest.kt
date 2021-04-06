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
import com.thewizrd.shared_resources.AppState
import com.thewizrd.shared_resources.ApplicationLib
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.FileUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.shared_resources.utils.WeatherException
import com.thewizrd.shared_resources.utils.WeatherUtils.Coordinate
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherAlert
import com.thewizrd.shared_resources.weatherdata.WeatherAlertType
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.simpleweather.notifications.WeatherAlertNotificationBuilder
import com.thewizrd.simpleweather.notifications.WeatherAlertNotificationService
import com.thewizrd.simpleweather.widgets.WidgetUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
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

        val app = object : ApplicationLib {
            override fun getAppContext(): Context {
                return appContext.applicationContext
            }

            override fun getPreferences(): SharedPreferences {
                return PreferenceManager.getDefaultSharedPreferences(getAppContext())
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

        SimpleLibrary.initialize(app)

        // Start logger
        Logger.init(appContext)
        settingsManager = app.settingsManager
        settingsManager.loadIfNeededSync()
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
            val wm = WeatherManager.getInstance()
            settingsManager.setAPI(WeatherAPI.HERE)
            wm.updateAPI()

            val collection = withContext(Dispatchers.IO) {
                wm.getLocations("Houston, Texas")
            }
            val locs = ArrayList(collection)
            var loc = locs[0]

            // Need to get FULL location data for HERE API
            // Data provided is incomplete
            if (loc.locationLat == -1.0 && loc.locationLong == -1.0 && loc.locationTZLong == null && wm.locationProvider.needsLocationFromID()) {
                val query_vm = loc
                loc = withContext(Dispatchers.IO) {
                    wm.locationProvider.getLocationFromID(query_vm)
                }
            } else if (wm.locationProvider.needsLocationFromName()) {
                val query_vm = loc
                loc = withContext(Dispatchers.IO) {
                    wm.locationProvider.getLocationFromName(query_vm)
                }
            } else if (wm.locationProvider.needsLocationFromGeocoder()) {
                val query_vm = loc
                loc = withContext(Dispatchers.IO) {
                    wm.locationProvider.getLocation(Coordinate(query_vm.locationLat, query_vm.locationLong), query_vm.weatherSource)
                }
            }

            val locationData = LocationData(loc)
            val weather = withContext(Dispatchers.IO) {
                wm.getWeather(locationData)
            }

            Assert.assertTrue(weather?.isValid == true)
        }
    }

    @Test
    @Throws(WeatherException::class)
    fun updateLocationQueryTest() {
        runBlocking(Dispatchers.Default) {
            val wm = WeatherManager.getInstance()
            settingsManager.setAPI(WeatherAPI.HERE)
            wm.updateAPI()

            val collection = withContext(Dispatchers.IO) {
                wm.getLocations("Houston, Texas")
            }
            val locs = ArrayList(collection)
            var loc = locs[0]

            // Need to get FULL location data for HERE API
            // Data provided is incomplete
            if (loc.locationLat == -1.0 && loc.locationLong == -1.0 && loc.locationTZLong == null && wm.locationProvider.needsLocationFromID()) {
                val query_vm = loc
                loc = withContext(Dispatchers.IO) {
                    wm.locationProvider.getLocationFromID(query_vm)
                }
            } else if (wm.locationProvider.needsLocationFromName()) {
                val query_vm = loc
                loc = withContext(Dispatchers.IO) {
                    wm.locationProvider.getLocationFromName(query_vm)
                }
            } else if (wm.locationProvider.needsLocationFromGeocoder()) {
                val query_vm = loc
                loc = withContext(Dispatchers.IO) {
                    wm.locationProvider.getLocation(Coordinate(query_vm.locationLat, query_vm.locationLong), query_vm.weatherSource)
                }
            }

            val locationData = LocationData(loc)
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

            Assert.assertTrue(weather?.isValid == true)
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

            val vm = LocationQueryViewModel().apply {
                locationCountry = "US"
                locationName = "New York, NY"
                locationQuery = "11434"
                locationTZLong = "America/New_York"
            }

            WeatherAlertNotificationBuilder.createNotifications(LocationData(vm), la)

            val vm2 = LocationQueryViewModel().apply {
                locationCountry = "US"
                locationName = "New York City, NY"
                locationQuery = "10007"
                locationTZLong = "America/New_York"
            }
            WeatherAlertNotificationBuilder.createNotifications(LocationData(vm2), la)

            while (WeatherAlertNotificationService.getNotificationsCount() > 0) {
                delay(3000)
            }
        }
    }

    @Test
    @Throws(IOException::class)
    fun logCleanupTest() {
        // Context of the app under test.
        val appContext = SimpleLibrary.getInstance().appContext
        val filePath = appContext.getExternalFilesDir(null).toString() + "/logs"
        val directory = File(filePath)
        if (!directory.exists()) {
            Assert.assertTrue(directory.mkdir())
        }
        for (i in 0..3) {
            val file = File(filePath + File.separator + "Log." + i + ".log")
            Assert.assertTrue(file.createNewFile())
        }
        Assert.assertTrue(FileUtils.deleteDirectory(filePath))
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
}