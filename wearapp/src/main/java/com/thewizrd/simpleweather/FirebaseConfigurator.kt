package com.thewizrd.simpleweather

import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.thewizrd.shared_resources.utils.CrashlyticsLoggingTree
import com.thewizrd.shared_resources.utils.Logger

object FirebaseConfigurator {
    @SuppressLint("MissingPermission")
    fun initialize(context: Context) {
        FirebaseAnalytics.getInstance(context).setUserProperty("device_type", "watch")

        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(true)
            sendUnsentReports()
        }
        FirebaseRemoteConfig.getInstance().setDefaultsAsync(R.xml.remote_config_defaults)

        if (!BuildConfig.DEBUG) {
            Logger.registerLogger(CrashlyticsLoggingTree())
        }
    }
}