package com.thewizrd.simpleweather

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.app.LocaleManager
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
import androidx.core.os.LocaleListCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import androidx.work.Configuration
import com.google.android.material.color.DynamicColors
import com.thewizrd.common.CommonModule
import com.thewizrd.common.commonModule
import com.thewizrd.common.utils.SettingsListener
import com.thewizrd.shared_resources.*
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.simpleweather.extras.attachToBaseContext
import com.thewizrd.simpleweather.extras.initializeExtras
import com.thewizrd.simpleweather.extras.initializeFirebase
import com.thewizrd.simpleweather.receivers.CommonActionsBroadcastReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.lang.reflect.Method
import kotlin.system.exitProcess

class App : Application(), ActivityLifecycleCallbacks, Configuration.Provider {
    private lateinit var applicationState: AppState
    private var mActivitiesStarted = 0
    private lateinit var mCommonReceiver: CommonActionsBroadcastReceiver

    private var isMainProcess = true

    @SuppressLint("MissingPermission")
    override fun onCreate() {
        super.onCreate()

        isMainProcess = packageName == getProcessNameCompat()
        registerActivityLifecycleCallbacks(this)
        applicationState = AppState.CLOSED
        mActivitiesStarted = 0

        // Initialize app dependencies (library module chain)
        // 1. ApplicationLib + SharedModule, 2. Firebase, 3. CommonModule, 4. ExtrasModule
        appLib = object : ApplicationLib() {
            override val context = LocaleUtils.attachBaseContext(applicationContext)
            override val preferences: SharedPreferences
                get() = PreferenceManager.getDefaultSharedPreferences(context)
            private val sharedPreferenceChangeListener by lazy { SettingsListener(appLib) }

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

            override val appState: AppState
                get() = this@App.applicationState
            override val isPhone = true
            override val properties = Bundle()
            override val settingsManager = SettingsManager(context)
        }

        if (isMainProcess) {
            sharedDeps = object : SharedModule() {
                override val context = appLib.context // keep same context as applib
            }

            initializeFirebase(applicationContext)

            // Initialize CommonModule: Version migrations (depends on SharedModule, Firebase)
            commonModule = object : CommonModule() {
                override val context = appLib.context // keep same context as applib
            }

            registerCommonReceiver()
            initializeExtras()
        }

        if (BuildConfig.DEBUG) {
            val threadPolicy = StrictMode.ThreadPolicy.Builder()
                .detectCustomSlowCalls()
                .penaltyLog()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                threadPolicy.detectResourceMismatches()
            }

            StrictMode.setThreadPolicy(threadPolicy.build())

            val vmPolicy = VmPolicy.Builder()
                .detectActivityLeaks()
                .detectLeakedClosableObjects()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                vmPolicy.detectCleartextNetwork()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                vmPolicy.detectNonSdkApiUsage()
            }

            StrictMode.setVmPolicy(vmPolicy.build())
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
            appLib.appScope.launch(Dispatchers.Default) {
                appLib.settingsManager.loadIfNeeded()
            }

            when (appLib.settingsManager.getUserThemeMode()) {
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

            appLib.registerAppSharedPreferenceListener()

            DynamicColors.applyToActivitiesIfAvailable(this)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val localeMgr = getSystemService(LocaleManager::class.java)
                val locales = localeMgr.applicationLocales
                if (!locales.isEmpty) {
                    val locale = locales.get(0)
                    if (locale != LocaleUtils.getLocale()) {
                        val tag = locale.toLanguageTag()
                        LocaleUtils.setLocaleCode(tag)
                    }
                } else {
                    LocaleUtils.setLocaleCode("")
                }
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(LocaleUtils.getLocale()))
            }
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        attachToBaseContext(base)
    }

    private fun registerCommonReceiver() {
        mCommonReceiver = CommonActionsBroadcastReceiver()
        val filter = IntentFilter().apply {
            addAction(CommonActions.ACTION_SETTINGS_UPDATEAPI)
            addAction(CommonActions.ACTION_SETTINGS_UPDATEGPS)
            addAction(CommonActions.ACTION_SETTINGS_UPDATEUNIT)
            addAction(CommonActions.ACTION_SETTINGS_UPDATEREFRESH)
            addAction(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE)
            addAction(CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE)
            addAction(CommonActions.ACTION_SETTINGS_SENDUPDATE)
            addAction(CommonActions.ACTION_WEATHER_UPDATEWIDGETLOCATION)
            addAction(CommonActions.ACTION_WIDGET_REFRESHWIDGETS)
            addAction(CommonActions.ACTION_WIDGET_RESETWIDGETS)
            addAction(CommonActions.ACTION_IMAGES_UPDATEWORKER)
            addAction(CommonActions.ACTION_SETTINGS_UPDATEDAILYNOTIFICATION)
            addAction(CommonActions.ACTION_WEATHER_LOCATIONREMOVED)
        }
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
        }
        appLib.appScope.cancel()
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
            .setDefaultProcessName(packageName) // Main process
            .build()
    }

    private fun getProcessNameCompat(): String? {
        return if (Build.VERSION.SDK_INT >= 28) {
            getProcessName()
        } else try {
            @SuppressLint("PrivateApi")
            val activityThread = Class.forName("android.app.ActivityThread")

            val methodName = "currentProcessName"

            @SuppressLint("DiscouragedPrivateApi")
            val getProcessName: Method = activityThread.getDeclaredMethod(methodName)
            getProcessName.invoke(null) as String?
        } catch (e: Exception) {
            null
        }
    }
}