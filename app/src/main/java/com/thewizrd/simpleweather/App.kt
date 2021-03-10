package com.thewizrd.simpleweather

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
import android.preference.PreferenceManager
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.Configuration
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.thewizrd.extras.ExtrasLibrary
import com.thewizrd.shared_resources.AppState
import com.thewizrd.shared_resources.ApplicationLib
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.Settings.SettingsListener
import com.thewizrd.simpleweather.receivers.CommonActionsBroadcastReceiver
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
    private var sharedPreferenceChangeListener: OnSharedPreferenceChangeListener? = null
    private lateinit var applicationState: AppState
    private var mActivitiesStarted = 0
    private lateinit var mCommonReceiver: CommonActionsBroadcastReceiver

    override fun getAppContext(): Context {
        return context
    }

    override fun getPreferences(): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    override fun getSharedPreferenceListener(): OnSharedPreferenceChangeListener {
        return sharedPreferenceChangeListener!!
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

    override fun onCreate() {
        super.onCreate()
        context = LocaleUtils.attachBaseContext(applicationContext)
        appProperties = Bundle()
        instance = this
        registerActivityLifecycleCallbacks(this)
        applicationState = AppState.CLOSED
        mActivitiesStarted = 0
        sharedPreferenceChangeListener = SettingsListener(context)

        // Init shared library
        SimpleLibrary.init(this)
        ExtrasLibrary.initialize(this)

        // Start logger
        Logger.init(context)
        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(true)
            sendUnsentReports()
        }
        FirebaseAnalytics.getInstance(context).setUserProperty("device_type", "mobile")
        FirebaseRemoteConfig.getInstance().setDefaultsAsync(R.xml.remote_config_defaults)

        // Init common action broadcast receiver
        registerCommonReceiver()

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

        // Load data if needed
        Settings.loadIfNeeded()

        when (Settings.getUserThemeMode()) {
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

        // Receive Firebase messages
        FirebaseMessaging.getInstance().subscribeToTopic("all")
        if (BuildConfig.DEBUG) {
            FirebaseMessaging.getInstance().subscribeToTopic("debug_all")
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        SplitCompat.install(this)
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
        filter.addAction(CommonActions.ACTION_WEATHER_UPDATEWIDGETWEATHER)
        filter.addAction(CommonActions.ACTION_WIDGET_REFRESHWIDGETS)
        filter.addAction(CommonActions.ACTION_WIDGET_RESETWIDGETS)
        filter.addAction(CommonActions.ACTION_IMAGES_UPDATEWORKER)
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mCommonReceiver, filter)
    }

    private fun unregisterCommonReceiver() {
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mCommonReceiver)
    }

    override fun onTerminate() {
        unregisterCommonReceiver()
        super.onTerminate()
        unregisterActivityLifecycleCallbacks(this)
        // Shutdown logger
        Logger.shutdown()
        SimpleLibrary.unRegister()
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
}