package com.thewizrd.simpleweather

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thewizrd.shared_resources.AppState
import com.thewizrd.shared_resources.ApplicationLib
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.shared_resources.utils.ZoneIdCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@RunWith(AndroidJUnit4::class)
class UnitTests {
    private lateinit var context: Context
    private lateinit var app: ApplicationLib
    private var wasUsingPersonalKey = false

    @Before
    fun init() {
        // Context of the app under test.
        context = ApplicationProvider.getApplicationContext()

        app = object : ApplicationLib {
            override fun getAppContext(): Context {
                return context.applicationContext
            }

            override fun getPreferences(): SharedPreferences {
                return PreferenceManager.getDefaultSharedPreferences(appContext)
            }

            override fun registerAppSharedPreferenceListener() {}
            override fun unregisterAppSharedPreferenceListener() {}
            override fun registerAppSharedPreferenceListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}
            override fun unregisterAppSharedPreferenceListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}

            override fun getAppState(): AppState {
                return AppState.BACKGROUND
            }

            override fun isPhone(): Boolean {
                return false
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
        runBlocking {
            app.settingsManager.loadIfNeeded()
        }

        if (app.settingsManager.usePersonalKey()) {
            app.settingsManager.setPersonalKey(false)
            wasUsingPersonalKey = true
        }
    }

    @Test
    fun tzdbTest() {
        val tzLong = "Asia/Qostanay" // tzdb - 2018h

        val zId = ZoneIdCompat.of(tzLong)
        Assert.assertNotNull(zId)

        val zDT = ZonedDateTime.now(zId)
        val zStr = zDT.format(
            DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.TIMEZONE_NAME)
        )
        Log.d("tzdbtest", "DT = ${zDT.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)}")
        Log.d("tzdbtest", "Z = $zStr")

        Assert.assertTrue(zStr == "Asia/Qostanay" || zStr == "GMT+06:00")
    }
}