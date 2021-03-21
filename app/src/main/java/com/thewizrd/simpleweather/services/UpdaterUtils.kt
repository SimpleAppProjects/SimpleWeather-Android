package com.thewizrd.simpleweather.services

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.thewizrd.shared_resources.utils.Settings
import com.thewizrd.simpleweather.utils.PowerUtils
import com.thewizrd.simpleweather.widgets.WidgetUpdaterHelper

class UpdaterUtils {
    companion object {
        @JvmStatic
        fun requestUpdateWidgets(context: Context) {
            if (PowerUtils.useForegroundService) {
                ContextCompat.startForegroundService(context, Intent(context, WeatherUpdaterService::class.java)
                        .setAction(WidgetUpdaterWorker.ACTION_UPDATEWIDGETS))
            } else {
                WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_UPDATEWIDGETS)
            }
        }

        @JvmStatic
        fun requestUpdateWeather(context: Context) {
            if (PowerUtils.useForegroundService) {
                ContextCompat.startForegroundService(context, Intent(context, WeatherUpdaterService::class.java)
                        .setAction(WeatherUpdaterWorker.ACTION_UPDATEWEATHER))
            } else {
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEWEATHER)
            }
        }

        @JvmStatic
        @JvmOverloads
        fun startAlarm(context: Context, onBoot: Boolean = false) {
            // Enable alarm if dependent features are enabled
            if (WidgetUpdaterHelper.widgetsExist() || Settings.showOngoingNotification() || Settings.useAlerts()) {
                if (PowerUtils.useForegroundService) {
                    ContextCompat.startForegroundService(context, Intent(context, WeatherUpdaterService::class.java)
                            .setAction(WeatherUpdaterService.ACTION_STARTALARM))
                } else {
                    WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_ENQUEUEWORK, onBoot)
                    WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_ENQUEUEWORK, onBoot)
                }
            }
        }

        @JvmStatic
        fun cancelAlarm(context: Context) {
            // Cancel alarm if dependent features are turned off
            if (!WidgetUpdaterHelper.widgetsExist() && !Settings.showOngoingNotification() && !Settings.useAlerts()) {
                if (PowerUtils.useForegroundService) {
                    ContextCompat.startForegroundService(context, Intent(context, WeatherUpdaterService::class.java)
                            .setAction(WeatherUpdaterService.ACTION_CANCELALARM))
                } else {
                    WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_CANCELWORK)
                    WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_CANCELWORK)
                }
            }
        }

        @JvmStatic
        fun updateAlarm(context: Context) {
            if (PowerUtils.useForegroundService) {
                ContextCompat.startForegroundService(context, Intent(context, WeatherUpdaterService::class.java)
                        .setAction(WeatherUpdaterService.ACTION_UPDATEALARM))
            } else {
                WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_REQUEUEWORK)
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_REQUEUEWORK)
            }
        }

        @JvmStatic
        fun enableForegroundService(context: Context, enable: Boolean) {
            if (enable) {
                ContextCompat.startForegroundService(context, Intent(context, WeatherUpdaterService::class.java)
                        .setAction(WeatherUpdaterService.ACTION_UPDATEALARM))

                WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_CANCELWORK)
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_CANCELWORK)
            } else {
                ContextCompat.startForegroundService(context, Intent(context, WeatherUpdaterService::class.java)
                        .setAction(WeatherUpdaterService.ACTION_CANCELALARM))

                WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_REQUEUEWORK)
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_REQUEUEWORK)
            }
        }
    }
}