package com.thewizrd.simpleweather

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.thewizrd.extras.ExtrasLibrary
import com.thewizrd.shared_resources.AppState
import com.thewizrd.shared_resources.ApplicationLib
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.utils.CommonActions
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.shared_resources.utils.SettingsManager.SettingsListener
import com.thewizrd.simpleweather.receivers.CommonActionsBroadcastReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class App : Application(), ApplicationLib, ActivityLifecycleCallbacks {
    companion object {
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
        return false
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

        // Initialize settings
        settingsMgr = SettingsManager(this)
        sharedPreferenceChangeListener = SettingsListener(this)

        // Init shared library
        SimpleLibrary.initialize(this)
        ExtrasLibrary.initialize(this)

        // Start logger
        Logger.init(context)

        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(true)
            sendUnsentReports()
        }
        FirebaseAnalytics.getInstance(context).setUserProperty("device_type", "watch")
        FirebaseRemoteConfig.getInstance().setDefaultsAsync(R.xml.remote_config_defaults)

        // Init common action broadcast receiver
        registerCommonReceiver()

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
        GlobalScope.launch(Dispatchers.Default) {
            settingsMgr.loadIfNeeded()
        }
    }

    private fun registerCommonReceiver() {
        mCommonReceiver = CommonActionsBroadcastReceiver()
        val filter = IntentFilter()
        filter.addAction(CommonActions.ACTION_SETTINGS_UPDATEAPI)
        filter.addAction(CommonActions.ACTION_SETTINGS_UPDATEGPS)
        filter.addAction(CommonActions.ACTION_SETTINGS_UPDATEUNIT)
        filter.addAction(CommonActions.ACTION_SETTINGS_UPDATEDATASYNC)
        filter.addAction(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE)
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
        SimpleLibrary.unregister()
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
}