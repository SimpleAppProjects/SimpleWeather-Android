package com.thewizrd.simpleweather.services

import android.content.Context
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.simpleweather.notifications.DailyWeatherNotificationWorker
import com.thewizrd.simpleweather.widgets.WidgetUpdaterHelper

class UpdaterUtils {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun startAlarm(context: Context, onBoot: Boolean = false) {
            val settingsManager = SettingsManager(context.applicationContext)
            // Enable alarm if dependent features are enabled
            if (getAlarmFeaturesEnabled(context)) {
                WidgetUpdaterWorker.enqueueAction(
                    context,
                    WidgetUpdaterWorker.ACTION_ENQUEUEWORK,
                    onBoot
                )
                WeatherUpdaterWorker.enqueueAction(
                    context,
                    WeatherUpdaterWorker.ACTION_ENQUEUEWORK,
                    onBoot
                )

                if (settingsManager.isDailyNotificationEnabled()) {
                    enableDailyNotificationService(context, true)
                }
            }
        }

        @JvmStatic
        fun cancelAlarm(context: Context) {
            // Cancel alarm if dependent features are turned off
            if (!getAlarmFeaturesEnabled(context)) {
                WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_CANCELWORK)
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_CANCELWORK)
            }
        }

        @JvmStatic
        fun updateAlarm(context: Context) {
            WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_REQUEUEWORK)
            WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_REQUEUEWORK)
        }

        @JvmStatic
        fun enableDailyNotificationService(context: Context, enable: Boolean) {
            if (enable) {
                DailyWeatherNotificationWorker.scheduleNotification(context)
            } else {
                DailyWeatherNotificationWorker.cancelWork(context)
                cancelAlarm(context)
            }
        }

        @JvmStatic
        fun rescheduleDailyNotificationService(context: Context) {
            DailyWeatherNotificationWorker.scheduleNotification(context)
        }

        private fun getAlarmFeaturesEnabled(context: Context): Boolean {
            val settingsManager = SettingsManager(context.applicationContext)
            return WidgetUpdaterHelper.widgetsExist() ||
                    settingsManager.showOngoingNotification() ||
                    settingsManager.useAlerts() ||
                    settingsManager.isDailyNotificationEnabled() ||
                    settingsManager.isPoPChanceNotificationEnabled()
        }
    }
}