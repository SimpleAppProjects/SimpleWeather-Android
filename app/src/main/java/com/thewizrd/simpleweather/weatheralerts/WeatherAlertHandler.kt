package com.thewizrd.simpleweather.weatheralerts

import com.thewizrd.shared_resources.AppState
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlert
import com.thewizrd.simpleweather.BuildConfig
import com.thewizrd.simpleweather.notifications.WeatherAlertNotificationBuilder
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime

object WeatherAlertHandler {
    suspend fun postAlerts(location: LocationData, alerts: Collection<WeatherAlert>?) = withContext(Dispatchers.Default) {
        val wm = weatherModule.weatherManager

        if (wm.supportsAlerts() && alerts?.isNotEmpty() == true) {
            // Only alert if we're in the background
            if (BuildConfig.DEBUG || appLib.appState != AppState.FOREGROUND) {
                // Check if any of these alerts have been posted before
                // or are past the expiration date
                val now = ZonedDateTime.now()
                val unotifiedAlerts = alerts.filter {
                    BuildConfig.DEBUG || !it.isNotified && it.expiresDate.isAfter(now) && !it.date.isAfter(
                        now
                    )
                }

                // Post any un-notified alerts
                WeatherAlertNotificationBuilder.createNotifications(location, unotifiedAlerts)

                setAsNotified(location, alerts)
            }
        }
    }

    suspend fun setAsNotified(location: LocationData, alerts: Collection<WeatherAlert>?) = withContext(Dispatchers.Default) {
        if (alerts != null) {
            val now = ZonedDateTime.now()
            // Update all alerts
            for (alert in alerts) {
                if (!alert.date.isAfter(now)) {
                    alert.isNotified = true
                }
            }

            // Save alert data
            withContext(Dispatchers.IO) {
                settingsManager.saveWeatherAlerts(location, alerts)
            }
        }
    }
}