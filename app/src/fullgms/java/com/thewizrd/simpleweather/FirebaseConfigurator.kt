package com.thewizrd.simpleweather

import android.annotation.SuppressLint
import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.thewizrd.shared_resources.R as sharedRes
import com.thewizrd.shared_resources.remoteconfig.remoteConfigService
import com.thewizrd.shared_resources.utils.AnalyticsProps
import com.thewizrd.shared_resources.utils.ContextUtils.isLargeTablet
import com.thewizrd.shared_resources.utils.ContextUtils.isSmallestWidth
import com.thewizrd.shared_resources.utils.ContextUtils.isTv
import com.thewizrd.shared_resources.utils.CrashlyticsLoggingTree
import com.thewizrd.shared_resources.utils.Logger

object FirebaseConfigurator {
    @SuppressLint("MissingPermission")
    fun initialize(context: Context) {
        FirebaseAnalytics.getInstance(context).run {
            setUserProperty(
                AnalyticsProps.DEVICE_TYPE, if (context.isTv()) {
                    "tv"
                } else if (context.isLargeTablet() || context.isSmallestWidth(600)) {
                    "tablet"
                } else {
                    "mobile"
                }
            )

            setUserProperty(AnalyticsProps.PLATFORM, "Android")
        }

        FirebaseCrashlytics.getInstance().run {
            isCrashlyticsCollectionEnabled = true
            sendUnsentReports()
        }
        FirebaseRemoteConfig.getInstance().setDefaultsAsync(sharedRes.xml.remote_config_defaults)

        if (!BuildConfig.DEBUG) {
            Logger.registerLogger(CrashlyticsLoggingTree())
        }

        // Receive Firebase messages
        FirebaseMessaging.getInstance().run {
            subscribeToTopic("all")
            if (BuildConfig.DEBUG) {
                FirebaseMessaging.getInstance().subscribeToTopic("debug_all")
            }
        }

        // Add Firebase RemoteConfig real-time listener
        FirebaseRemoteConfig.getInstance().run {
            addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    Logger.verbose("FirebaseConfigurator", "Remote update received")

                    remoteConfigService.checkConfig()
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Logger.error(
                        "FirebaseConfigurator",
                        message = "Error on real-time update",
                        t = error
                    )
                }
            })
        }
    }
}