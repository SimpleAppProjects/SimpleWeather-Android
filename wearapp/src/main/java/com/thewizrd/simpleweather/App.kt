package com.thewizrd.simpleweather

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.thewizrd.common.CommonModule
import com.thewizrd.common.commonModule
import com.thewizrd.common.utils.SettingsListener
import com.thewizrd.extras.extrasModule
import com.thewizrd.shared_resources.*
import com.thewizrd.shared_resources.utils.CommonActions
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.simpleweather.receivers.CommonActionsBroadcastReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class App : Application(), ActivityLifecycleCallbacks {
    private lateinit var applicationState: AppState
    private var mActivitiesStarted = 0
    private lateinit var mCommonReceiver: CommonActionsBroadcastReceiver

    override fun onCreate() {
        super.onCreate()

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
            override val isPhone = false
            override val properties = Bundle()
            override val settingsManager = SettingsManager(context)
        }

        sharedDeps = object : SharedModule() {
            override val context = applicationContext
        }

        FirebaseConfigurator.initialize(applicationContext)

        // Initialize CommonModule: Version migrations (depends on SharedModule, Firebase)
        commonModule = object : CommonModule() {
            override val context = appLib.context
        }

        registerCommonReceiver()
        extrasModule.initialize()

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
        appLib.appScope.launch(Dispatchers.Default) {
            appLib.settingsManager.loadIfNeeded()
        }

        appLib.registerAppSharedPreferenceListener()
    }

    private fun registerCommonReceiver() {
        mCommonReceiver = CommonActionsBroadcastReceiver()
        val filter = IntentFilter().apply {
            addAction(CommonActions.ACTION_SETTINGS_UPDATEAPI)
            addAction(CommonActions.ACTION_SETTINGS_UPDATEGPS)
            addAction(CommonActions.ACTION_SETTINGS_UPDATEUNIT)
            addAction(CommonActions.ACTION_SETTINGS_UPDATEDATASYNC)
            addAction(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE)
        }
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mCommonReceiver, filter)
    }

    private fun unregisterCommonReceiver() {
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mCommonReceiver)
    }

    override fun onTerminate() {
        unregisterCommonReceiver()
        appLib.appScope.cancel()
        super.onTerminate()
        unregisterActivityLifecycleCallbacks(this)
        // Shutdown logger
        Logger.shutdown()
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