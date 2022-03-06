package com.thewizrd.simpleweather.services

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Build
import androidx.work.*
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.simpleweather.widgets.WeatherWidgetProvider.Companion.EXTRA_WIDGET_IDS
import com.thewizrd.simpleweather.widgets.WeatherWidgetProvider.Companion.EXTRA_WIDGET_TYPE
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetType
import com.thewizrd.simpleweather.widgets.WidgetUpdaterHelper
import com.thewizrd.simpleweather.widgets.WidgetUtils

class WidgetWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {
    companion object {
        private const val KEY_ACTION = "action"

        // Widget Actions
        const val ACTION_REFRESHWIDGET = "SimpleWeather.Droid.action.REFRESH_WIDGET"
        const val ACTION_RESETGPSWIDGETS = "SimpleWeather.Droid.action.RESET_GPSWIDGETS"
        const val ACTION_REFRESHGPSWIDGETS = "SimpleWeather.Droid.action.REFRESH_GPSWIDGETS"
        const val ACTION_REFRESHWIDGETS = "SimpleWeather.Droid.action.REFRESH_WIDGETS"

        // Extras
        private const val EXTRA_LOCATIONNAME = "SimpleWeather.Droid.extra.LOCATION_NAME"
        private const val EXTRA_LOCATIONQUERY = "SimpleWeather.Droid.extra.LOCATION_QUERY"

        fun enqueueRefreshWidget(
            context: Context,
            appWidgetIds: IntArray,
            info: WidgetProviderInfo,
            expedited: Boolean = false
        ) {
            val workMgr = WorkManager.getInstance(context.applicationContext)

            val request = OneTimeWorkRequestBuilder<WidgetWorker>()
                .apply {
                    if (expedited) {
                        setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    }
                }
                .setConstraints(Constraints.NONE)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_ACTION, ACTION_REFRESHWIDGET)
                        .putIntArray(EXTRA_WIDGET_IDS, appWidgetIds)
                        .putInt(EXTRA_WIDGET_TYPE, info.widgetType.value)
                        .build()
                )
                .build()

            workMgr.enqueue(request)
        }

        fun enqueueRefreshWidgets(
            context: Context,
            location: LocationData,
            expedited: Boolean = false
        ) {
            val workMgr = WorkManager.getInstance(context.applicationContext)

            val request = OneTimeWorkRequestBuilder<WidgetWorker>()
                .apply {
                    if (expedited) {
                        setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    }
                }
                .setConstraints(Constraints.NONE)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_ACTION, ACTION_REFRESHWIDGETS)
                        .putString(EXTRA_LOCATIONQUERY, location.query)
                        .build()
                )
                .build()

            workMgr.enqueue(request)
        }

        fun enqueueResetGPSWidgets(context: Context, expedited: Boolean = false) {
            val workMgr = WorkManager.getInstance(context.applicationContext)

            val request = OneTimeWorkRequestBuilder<WidgetWorker>()
                .apply {
                    if (expedited) {
                        setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    }
                }
                .setConstraints(Constraints.NONE)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_ACTION, ACTION_RESETGPSWIDGETS)
                        .build()
                )
                .build()

            workMgr.enqueue(request)
        }

        fun enqueueRefreshGPSWidgets(context: Context, expedited: Boolean = false) {
            val workMgr = WorkManager.getInstance(context.applicationContext)

            val request = OneTimeWorkRequestBuilder<WidgetWorker>()
                .apply {
                    if (expedited) {
                        setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    }
                }
                .setConstraints(Constraints.NONE)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_ACTION, ACTION_REFRESHGPSWIDGETS)
                        .build()
                )
                .build()

            workMgr.enqueue(request)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ServiceNotificationHelper.initChannel(applicationContext)
        }

        return ForegroundInfo(
            ServiceNotificationHelper.JOB_ID,
            ServiceNotificationHelper.createForegroundNotification(applicationContext)
        )
    }

    override suspend fun doWork(): Result {
        val mAppWidgetManager = AppWidgetManager.getInstance(applicationContext)

        when (inputData.getString(KEY_ACTION)) {
            ACTION_REFRESHWIDGET -> {
                val appWidgetIds = inputData.getIntArray(EXTRA_WIDGET_IDS)!!
                val widgetType = WidgetType.valueOf(inputData.getInt(EXTRA_WIDGET_TYPE, -1))

                run {
                    val info = WidgetUtils.getWidgetProviderInfoFromType(widgetType) ?: return@run
                    WidgetUpdaterHelper.refreshWidget(
                        applicationContext,
                        info,
                        mAppWidgetManager,
                        appWidgetIds
                    )
                }
            }
            ACTION_REFRESHWIDGETS -> {
                run {
                    val locationQuery = inputData.getString(EXTRA_LOCATIONQUERY) ?: return@run
                    WidgetUpdaterHelper.refreshWidgets(applicationContext, locationQuery)
                }
            }
            ACTION_RESETGPSWIDGETS -> {
                WidgetUpdaterHelper.resetGPSWidgets(applicationContext)
            }
            ACTION_REFRESHGPSWIDGETS -> {
                WidgetUpdaterHelper.refreshWidgets(applicationContext, Constants.KEY_GPS)
            }
        }

        return Result.success()
    }
}