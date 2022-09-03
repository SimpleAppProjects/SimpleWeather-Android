package com.thewizrd.simpleweather.widgets.remoteviews

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.provider.AlarmClock
import android.widget.RemoteViews
import com.thewizrd.common.weatherdata.WeatherDataLoader
import com.thewizrd.common.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.helpers.toImmutableCompatFlag
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.preferences.SettingsManager
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.main.MainActivity
import com.thewizrd.simpleweather.widgets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class AbstractWidgetRemoteViewCreator(protected val context: Context) {
    protected val settingsManager = SettingsManager(context)

    protected abstract val info: WidgetProviderInfo

    abstract suspend fun buildUpdate(appWidgetId: Int, newOptions: Bundle): RemoteViews?

    abstract fun resizeWidget(
        info: WidgetProviderInfo,
        appWidgetManager: AppWidgetManager, appWidgetId: Int,
        newOptions: Bundle
    )

    protected suspend fun getLocation(appWidgetId: Int): LocationData? =
        withContext(Dispatchers.IO) {
            return@withContext if (WidgetUtils.isGPS(appWidgetId)) {
                if (!settingsManager.useFollowGPS()) {
                    WidgetUpdaterHelper.resetGPSWidgets(context, listOf(appWidgetId))
                    null
                } else {
                    settingsManager.getLastGPSLocData()
                }
            } else {
                WidgetUtils.getLocationData(appWidgetId)
            }
        }

    protected suspend fun loadWeather(
        locData: LocationData
    ): Weather? {
        return try {
            // If saved data DNE (for current location), refresh weather
            val wLoader = WeatherDataLoader(locData)
            var weather = wLoader.loadWeatherData(
                WeatherRequest.Builder()
                    .forceLoadSavedData()
                    .build()
            )

            if (weather == null) {
                weather = wLoader.loadWeatherData(
                    WeatherRequest.Builder()
                        .forceRefresh(false)
                        .loadAlerts()
                        .loadForecasts()
                        .build()
                )
            }

            weather
        } catch (e: Exception) {
            null
        }
    }

    protected suspend fun setOnClickIntent(
        location: LocationData?,
        updateViews: RemoteViews?
    ) {
        updateViews?.setOnClickPendingIntent(R.id.widget, getOnClickIntent(location))
    }

    protected suspend fun getOnClickIntent(location: LocationData?): PendingIntent {
        // When user clicks on widget, launch to WeatherNow page
        val onClickIntent = Intent(context.applicationContext, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (settingsManager.getHomeData() != location) {
            onClickIntent.putExtra(
                Constants.KEY_DATA,
                JSONParser.serializer(location, LocationData::class.java)
            )
            onClickIntent.putExtra(Constants.FRAGTAG_HOME, false)
        }

        return PendingIntent.getActivity(
            context,
            location?.hashCode()
                ?: SystemClock.uptimeMillis().toInt(),
            onClickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT.toImmutableCompatFlag()
        )
    }

    protected fun setOnSettingsClickIntent(
        updateViews: RemoteViews?,
        location: LocationData?,
        appWidgetId: Int
    ) {
        if (updateViews != null) {
            // When user clicks on widget, launch to Config activity
            val onClickIntent =
                Intent(context.applicationContext, WeatherWidgetConfigActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            if (WidgetUtils.isGPS(appWidgetId)) {
                onClickIntent.putExtra(WeatherWidgetProvider.EXTRA_LOCATIONQUERY, Constants.KEY_GPS)
            } else {
                onClickIntent.putExtra(WeatherWidgetProvider.EXTRA_LOCATIONNAME, location?.name)
                onClickIntent.putExtra(WeatherWidgetProvider.EXTRA_LOCATIONQUERY, location?.query)
            }
            onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            val clickPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                onClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT.toImmutableCompatFlag()
            )
            updateViews.setOnClickPendingIntent(R.id.settings_button, clickPendingIntent)
        }
    }

    protected fun setOnRefreshClickIntent(
        updateViews: RemoteViews?,
        appWidgetId: Int
    ) {
        if (updateViews != null) {
            val refreshIntent = Intent()
                .setComponent(info.componentName)
                .setAction(WeatherWidgetProvider.ACTION_REFRESHWIDGET)
                .putExtra(WeatherWidgetProvider.EXTRA_WIDGET_ID, appWidgetId)

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT.toImmutableCompatFlag()
            )

            updateViews.setOnClickPendingIntent(R.id.refresh_button, pendingIntent)
        }
    }

    protected fun getCalendarAppIntent(): PendingIntent {
        val componentName = WidgetUtils.getCalendarAppComponent(context)
        return if (componentName != null) {
            val launchIntent =
                context.packageManager.getLaunchIntentForPackage(componentName.packageName)
            PendingIntent.getActivity(context, 0, launchIntent, 0.toImmutableCompatFlag())
        } else {
            val onClickIntent =
                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
            PendingIntent.getActivity(context, 0, onClickIntent, 0.toImmutableCompatFlag())
        }
    }

    protected fun getClockAppIntent(): PendingIntent {
        val componentName = WidgetUtils.getClockAppComponent(context)
        return if (componentName != null) {
            val launchIntent =
                context.packageManager.getLaunchIntentForPackage(componentName.packageName)
            PendingIntent.getActivity(context, 0, launchIntent, 0.toImmutableCompatFlag())
        } else {
            val onClickIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            PendingIntent.getActivity(context, 0, onClickIntent, 0.toImmutableCompatFlag())
        }
    }
}