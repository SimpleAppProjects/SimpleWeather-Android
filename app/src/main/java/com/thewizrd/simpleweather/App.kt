package com.thewizrd.simpleweather

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.work.Configuration
import com.thewizrd.shared_resources.AppState
import com.thewizrd.shared_resources.ApplicationLib
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.simpleweather.extras.attachToBaseContext
import com.thewizrd.simpleweather.extras.initializeExtras
import com.thewizrd.simpleweather.receivers.CommonActionsBroadcastReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.reflect.Method
import kotlin.system.exitProcess

class App : Application(), ApplicationLib, ActivityLifecycleCallbacks, Configuration.Provider {
    companion object {
        const val HOMEIDX = 0

        @JvmStatic
        lateinit var instance: ApplicationLib
            private set
    }

    private lateinit var context: Context
    private lateinit var appProperties: Bundle
    private lateinit var settingsMgr: SettingsManager
    private lateinit var sharedPreferenceChangeListener: OnSharedPreferenceChangeListener
    private lateinit var applicationState: AppState
    private var mActivitiesStarted = 0
    private lateinit var mCommonReceiver: CommonActionsBroadcastReceiver

    private var isMainProcess = true

    override fun getAppContext(): Context {
        return context
    }

    override fun getPreferences(): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    override fun getSettingsManager(): SettingsManager {
        return settingsMgr
    }

    override fun registerAppSharedPreferenceListener() {
        registerAppSharedPreferenceListener(sharedPreferenceChangeListener)
    }

    override fun registerAppSharedPreferenceListener(listener: OnSharedPreferenceChangeListener) {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun unregisterAppSharedPreferenceListener() {
        unregisterAppSharedPreferenceListener(sharedPreferenceChangeListener)
    }

    override fun unregisterAppSharedPreferenceListener(listener: OnSharedPreferenceChangeListener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    override fun getAppState(): AppState {
        return applicationState
    }

    override fun isPhone(): Boolean {
        return true
    }

    override fun getProperties(): Bundle {
        return appProperties
    }

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()
        isMainProcess = packageName == getProcessNameCompat()
        context = if (isMainProcess) LocaleUtils.attachBaseContext(applicationContext) else applicationContext
        appProperties = Bundle()
        instance = this
        applicationState = AppState.CLOSED
        mActivitiesStarted = 0

        if (isMainProcess) {
            registerActivityLifecycleCallbacks(this)

            // Initialize settings
            settingsMgr = SettingsManager(this)
            sharedPreferenceChangeListener = SettingsManager.SettingsListener(this)

            // Init shared library
            SimpleLibrary.initialize(this)

            // Start logger
            Logger.init(context)

            // Init common action broadcast receiver
            registerCommonReceiver()

            initializeExtras(this)
        }

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                    StrictMode.ThreadPolicy.Builder()
                            .detectCustomSlowCalls()
                            .penaltyLog()
                            .build())

            val vmPolicyBuild = VmPolicy.Builder()
                    .detectActivityLeaks()
                    .detectLeakedClosableObjects()
                    .detectLeakedSqlLiteObjects()
                    .penaltyLog()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                vmPolicyBuild.detectCleartextNetwork()
            }

            StrictMode.setVmPolicy(vmPolicyBuild.build())
        }

        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            Logger.writeLine(Log.ERROR, e)
            if (oldHandler != null) {
                oldHandler.uncaughtException(t, e)
            } else {
                exitProcess(2)
            }
        }

        if (isMainProcess) {
            // Load data if needed
            GlobalScope.launch(Dispatchers.Default) {
                settingsMgr.loadIfNeeded()
            }

            when (settingsMgr.getUserThemeMode()) {
                UserThemeMode.FOLLOW_SYSTEM -> {
                    if (Build.VERSION.SDK_INT >= 29) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                    }
                }
                UserThemeMode.DARK, UserThemeMode.AMOLED_DARK -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
                UserThemeMode.LIGHT -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
                else -> {
                    if (Build.VERSION.SDK_INT >= 29) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
                    }
                }
            }

            registerAppSharedPreferenceListener()
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        attachToBaseContext(base)
    }

    private fun registerCommonReceiver() {
        mCommonReceiver = CommonActionsBroadcastReceiver()
        val filter = IntentFilter()
        filter.addAction(CommonActions.ACTION_SETTINGS_UPDATEAPI)
        filter.addAction(CommonActions.ACTION_SETTINGS_UPDATEGPS)
        filter.addAction(CommonActions.ACTION_SETTINGS_UPDATEUNIT)
        filter.addAction(CommonActions.ACTION_SETTINGS_UPDATEREFRESH)
        filter.addAction(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE)
        filter.addAction(CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE)
        filter.addAction(CommonActions.ACTION_WEATHER_UPDATEWIDGETLOCATION)
        filter.addAction(CommonActions.ACTION_WIDGET_REFRESHWIDGETS)
        filter.addAction(CommonActions.ACTION_WIDGET_RESETWIDGETS)
        filter.addAction(CommonActions.ACTION_IMAGES_UPDATEWORKER)
        filter.addAction(CommonActions.ACTION_SETTINGS_UPDATEDAILYNOTIFICATION)
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mCommonReceiver, filter)
    }

    private fun unregisterCommonReceiver() {
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mCommonReceiver)
    }

    override fun onTerminate() {
        if (isMainProcess) {
            unregisterCommonReceiver()
            unregisterActivityLifecycleCallbacks(this)
            // Shutdown logger
            Logger.shutdown()
            SimpleLibrary.unregister()
        }
        super.onTerminate()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (activity.localClassName.contains("LaunchActivity") ||
                activity.localClassName.contains("MainActivity")) {
            applicationState = AppState.FOREGROUND
        }
    }

    override fun onActivityStarted(activity: Activity) {
        if (mActivitiesStarted == 0) applicationState = AppState.FOREGROUND
        mActivitiesStarted++
    }

    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {
        mActivitiesStarted--
        if (mActivitiesStarted == 0) applicationState = AppState.BACKGROUND
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (activity.localClassName.contains("MainActivity")) {
            applicationState = AppState.CLOSED
        }
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
                .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
                .build()
    }

    private fun getProcessNameCompat(): String? {
        return if (Build.VERSION.SDK_INT >= 28) {
            getProcessName()
        } else try {
            @SuppressLint("PrivateApi")
            val activityThread = Class.forName("android.app.ActivityThread")

            val methodName = "currentProcessName"
            val getProcessName: Method = activityThread.getDeclaredMethod(methodName)
            getProcessName.invoke(null) as String?
        } catch (e: Exception) {
            null
        }
    }
}