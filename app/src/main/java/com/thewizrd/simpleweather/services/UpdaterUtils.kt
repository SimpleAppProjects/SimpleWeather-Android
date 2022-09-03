package com.thewizrd.simpleweather.services

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.thewizrd.shared_resources.preferences.SettingsManager
import com.thewizrd.simpleweather.notifications.DailyWeatherNotificationWorker
import com.thewizrd.simpleweather.notifications.DailyWeatherNotificationWorkerActions
import com.thewizrd.simpleweather.utils.PowerUtils
import com.thewizrd.simpleweather.widgets.WidgetUpdaterHelper

class UpdaterUtils {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun startAlarm(context: Context, onBoot: Boolean = false) {
            val settingsManager = SettingsManager(context.applicationContext)
            // Enable alarm if dependent features are enabled
            if (isAlarmFeaturesEnabled(context)) {
                if (PowerUtils.useForegroundService) {
                    PowerUtils.startForegroundService(
                        context,
                        Intent(context, UpdaterTimerService::class.java)
                            .setAction(UpdaterTimerService.ACTION_STARTALARM)
                            .putExtra(
                                UpdaterTimerService.EXTRA_INTERVAL,
                                settingsManager.getRefreshInterval()
                            ),
                        onBoot
                    )
                } else {
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
                }

                if (settingsManager.isDailyNotificationEnabled()) {
                    enableDailyNotificationService(context, true, onBoot)
                }
            }
        }

        @JvmStatic
        fun cancelAlarm(context: Context) {
            // Cancel alarm if dependent features are turned off
            if (!isAlarmFeaturesEnabled(context)) {
                if (PowerUtils.useForegroundService) {
                    PowerUtils.startForegroundService(
                        context,
                        Intent(context, UpdaterTimerService::class.java)
                            .setAction(UpdaterTimerService.ACTION_CANCELALARM)
                    )
                } else {
                    WidgetUpdaterWorker.enqueueAction(
                        context,
                        WidgetUpdaterWorker.ACTION_CANCELWORK
                    )
                    WeatherUpdaterWorker.enqueueAction(
                        context,
                        WeatherUpdaterWorker.ACTION_CANCELWORK
                    )
                }
            }
        }

        @JvmStatic
        fun updateAlarm(context: Context) {
            if (PowerUtils.useForegroundService) {
                val settingsManager = SettingsManager(context.applicationContext)
                PowerUtils.startForegroundService(
                    context,
                    Intent(context, UpdaterTimerService::class.java)
                        .setAction(UpdaterTimerService.ACTION_UPDATEALARM)
                        .putExtra(
                            UpdaterTimerService.EXTRA_INTERVAL,
                            settingsManager.getRefreshInterval()
                        )
                )
            } else {
                WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_REQUEUEWORK)
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_REQUEUEWORK)
            }
        }

        @JvmStatic
        fun enableForegroundService(context: Context, enable: Boolean) {
            // NOTE For Android 12: this is always called in the foreground (fg start is allowed)
            if (enable) {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, UpdaterTimerService::class.java)
                        .setAction(UpdaterTimerService.ACTION_UPDATEALARM)
                )

                WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_CANCELWORK)
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_CANCELWORK)

                DailyWeatherNotificationWorker.cancelWork(context)
            } else {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, UpdaterTimerService::class.java)
                        .setAction(UpdaterTimerService.ACTION_CANCELALARM)
                )

                WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_REQUEUEWORK)
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_REQUEUEWORK)

                val settingsManager = SettingsManager(context.applicationContext)
                if (settingsManager.isDailyNotificationEnabled())
                    DailyWeatherNotificationWorker.scheduleNotification(context)
            }
        }

        @JvmStatic
        fun enableDailyNotificationService(
            context: Context,
            enable: Boolean,
            onBoot: Boolean = false
        ) {
            if (PowerUtils.useForegroundService) {
                if (enable) {
                    val settingsManager = SettingsManager(context.applicationContext)

                    PowerUtils.startForegroundService(
                        context, Intent(context, UpdaterTimerService::class.java)
                            .setAction(DailyWeatherNotificationWorkerActions.ACTION_UPDATENOTIFICATIONTIME)
                            .putExtra(
                                DailyWeatherNotificationWorkerActions.EXTRA_UPDATETIME,
                                settingsManager.getDailyNotificationTime()
                            )
                    )
                } else {
                    PowerUtils.startForegroundService(
                        context, Intent(context, UpdaterTimerService::class.java)
                            .setAction(DailyWeatherNotificationWorkerActions.ACTION_CANCELNOTIFICATION)
                            .putExtra(
                                DailyWeatherNotificationWorkerActions.EXTRA_UPDATETIME,
                                null as String?
                            )
                    )
                }
            } else {
                if (enable) {
                    DailyWeatherNotificationWorker.scheduleNotification(context)
                } else {
                    DailyWeatherNotificationWorker.cancelWork(context)
                }
            }

            if (!enable) cancelAlarm(context)
        }

        @JvmStatic
        fun rescheduleDailyNotificationService(context: Context) {
            if (PowerUtils.useForegroundService) {
                // NOTE For Android 12: this is always called in the foreground (fg start is allowed)
                val settingsManager = SettingsManager(context.applicationContext)
                ContextCompat.startForegroundService(
                    context, Intent(context, UpdaterTimerService::class.java)
                        .setAction(DailyWeatherNotificationWorkerActions.ACTION_UPDATENOTIFICATIONTIME)
                        .putExtra(
                            DailyWeatherNotificationWorkerActions.EXTRA_UPDATETIME,
                            settingsManager.getDailyNotificationTime()
                        )
                )
            } else {
                DailyWeatherNotificationWorker.scheduleNotification(context)
            }
        }

        private fun isAlarmFeaturesEnabled(context: Context): Boolean {
            val settingsManager = SettingsManager(context.applicationContext)
            return WidgetUpdaterHelper.widgetsExist() ||
                    settingsManager.showOngoingNotification() ||
                    settingsManager.useAlerts() ||
                    settingsManager.isDailyNotificationEnabled() ||
                    settingsManager.isPoPChanceNotificationEnabled()
        }
    }
}