package com.thewizrd.simpleweather

import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.thewizrd.shared_resources.utils.CrashlyticsLoggingTree
import com.thewizrd.shared_resources.utils.Logger
import timber.log.Timber

object FirebaseConfigurator {
    @SuppressLint("MissingPermission")
    fun initialize(context: Context) {
        FirebaseAnalytics.getInstance(context).setUserProperty("device_type", "mobile")

        FirebaseCrashlytics.getInstance().apply {
            setCrashlyticsCollectionEnabled(true)
            sendUnsentReports()
        }
        FirebaseRemoteConfig.getInstance().setDefaultsAsync(R.xml.remote_config_defaults)

        if (!BuildConfig.DEBUG) {
            Logger.registerLogger(CrashlyticsLoggingTree())
        }

        // Receive Firebase messages
        FirebaseMessaging.getInstance().subscribeToTopic("all")
        if (BuildConfig.DEBUG) {
            FirebaseMessaging.getInstance().subscribeToTopic("debug_all")
        }
    }
}