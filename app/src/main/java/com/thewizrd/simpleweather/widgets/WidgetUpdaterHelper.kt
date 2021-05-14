package com.thewizrd.simpleweather.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.provider.AlarmClock
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.palette.graphics.Palette
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.controls.*
import com.thewizrd.shared_resources.helpers.ColorsUtils
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.weatherdata.*
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.GlideApp
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.ImageDataViewModel
import com.thewizrd.simpleweather.controls.getBackgroundColor
import com.thewizrd.simpleweather.controls.getImageData
import com.thewizrd.simpleweather.main.MainActivity
import kotlinx.coroutines.*
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.resume

object WidgetUpdaterHelper {
    private const val TAG = "WidgetUpdaterHelper"
    private val settingsManager = App.instance.settingsManager

    @JvmStatic
    fun widgetsExist(): Boolean =
            WeatherWidgetProvider1x1.Info.getInstance().hasInstances ||
                    WeatherWidgetProvider2x2.Info.getInstance().hasInstances ||
                    WeatherWidgetProvider4x1.Info.getInstance().hasInstances ||
                    WeatherWidgetProvider4x1Google.Info.getInstance().hasInstances ||
                    WeatherWidgetProvider4x1Notification.Info.getInstance().hasInstances ||
                    WeatherWidgetProvider4x2.Info.getInstance().hasInstances ||
                    WeatherWidgetProvider4x2Clock.Info.getInstance().hasInstances ||
                    WeatherWidgetProvider4x2Huawei.Info.getInstance().hasInstances

    suspend fun refreshWidgets(context: Context) {
        coroutineScope {
            val appWidgetManager = AppWidgetManager.getInstance(context)

            launch {
                try {
                    val info = WeatherWidgetProvider1x1.Info.getInstance()
                    refreshWidget(context, info, appWidgetManager, info.appWidgetIds)
                } catch (e: Exception) {
                    //
                }
            }

            launch {
                try {
                    val info = WeatherWidgetProvider2x2.Info.getInstance()
                    refreshWidget(context, info, appWidgetManager, info.appWidgetIds)
                } catch (e: Exception) {
                    //
                }
            }

            launch {
                try {
                    val info = WeatherWidgetProvider4x1.Info.getInstance()
                    refreshWidget(context, info, appWidgetManager, info.appWidgetIds)
                } catch (e: Exception) {
                    //
                }
            }

            launch {
                try {
                    val info = WeatherWidgetProvider4x1Google.Info.getInstance()
                    refreshWidget(context, info, appWidgetManager, info.appWidgetIds)
                } catch (e: Exception) {
                    //
                }
            }

            launch {
                try {
                    val info = WeatherWidgetProvider4x1Notification.Info.getInstance()
                    refreshWidget(context, info, appWidgetManager, info.appWidgetIds)
                } catch (e: Exception) {
                    //
                }
            }

            launch {
                try {
                    val info = WeatherWidgetProvider4x2.Info.getInstance()
                    refreshWidget(context, info, appWidgetManager, info.appWidgetIds)
                } catch (e: Exception) {
                    //
                }
            }

            launch {
                try {
                    val info = WeatherWidgetProvider4x2Clock.Info.getInstance()
                    refreshWidget(context, info, appWidgetManager, info.appWidgetIds)
                } catch (e: Exception) {
                    //
                }
            }

            launch {
                try {
                    val info = WeatherWidgetProvider4x2Huawei.Info.getInstance()
                    refreshWidget(context, info, appWidgetManager, info.appWidgetIds)
                } catch (e: Exception) {
                    //
                }
            }
        }
    }

    private fun resetWidget(context: Context, appWidgetId: Int, appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)) {
        val views = RemoteViews(context.packageName, R.layout.app_widget_configure_layout)

        val configureIntent = Intent(context, WeatherWidgetConfigActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        configureIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

        val clickPendingIntent = PendingIntent.getActivity(context, appWidgetId, configureIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        views.setOnClickPendingIntent(R.id.widget, clickPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    suspend fun resetGPSWidgets(context: Context) {
        val appWidgetIds = WidgetUtils.getWidgetIds(Constants.KEY_GPS)
        resetGPSWidgets(context, appWidgetIds)
    }

    private suspend fun resetGPSWidgets(context: Context, appWidgetIds: List<Int>) {
        coroutineScope {
            val appWidgetManager = AppWidgetManager.getInstance(context)

            for (appWidgetId in appWidgetIds) {
                launch(Dispatchers.Default) {
                    resetWidget(context, appWidgetId, appWidgetManager)
                }
            }
        }
    }

    suspend fun refreshWidgets(context: Context, location_query: String) {
        coroutineScope {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = WidgetUtils.getWidgetIds(location_query)

            for (appWidgetId in appWidgetIds) {
                launch(Dispatchers.Default) {
                    val widgetType = WidgetUtils.getWidgetTypeFromID(appWidgetId)
                    val info = WidgetUtils.getWidgetProviderInfoFromType(widgetType)
                            ?: return@launch

                    refreshWidget(context, info, appWidgetManager, IntArray(1) { appWidgetId })
                }
            }
        }
    }

    suspend fun refreshWidget(context: Context, info: WidgetProviderInfo, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val locData = getLocation(context, appWidgetId)
            if (locData != null) {
                val weather = getWeather(context, info, appWidgetId, locData)
                if (weather != null) {
                    val viewModel = WeatherNowViewModel(weather)
                    val newOptions = appWidgetManager.getAppWidgetOptions(appWidgetId)

                    // Build the widget update for provider
                    val views = buildUpdate(context, info, appWidgetId, locData, viewModel, newOptions)

                    buildExtras(context, info, locData, weather, views, appWidgetId, newOptions)

                    // Push update for this widget to the home screen
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } else {
                Logger.writeLine(Log.DEBUG, "%s: provider: %s; widgetId: %d; Unable to find location data", TAG, info.className, appWidgetId)
                resetWidget(context, appWidgetId, appWidgetManager)
            }
        }
    }

    internal suspend fun buildUpdate(context: Context, info: WidgetProviderInfo,
                                     appWidgetId: Int, location: LocationData,
                                     weather: WeatherNowViewModel, newOptions: Bundle,
                                     loadBackground: Boolean = true): RemoteViews {
        // Build an update that holds the updated widget contents
        val updateViews = RemoteViews(context.packageName, info.widgetLayoutId)

        // Widget dimensions
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val cellHeight = WidgetUtils.getCellsForSize(minHeight)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)

        val background = WidgetUtils.getWidgetBackground(appWidgetId)
        var style: WidgetUtils.WidgetBackgroundStyle? = null

        if (WidgetUtils.isBackgroundOptionalWidget(info.widgetType)) {
            val backgroundColor = WidgetUtils.getBackgroundColor(appWidgetId, background)

            if (background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
                style = WidgetUtils.getBackgroundStyle(appWidgetId)
                var imageData: ImageDataViewModel? = null

                if (loadBackground) {
                    imageData = weather.getImageData()
                }

                updateViews.removeAllViews(R.id.panda_container)
                updateViews.addView(R.id.panda_container, RemoteViews(context.packageName, R.layout.layout_panda_bg))

                if (style == WidgetUtils.WidgetBackgroundStyle.PANDA) {
                    // No-op
                } else if (style == WidgetUtils.WidgetBackgroundStyle.PENDINGCOLOR) {
                    updateViews.setImageViewResource(R.id.panda_background, R.drawable.widget_background)
                    updateViews.setInt(R.id.panda_background, "setColorFilter", imageData?.color
                                                                                ?: weather.getBackgroundColor())
                } else if (style == WidgetUtils.WidgetBackgroundStyle.LIGHT) {
                    updateViews.setImageViewResource(R.id.panda_background, R.drawable.widget_background)
                    updateViews.setInt(R.id.panda_background, "setColorFilter", Colors.WHITE)
                } else if (style == WidgetUtils.WidgetBackgroundStyle.DARK) {
                    updateViews.setImageViewResource(R.id.panda_background, R.drawable.widget_background)
                    updateViews.setInt(R.id.panda_background, "setColorFilter", Colors.BLACK)
                } else {
                    updateViews.removeAllViews(R.id.panda_container)
                }

                updateViews.setInt(R.id.widgetBackground, "setColorFilter", backgroundColor)
                updateViews.setInt(R.id.widgetBackground, "setImageAlpha", 0xFF)

                if (loadBackground) {
                    loadBackgroundImage(context, updateViews, info, appWidgetId, imageData?.imageURI, cellWidth, cellHeight)
                } else {
                    updateViews.setImageViewBitmap(R.id.widgetBackground, null)
                }
            } else if (background == WidgetUtils.WidgetBackground.TRANSPARENT) {
                updateViews.setImageViewResource(R.id.widgetBackground, R.drawable.widget_background)
                updateViews.setInt(R.id.widgetBackground, "setColorFilter", Colors.BLACK)
                updateViews.setInt(R.id.widgetBackground, "setImageAlpha", 0x00)
                updateViews.setInt(R.id.panda_background, "setColorFilter", Colors.TRANSPARENT)
                updateViews.setImageViewBitmap(R.id.panda_background, null)

                if (info.widgetType == WidgetType.Widget4x2) {
                    updateViews.setViewVisibility(R.id.weather_icon_overlay, View.GONE)
                }
            } else {
                updateViews.setImageViewBitmap(R.id.widgetBackground, ImageUtils.createColorBitmap(backgroundColor))
                updateViews.setInt(R.id.widgetBackground, "setColorFilter", Colors.TRANSPARENT)
                updateViews.setInt(R.id.widgetBackground, "setImageAlpha", 0xFF)
                updateViews.setInt(R.id.panda_background, "setColorFilter", Colors.TRANSPARENT)
                updateViews.setImageViewBitmap(R.id.panda_background, null)

                if (info.widgetType == WidgetType.Widget4x2) {
                    updateViews.setViewVisibility(R.id.weather_icon_overlay, View.GONE)
                }
            }
        }

        if (info.widgetType == WidgetType.Widget2x2) {
            updateViews.removeAllViews(R.id.weather_notif_layout)

            if (style != WidgetUtils.WidgetBackgroundStyle.PANDA) {
                updateViews.addView(R.id.weather_notif_layout, RemoteViews(context.packageName, R.layout.app_widget_2x2_notif_layout))
            } else {
                updateViews.addView(R.id.weather_notif_layout, RemoteViews(context.packageName, R.layout.app_widget_2x2_notif_layout_themed))
            }
        }

        // Colors
        setTextColorDependents(context, updateViews, info, appWidgetId, weather, background, style)

        // Location Name
        updateViews.setTextViewText(R.id.location_name, weather.location)

        // Set specific data for widgets
        if (info.widgetType == WidgetType.Widget2x2 || info.widgetType == WidgetType.Widget4x1Notification) {
            // Condition text
            updateViews.setTextViewText(R.id.condition_weather, String.format(Locale.ROOT, "%s - %s",
                    if (weather.curTemp?.isNotBlank() == true) weather.curTemp else WeatherIcons.PLACEHOLDER,
                    weather.curCondition))
            updateViews.setTextViewText(R.id.condition_hi, if (weather.hiTemp?.isNotBlank() == true) weather.hiTemp else WeatherIcons.PLACEHOLDER)
            updateViews.setTextViewText(R.id.condition_lo, if (weather.loTemp?.isNotBlank() == true) weather.loTemp else WeatherIcons.PLACEHOLDER)
            updateViews.setViewVisibility(R.id.condition_hilo_layout, if (weather.isShowHiLo) View.VISIBLE else View.GONE)

            var chanceModel: DetailItemViewModel? = null
            var windModel: DetailItemViewModel? = null
            for (input in weather.getWeatherDetails()) {
                if (input.detailsType == WeatherDetailsType.POPCHANCE) {
                    chanceModel = input
                } else if (chanceModel == null && input.detailsType == WeatherDetailsType.POPCLOUDINESS) {
                    chanceModel = input
                } else if (input.detailsType == WeatherDetailsType.WINDSPEED) {
                    windModel = input
                }
                if (chanceModel != null && windModel != null) {
                    break
                }
            }

            if (chanceModel != null) {
                updateViews.setTextViewText(R.id.weather_pop, chanceModel.value)
                updateViews.setViewVisibility(R.id.weather_pop_layout, View.VISIBLE)
            } else {
                updateViews.setViewVisibility(R.id.weather_pop_layout, View.GONE)
            }

            if (windModel != null) {
                var speed = if (TextUtils.isEmpty(windModel.value)) "" else windModel.value.toString()
                speed = speed.split(",").toTypedArray()[0]
                updateViews.setTextViewText(R.id.weather_windspeed, speed)
                updateViews.setViewVisibility(R.id.weather_wind_layout, View.VISIBLE)
            } else {
                updateViews.setViewVisibility(R.id.weather_wind_layout, View.GONE)
            }

            updateViews.setViewVisibility(R.id.extra_layout, if (chanceModel != null || windModel != null) View.VISIBLE else View.GONE)
        } else if (info.widgetType == WidgetType.Widget4x2 || info.widgetType == WidgetType.Widget4x2Clock) {
            // Condition text
            updateViews.setTextViewText(R.id.condition_weather, weather.curCondition)
        } else if (info.widgetType == WidgetType.Widget4x2Huawei) {
            // Condition text
            updateViews.setTextViewText(R.id.condition_hilo, String.format(Locale.ROOT, "%s | %s",
                    if (weather.hiTemp?.isNotBlank() == true) weather.hiTemp else WeatherIcons.PLACEHOLDER,
                    if (weather.loTemp?.isNotBlank() == true) weather.loTemp else WeatherIcons.PLACEHOLDER))
            updateViews.setViewVisibility(R.id.condition_hilo, if (weather.isShowHiLo) View.VISIBLE else View.GONE)
        }

        if (info.widgetType != WidgetType.Widget2x2 && info.widgetType != WidgetType.Widget4x1Notification) {
            updateViews.setTextViewText(R.id.condition_temp, weather.curTemp)
        }

        // Set sizes for views
        updateViewSizes(context, info, updateViews, newOptions)

        if (WidgetUtils.isDateWidget(info.widgetType)) {
            // Open default clock/calendar app
            updateViews.setOnClickPendingIntent(R.id.date_panel, getCalendarAppIntent(context))
        }
        if (WidgetUtils.isClockWidget(info.widgetType)) {
            // Open default clock/calendar app
            updateViews.setOnClickPendingIntent(R.id.clock_panel, getClockAppIntent(context))
        }

        updateViews.setViewVisibility(R.id.location_name, if (WidgetUtils.isLocationNameHidden(appWidgetId)) View.GONE else View.VISIBLE)
        updateViews.setViewVisibility(R.id.settings_button, if (WidgetUtils.isSettingsButtonHidden(appWidgetId)) View.GONE else View.VISIBLE)

        setOnClickIntent(context, location, updateViews)
        setOnSettingsClickIntent(context, updateViews, location, appWidgetId)

        return updateViews
    }

    suspend fun getLocation(context: Context, appWidgetId: Int): LocationData? = withContext(Dispatchers.IO) {
        return@withContext if (WidgetUtils.isGPS(appWidgetId)) {
            if (!settingsManager.useFollowGPS()) {
                resetGPSWidgets(context, listOf(appWidgetId))
                null
            } else {
                settingsManager.getLastGPSLocData()
            }
        } else {
            WidgetUtils.getLocationData(appWidgetId)
        }
    }

    private suspend fun loadWeather(info: WidgetProviderInfo, locData: LocationData?, appWidgetId: Int): Weather? {
        if (locData != null) {
            return try {
                WeatherDataLoader(locData)
                        .loadWeatherData(WeatherRequest.Builder()
                                .forceLoadSavedData()
                                .build())
            } catch (e: Exception) {
                null
            }
        } else {
            Logger.writeLine(Log.DEBUG, "%s: provider: %s; widgetId: %d; Unable to find location data", TAG, info.className, appWidgetId)
        }

        return null
    }

    private suspend fun getWeather(context: Context, info: WidgetProviderInfo, appWidgetId: Int,
                                   locData: LocationData?): Weather? = withContext(Dispatchers.IO) {
        return@withContext if (locData == null) {
            loadWeather(info, getLocation(context, appWidgetId), appWidgetId)
        } else {
            loadWeather(info, locData, appWidgetId)
        }
    }

    internal fun buildExtras(context: Context, info: WidgetProviderInfo,
                             locData: LocationData, weather: Weather,
                             views: RemoteViews, appWidgetId: Int, newOptions: Bundle) {
        if (WidgetUtils.isForecastWidget(info.widgetType)) {
            buildForecast(context, info, views, appWidgetId,
                    locData, weather, newOptions)
        }

        if (WidgetUtils.isDateWidget(info.widgetType)) {
            buildDate(context, info, locData, views, appWidgetId, newOptions)
        }

        if (WidgetUtils.isClockWidget(info.widgetType)) {
            buildClock(context, info, locData, views, appWidgetId, newOptions)
        }
    }

    private fun resizeExtras(context: Context, info: WidgetProviderInfo,
                             views: RemoteViews, appWidgetId: Int, newOptions: Bundle) {
        if (WidgetUtils.isForecastWidget(info.widgetType)) {
            updateForecastSizes(context, info, appWidgetId, views, newOptions)
        }

        if (WidgetUtils.isDateWidget(info.widgetType)) {
            updateDateSize(context, info, views, appWidgetId, newOptions)
        }

        if (WidgetUtils.isClockWidget(info.widgetType)) {
            updateClockSize(context, info, views, appWidgetId, newOptions)
        }
    }

    private fun buildForecast(context: Context, info: WidgetProviderInfo,
                              updateViews: RemoteViews, appWidgetId: Int,
                              locData: LocationData, weather: Weather?,
                              newOptions: Bundle) {
        updateViews.removeAllViews(R.id.forecast_layout)

        // Widget dimensions
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)

        // Determine forecast size
        val forecastLength = WidgetUtils.getForecastLength(info.widgetType, cellWidth)
        buildForecastPanel(context, info, updateViews, appWidgetId, forecastLength,
                locData, weather, newOptions)
    }

    private fun buildForecastPanel(context: Context, info: WidgetProviderInfo,
                                   updateViews: RemoteViews, appWidgetId: Int,
                                   forecastLength: Int,
                                   locData: LocationData, weather: Weather?,
                                   newOptions: Bundle) {
        if (WidgetUtils.isForecastWidget(info.widgetType)) {
            // Background & Text Color
            val background = WidgetUtils.getWidgetBackground(appWidgetId)
            val style = WidgetUtils.getBackgroundStyle(appWidgetId)
            val textColor = WidgetUtils.getPanelTextColor(appWidgetId, background, style, context.isNightMode())

            var forecastPanel: RemoteViews? = null
            var hrForecastPanel: RemoteViews? = null

            val forecasts = getForecasts(locData, weather?.forecast, forecastLength)
            val hourlyForecasts = getHourlyForecasts(locData, weather?.hrForecast, forecastLength)
            val forecastOption = WidgetUtils.getForecastOption(appWidgetId)

            val forecastLayoutId: Int
            val hrForecastLayoutId: Int
            if (info.widgetType == WidgetType.Widget4x1 || style != WidgetUtils.WidgetBackgroundStyle.PANDA) {
                forecastLayoutId = R.layout.app_widget_forecast_layout_container
                hrForecastLayoutId = R.layout.app_widget_hrforecast_layout_container
            } else {
                forecastLayoutId = R.layout.app_widget_forecast_layout_container_themed
                hrForecastLayoutId = R.layout.app_widget_hrforecast_layout_container_themed
            }

            if (forecastOption == WidgetUtils.ForecastOption.DAILY) {
                forecastPanel = RemoteViews(context.packageName, forecastLayoutId)
            } else if (forecastOption == WidgetUtils.ForecastOption.HOURLY) {
                if (hourlyForecasts.isNotEmpty()) {
                    hrForecastPanel = RemoteViews(context.packageName, hrForecastLayoutId)
                }
            } else {
                forecastPanel = RemoteViews(context.packageName, forecastLayoutId)
                if (hourlyForecasts.isNotEmpty()) {
                    hrForecastPanel = RemoteViews(context.packageName, hrForecastLayoutId)
                }
            }

            for (i in 0 until forecastLength) {
                if (forecastPanel != null && i < forecasts.size) {
                    setForecastItem(context, info, forecastPanel, background, style, forecasts[i], i, textColor)
                }

                if (hrForecastPanel != null && i < hourlyForecasts.size) {
                    setForecastItem(context, info, hrForecastPanel, background, style, hourlyForecasts[i], i, textColor)
                }

                if (i >= forecasts.size && i >= hourlyForecasts.size)
                    break
            }

            if (forecastPanel != null) {
                updateViews.addView(R.id.forecast_layout, forecastPanel)
            }
            if (hrForecastPanel != null) {
                updateViews.addView(R.id.forecast_layout, hrForecastPanel)
            }

            updateForecastSizes(context, info, appWidgetId, updateViews, newOptions)

            if (forecastPanel != null && hrForecastPanel != null && WidgetUtils.isTap2Switch(appWidgetId)) {
                updateViews.setOnClickPendingIntent(R.id.forecast_layout,
                        getShowNextIntent(context, info, appWidgetId))
            } else {
                updateViews.setOnClickPendingIntent(R.id.forecast_layout,
                        getOnClickIntent(context, WidgetUtils.getLocationData(appWidgetId)))
            }
        }
    }

    private fun setForecastItem(context: Context, info: WidgetProviderInfo,
                                forecastPanel: RemoteViews,
                                background: WidgetUtils.WidgetBackground,
                                style: WidgetUtils.WidgetBackgroundStyle,
                                forecast: BaseForecastItemViewModel,
                                forecastIdx: Int,
                                textColor: Int) {
        val prefix = if (forecast is HourlyForecastItemViewModel) "hrforecast" else "forecast"

        val viewId = getResIdentifier(R.id::class.java, "${prefix}${forecastIdx + 1}") ?: return
        val dateId = getResIdentifier(R.id::class.java, "${prefix}${forecastIdx + 1}_date")
                ?: return
        val hiId = getResIdentifier(R.id::class.java, "${prefix}${forecastIdx + 1}_hi") ?: return
        val iconId = getResIdentifier(R.id::class.java, "${prefix}${forecastIdx + 1}_icon")
                ?: return
        val loId = getResIdentifier(R.id::class.java, "${prefix}${forecastIdx + 1}_lo") ?: return
        val dividerId = getResIdentifier(R.id::class.java, "${prefix}${forecastIdx + 1}_divider")
                ?: return

        forecastPanel.setTextViewText(dateId, forecast.shortDate)
        forecastPanel.setTextViewText(hiId, forecast.hiTemp)
        if (forecast is ForecastItemViewModel) {
            forecastPanel.setTextViewText(loId, forecast.loTemp)
        }

        if (background != WidgetUtils.WidgetBackground.CURRENT_CONDITIONS || style != WidgetUtils.WidgetBackgroundStyle.PANDA) {
            forecastPanel.setTextColor(dateId, textColor)
            forecastPanel.setTextColor(hiId, textColor)
            if (forecast is ForecastItemViewModel) {
                forecastPanel.setTextColor(dividerId, textColor)
                forecastPanel.setTextColor(loId, textColor)
            }
        }

        // WeatherIcon
        if (!WidgetUtils.isBackgroundOptionalWidget(info.widgetType)) {
            forecastPanel.setImageViewBitmap(iconId,
                    ImageUtils.bitmapFromDrawable(ContextUtils.getThemeContextOverride(context, false), forecast.weatherIcon))
        } else if (background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS && style == WidgetUtils.WidgetBackgroundStyle.PANDA) {
            forecastPanel.setImageViewResource(iconId, forecast.weatherIcon)
        } else {
            forecastPanel.setImageViewBitmap(iconId,
                    ImageUtils.bitmapFromDrawable(ContextUtils.getThemeContextOverride(context, style == WidgetUtils.WidgetBackgroundStyle.LIGHT), forecast.weatherIcon))
        }

        if (forecast is HourlyForecastItemViewModel) {
            forecastPanel.setViewVisibility(dividerId, View.GONE)
            forecastPanel.setViewVisibility(loId, View.GONE)
        }

        forecastPanel.setViewVisibility(viewId, View.VISIBLE)
    }

    private fun updateForecastSizes(context: Context, info: WidgetProviderInfo,
                                    appWidgetId: Int,
                                    views: RemoteViews, newOptions: Bundle) {
        // Widget dimensions
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        val maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val maxCellHeight = WidgetUtils.getCellsForSize(maxHeight)
        val maxCellWidth = WidgetUtils.getCellsForSize(maxWidth)
        val cellHeight = WidgetUtils.getCellsForSize(minHeight)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)
        val forceSmallWidth = cellWidth == maxCellWidth
        val forceSmallHeight = cellHeight == maxCellHeight
        val isSmallHeight = maxCellHeight.toFloat() / cellHeight <= 1.5f
        val isSmallWidth = maxCellWidth.toFloat() / cellWidth <= 1.5f

        val forecastLength = WidgetUtils.getForecastLength(info.widgetType, cellWidth)

        var maxIconSize = ContextUtils.dpToPx(context, 40f).toInt()
        if (info.widgetType == WidgetType.Widget4x1) {
            if (WidgetUtils.isLocationNameHidden(appWidgetId) && WidgetUtils.isSettingsButtonHidden(appWidgetId)) {
                maxIconSize = (maxIconSize * 6 / 5f).toInt() // 48dp
            }
        }

        var datePadding = 0
        var textSize = 12f
        if (info.widgetType === WidgetType.Widget4x1) {
            datePadding = if (cellHeight <= 1) {
                0
            } else {
                ContextUtils.dpToPx(context, 2f).toInt()
            }

            if (cellHeight > 1 && (!isSmallWidth || cellWidth > 4)) textSize = 14f
        } else {
            if (!isSmallHeight && cellHeight > 2 && (!isSmallWidth || cellWidth > 4)) textSize = 14f
        }

        for (i in 0 until forecastLength) {
            val fcastDateId = getResIdentifier(R.id::class.java, "forecast${i + 1}_date") ?: break
            val fcastHiId = getResIdentifier(R.id::class.java, "forecast${i + 1}_hi") ?: break
            val fcastIconId = getResIdentifier(R.id::class.java, "forecast${i + 1}_icon") ?: break
            val fcastLoId = getResIdentifier(R.id::class.java, "forecast${i + 1}_lo") ?: break
            val fcastDividerId = getResIdentifier(R.id::class.java, "forecast${i + 1}_divider")
                    ?: break

            val hrfcastDateId = getResIdentifier(R.id::class.java, "hrforecast${i + 1}_date")
                    ?: break
            val hrfcastHiId = getResIdentifier(R.id::class.java, "hrforecast${i + 1}_hi") ?: break
            val hrfcastIconId = getResIdentifier(R.id::class.java, "hrforecast${i + 1}_icon")
                    ?: break

            views.setInt(fcastIconId, "setMaxWidth", maxIconSize)
            views.setInt(fcastIconId, "setMaxHeight", maxIconSize)

            views.setInt(hrfcastIconId, "setMaxWidth", maxIconSize)
            views.setInt(hrfcastIconId, "setMaxHeight", maxIconSize)

            views.setViewPadding(fcastDateId, datePadding, datePadding, datePadding, datePadding)
            views.setViewPadding(hrfcastDateId, datePadding, datePadding, datePadding, datePadding)

            views.setTextViewTextSize(fcastDateId, TypedValue.COMPLEX_UNIT_SP, textSize)
            views.setTextViewTextSize(fcastHiId, TypedValue.COMPLEX_UNIT_SP, textSize)
            views.setTextViewTextSize(fcastDividerId, TypedValue.COMPLEX_UNIT_SP, textSize)
            views.setTextViewTextSize(fcastLoId, TypedValue.COMPLEX_UNIT_SP, textSize)

            views.setTextViewTextSize(hrfcastDateId, TypedValue.COMPLEX_UNIT_SP, textSize)
            views.setTextViewTextSize(hrfcastHiId, TypedValue.COMPLEX_UNIT_SP, textSize)
        }
    }

    private fun updateViewSizes(context: Context, info: WidgetProviderInfo, updateViews: RemoteViews, newOptions: Bundle) {
        // Widget dimensions
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        val maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val maxCellHeight = WidgetUtils.getCellsForSize(maxHeight)
        val maxCellWidth = WidgetUtils.getCellsForSize(maxWidth)
        val cellHeight = WidgetUtils.getCellsForSize(minHeight)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)
        val forceSmallHeight = cellHeight == maxCellHeight
        val isSmallHeight = maxCellHeight.toFloat() / cellHeight <= 1.5f
        val isSmallWidth = maxCellWidth.toFloat() / cellWidth <= 1.5f

        if (info.widgetType == WidgetType.Widget1x1) {
            if (cellWidth > 1 && cellHeight > 1) {
                updateViews.setTextViewTextSize(R.id.location_name, TypedValue.COMPLEX_UNIT_SP, 14f)
            } else {
                updateViews.setTextViewTextSize(R.id.location_name, TypedValue.COMPLEX_UNIT_SP, 12f)
            }
            if (cellWidth > 2 && cellHeight > 2) {
                updateViews.setTextViewTextSize(R.id.condition_temp, TypedValue.COMPLEX_UNIT_SP, 24f)
            } else if (cellWidth > 1 && cellHeight > 1) {
                updateViews.setTextViewTextSize(R.id.condition_temp, TypedValue.COMPLEX_UNIT_SP, 18f)
            } else {
                updateViews.setTextViewTextSize(R.id.condition_temp, TypedValue.COMPLEX_UNIT_SP, 16f)
            }
        } else if (info.widgetType == WidgetType.Widget4x1Google) {
            var forceSmall = false
            var textSize: Float = context.resources.getDimensionPixelSize(R.dimen.widget4x1G_text_size).toFloat() // 24sp
            if (cellWidth <= 3) {
                textSize *= (2f / 3) // 16sp
            } else if (isSmallHeight && cellHeight == 1) {
                textSize *= (5 / 6f) // 20sp
                forceSmall = true
            } else if (cellWidth == 4) {
                textSize *= 0.75f // 18sp
            }

            val layoutPadding = ContextUtils.dpToPx(context, if (forceSmall) 0f else 12f).toInt()
            updateViews.setViewPadding(R.id.layout_container, layoutPadding, layoutPadding, layoutPadding, layoutPadding)

            updateViews.setTextViewTextSize(R.id.date_panel, TypedValue.COMPLEX_UNIT_PX, textSize)
            updateViews.setTextViewTextSize(R.id.condition_temp, TypedValue.COMPLEX_UNIT_PX, textSize)
            updateViews.setTextViewTextSize(R.id.location_name, TypedValue.COMPLEX_UNIT_SP, if (forceSmall) 12f else 14f)
        } else if (info.widgetType == WidgetType.Widget4x2) {
            val maxHeightSize = ContextUtils.dpToPx(context, 60f).toInt()

            if (isSmallHeight && cellHeight <= 2 || cellWidth < 4) {
                val iconWidth = ContextUtils.dpToPx(context, 45f).toInt()
                updateViews.setInt(R.id.weather_icon, "setMaxWidth", iconWidth)
                updateViews.setInt(R.id.weather_icon, "setMaxHeight", maxHeightSize)
            } else {
                val iconWidth = ContextUtils.dpToPx(context, 55f).toInt()
                updateViews.setInt(R.id.weather_icon, "setMaxWidth", iconWidth)
                updateViews.setInt(R.id.weather_icon, "setMaxHeight", (maxHeightSize * 7f / 6).toInt()) // 70dp
            }

            var textSize = ContextUtils.dpToPx(context, 36f)
            if (isSmallHeight && cellHeight <= 2 || cellWidth < 4) textSize = ContextUtils.dpToPx(context, 28f)

            updateViews.setTextViewTextSize(R.id.condition_temp, TypedValue.COMPLEX_UNIT_PX, textSize)
            updateViews.setViewVisibility(R.id.condition_weather, if (forceSmallHeight && cellHeight <= 2) View.GONE else View.VISIBLE)
        } else if (info.widgetType == WidgetType.Widget4x1) {
            var locTextSize = 12
            if (cellHeight > 1 && (!isSmallWidth || cellWidth > 4)) locTextSize = 14

            updateViews.setTextViewTextSize(R.id.location_name, TypedValue.COMPLEX_UNIT_SP, locTextSize.toFloat())

            if (isSmallHeight && cellHeight == 1) {
                val padding = ContextUtils.dpToPx(context, 0f).toInt()
                updateViews.setViewPadding(R.id.layout_container, padding, padding, padding, padding)
            } else {
                val padding = ContextUtils.dpToPx(context, 8f).toInt()
                updateViews.setViewPadding(R.id.layout_container, padding, padding, padding, padding)
            }
        } else if (info.widgetType == WidgetType.Widget2x2 || info.widgetType == WidgetType.Widget4x1Notification) {
            val largeText = cellHeight > 2 || info.widgetType == WidgetType.Widget4x1Notification
            updateViews.setTextViewTextSize(R.id.location_name, TypedValue.COMPLEX_UNIT_SP, if (largeText) 16f else 14f)
            updateViews.setTextViewTextSize(R.id.condition_weather, TypedValue.COMPLEX_UNIT_SP, if (largeText) 15f else 13f)
            updateViews.setTextViewTextSize(R.id.condition_hi, TypedValue.COMPLEX_UNIT_SP, if (largeText) 14f else 12f)
            updateViews.setTextViewTextSize(R.id.divider, TypedValue.COMPLEX_UNIT_SP, if (largeText) 14f else 12f)
            updateViews.setTextViewTextSize(R.id.condition_lo, TypedValue.COMPLEX_UNIT_SP, if (largeText) 14f else 12f)
            updateViews.setTextViewTextSize(R.id.weather_pop, TypedValue.COMPLEX_UNIT_SP, if (largeText) 14f else 12f)
            updateViews.setTextViewTextSize(R.id.weather_windspeed, TypedValue.COMPLEX_UNIT_SP, if (largeText) 14f else 12f)
            updateViews.setViewVisibility(R.id.extra_layout, if (cellWidth <= 3) View.GONE else View.VISIBLE)
        } else if (info.widgetType == WidgetType.Widget4x2Clock) {
            updateViews.setViewVisibility(R.id.spacer_left, if (cellWidth <= 3) View.GONE else View.VISIBLE)
            updateViews.setViewVisibility(R.id.spacer_right, if (cellWidth <= 3) View.GONE else View.VISIBLE)
            updateViews.setTextViewTextSize(R.id.condition_temp, TypedValue.COMPLEX_UNIT_SP, if (cellWidth <= 3) 28f else 36f)
        }
    }

    private fun setTextColorDependents(context: Context, updateViews: RemoteViews, info: WidgetProviderInfo, appWidgetId: Int,
                                       weather: WeatherNowViewModel, background: WidgetUtils.WidgetBackground, style: WidgetUtils.WidgetBackgroundStyle?) {
        val textColor = WidgetUtils.getTextColor(appWidgetId, background)
        val panelTextColor = WidgetUtils.getPanelTextColor(appWidgetId, background, style, context.isNightMode())
        if (info.widgetType == WidgetType.Widget1x1 && info.widgetType == WidgetType.Widget4x2) {
            updateViews.setTextColor(R.id.condition_temp, textColor)
        }

        val is4x2 = info.widgetType == WidgetType.Widget4x2

        if (info.widgetType != WidgetType.Widget4x1) {
            // WeatherIcon
            val weatherIconResId = WeatherIconsManager.getInstance().getWeatherIconResource(weather.weatherIcon)
            if (!WidgetUtils.isBackgroundOptionalWidget(info.widgetType) || is4x2) {
                updateViews.setImageViewBitmap(R.id.weather_icon,
                        ImageUtils.bitmapFromDrawable(ContextUtils.getThemeContextOverride(context, false), weatherIconResId))
            } else if (style == WidgetUtils.WidgetBackgroundStyle.PANDA) {
                updateViews.setImageViewResource(R.id.weather_icon, weatherIconResId)
            } else {
                updateViews.setImageViewBitmap(R.id.weather_icon,
                        ImageUtils.bitmapFromDrawable(ContextUtils.getThemeContextOverride(context, style == WidgetUtils.WidgetBackgroundStyle.LIGHT), weatherIconResId))
            }
        }

        if (info.widgetType == WidgetType.Widget2x2) {
            if (style != WidgetUtils.WidgetBackgroundStyle.PANDA) {
                updateViews.setTextColor(R.id.location_name, panelTextColor)
            }
        } else {
            updateViews.setTextColor(R.id.location_name, if (is4x2) textColor else panelTextColor)
        }

        if (info.widgetType != WidgetType.Widget4x1Google && info.widgetType != WidgetType.Widget4x1) {
            if (info.widgetType == WidgetType.Widget2x2) {
                if (style != WidgetUtils.WidgetBackgroundStyle.PANDA) {
                    updateViews.setTextColor(R.id.condition_weather, panelTextColor)
                }
            } else {
                updateViews.setTextColor(R.id.condition_weather, if (is4x2) textColor else panelTextColor)
            }
        }

        if (info.widgetType == WidgetType.Widget2x2 || info.widgetType == WidgetType.Widget4x1Notification) {
            updateViews.setImageViewBitmap(R.id.hi_icon,
                    ImageUtils.tintedBitmapFromDrawable(context, R.drawable.wi_direction_up, Colors.WHITE)
            )
            updateViews.setImageViewBitmap(R.id.lo_icon,
                    ImageUtils.tintedBitmapFromDrawable(context, R.drawable.wi_direction_down, Colors.WHITE)
            )

            if (style != WidgetUtils.WidgetBackgroundStyle.PANDA) {
                updateViews.setTextColor(R.id.condition_hi, panelTextColor)
                updateViews.setTextColor(R.id.divider, panelTextColor)
                updateViews.setTextColor(R.id.condition_lo, panelTextColor)
                updateViews.setTextColor(R.id.weather_pop, panelTextColor)
                updateViews.setTextColor(R.id.weather_windspeed, panelTextColor)

                if (background != WidgetUtils.WidgetBackground.TRANSPARENT) {
                    updateViews.setInt(R.id.hi_icon, "setColorFilter", panelTextColor)
                    updateViews.setInt(R.id.lo_icon, "setColorFilter", panelTextColor)
                    updateViews.setInt(R.id.weather_popicon, "setColorFilter", panelTextColor)
                    updateViews.setInt(R.id.weather_windicon, "setColorFilter", panelTextColor)
                }
            }

            var chanceModel: DetailItemViewModel? = null
            var windModel: DetailItemViewModel? = null
            for (input in weather.getWeatherDetails()) {
                if (input.detailsType == WeatherDetailsType.POPCHANCE) {
                    chanceModel = input
                } else if (chanceModel == null && input.detailsType == WeatherDetailsType.POPCLOUDINESS) {
                    chanceModel = input
                } else if (input.detailsType == WeatherDetailsType.WINDSPEED) {
                    windModel = input
                }
                if (chanceModel != null && windModel != null) {
                    break
                }
            }

            if (chanceModel != null) {
                if (!WidgetUtils.isBackgroundOptionalWidget(info.widgetType)) {
                    updateViews.setImageViewBitmap(R.id.weather_popicon,
                            ImageUtils.bitmapFromDrawable(ContextUtils.getThemeContextOverride(context, false), chanceModel.icon)
                    )
                } else if (background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS && style == WidgetUtils.WidgetBackgroundStyle.PANDA) {
                    updateViews.setImageViewResource(R.id.weather_popicon, chanceModel.icon)
                } else {
                    updateViews.setImageViewBitmap(R.id.weather_popicon,
                            ImageUtils.bitmapFromDrawable(ContextUtils.getThemeContextOverride(context, style == WidgetUtils.WidgetBackgroundStyle.LIGHT), chanceModel.icon)
                    )
                }
            }

            if (windModel != null) {
                if (!WidgetUtils.isBackgroundOptionalWidget(info.widgetType)) {
                    updateViews.setImageViewBitmap(R.id.weather_windicon,
                            ImageUtils.rotateBitmap(
                                    ImageUtils.bitmapFromDrawable(ContextUtils.getThemeContextOverride(context, false), windModel.icon),
                                    windModel.iconRotation
                                            .toFloat())
                    )
                } else if (background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS && style == WidgetUtils.WidgetBackgroundStyle.PANDA) {
                    updateViews.setImageViewBitmap(R.id.weather_popicon,
                            ImageUtils.rotateBitmap(
                                    ImageUtils.bitmapFromDrawable(context, windModel.icon),
                                    windModel.iconRotation
                                            .toFloat()))
                } else {
                    updateViews.setImageViewBitmap(R.id.weather_windicon,
                            ImageUtils.rotateBitmap(
                                    ImageUtils.bitmapFromDrawable(ContextUtils.getThemeContextOverride(context, style == WidgetUtils.WidgetBackgroundStyle.LIGHT), windModel.icon),
                                    windModel.iconRotation
                                            .toFloat())
                    )
                }
            }
        }

        updateViews.setInt(R.id.settings_button, "setColorFilter", textColor)

        if (WidgetUtils.isDateWidget(info.widgetType) && info.widgetType != WidgetType.Widget4x1Google) {
            updateViews.setTextColor(R.id.date_panel, textColor)
        }
        if (WidgetUtils.isClockWidget(info.widgetType)) {
            updateViews.setTextColor(R.id.clock_panel, textColor)
        }
    }

    private suspend fun loadBackgroundImage(context: Context, updateViews: RemoteViews,
                                            info: WidgetProviderInfo, appWidgetId: Int,
                                            backgroundURI: String?, cellWidth: Int, cellHeight: Int
    ) = withContext(Dispatchers.IO) {
        /*
         * The total Bitmap memory used by the RemoteViews object cannot exceed
         * that required to fill the screen 1.5 times,
         * ie. (screen width x screen height x 4 x 1.5) bytes.
         */
        val maxBitmapSize = context.getMaxBitmapSize()

        var imgWidth = 200 * cellWidth
        var imgHeight = 200 * cellHeight

        /*
         * Ensure width and height are both > 0
         * To avoid IllegalArgumentException
         */
        if (imgWidth == 0 || imgHeight == 0) {
            when (info.widgetType) {
                WidgetType.Widget1x1 -> {
                    imgHeight = 200
                    imgWidth = imgHeight
                }
                WidgetType.Widget2x2 -> {
                    imgHeight = 200 * 2
                    imgWidth = imgHeight
                }
                WidgetType.Widget4x1, WidgetType.Widget4x1Google -> {
                    imgWidth = 200 * 4
                    imgHeight = 200
                }
                WidgetType.Widget4x2 -> {
                    imgWidth = 200 * 4
                    imgHeight = 200 * 2
                }
                else -> {
                    imgHeight = 200
                    imgWidth = imgHeight
                }
            }
        }

        /*
         * The total Bitmap memory used by the RemoteViews object cannot exceed
         * that required to fill the screen 1.5 times,
         * ie. (screen width x screen height x 4 x 1.5) bytes.
         */
        if (maxBitmapSize < 3840000) { // (200 * 4) * (200 * 4) * 4 * 1.5f
            imgHeight = 200
            imgWidth = imgHeight
        } else if (imgHeight * imgWidth * 4 * 0.75f > maxBitmapSize) {
            when (info.widgetType) {
                WidgetType.Widget1x1 -> {
                    imgHeight = 200
                    imgWidth = imgHeight
                }
                WidgetType.Widget2x2 -> {
                    imgHeight = 200 * 2
                    imgWidth = imgHeight
                }
                WidgetType.Widget4x1, WidgetType.Widget4x1Google -> {
                    imgWidth = 200 * 4
                    imgHeight = 200
                }
                WidgetType.Widget4x2 -> {
                    imgWidth = 200 * 4
                    imgHeight = 200 * 2
                }
                else -> {
                    imgHeight = 200
                    imgWidth = imgHeight
                }
            }
        }

        try {
            val bmp = suspendCancellableCoroutine<Bitmap?> {
                val task = GlideApp.with(context)
                        .asBitmap()
                        .load(backgroundURI)
                        .format(DecodeFormat.PREFER_RGB_565)
                        .centerCrop()
                        .transform(TransparentOverlay(0x33))
                        .thumbnail(0.75f)
                        .addListener(object : RequestListener<Bitmap> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                                if (it.isActive) {
                                    it.resume(null)
                                }
                                return true
                            }

                            override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                // Original image -> firstResource
                                // Thumbnail -> second resource
                                // Resume on the second call
                                if (it.isActive && !isFirstResource) it.resume(resource)
                                return true
                            }
                        })
                        .submit(imgWidth, imgHeight)

                it.invokeOnCancellation {
                    task.cancel(true)
                }
            } ?: return@withContext

            // Add overlay if background is a light overall color
            val p = Palette.from(bmp).generate()
            val isLight = ColorsUtils.isSuperLight(p)
            if (WidgetUtils.getBackgroundStyle(appWidgetId) == WidgetUtils.WidgetBackgroundStyle.FULLBACKGROUND && isLight) {
                updateViews.setInt(R.id.panda_container, "setBackgroundColor", 0x50000000)
                if (info.widgetType == WidgetType.Widget4x2) {
                    updateViews.setViewVisibility(R.id.weather_icon_overlay, View.VISIBLE)
                }
            } else {
                if (info.widgetType == WidgetType.Widget4x2) {
                    updateViews.setViewVisibility(R.id.weather_icon_overlay, View.GONE)
                }
            }
            updateViews.setInt(R.id.widgetBackground, "setColorFilter", Colors.TRANSPARENT)
            updateViews.setInt(R.id.widgetBackground, "setImageAlpha", 0xFF)
            updateViews.setImageViewBitmap(R.id.widgetBackground, bmp)
        } catch (e: Exception) {
            Logger.writeLine(Log.ERROR, e)
        }
    }

    private fun getForecasts(locData: LocationData, forecasts: List<Forecast>?, forecastLength: Int): List<ForecastItemViewModel> {
        val fcasts: List<Forecast>?

        if (forecasts?.isNotEmpty() == true) {
            fcasts = forecasts
        } else {
            val fcastData = settingsManager.getWeatherForecastData(locData.query)
            fcasts = fcastData?.forecast
        }

        if (fcasts?.isNotEmpty() == true) {
            return ArrayList<ForecastItemViewModel>(forecastLength).also {
                for (i in 0 until Math.min(forecastLength, fcasts.size)) {
                    it.add(ForecastItemViewModel(fcasts[i]))
                }
            }
        }

        return emptyList()
    }

    private fun getHourlyForecasts(locData: LocationData, forecasts: List<HourlyForecast>?, forecastLength: Int): List<HourlyForecastItemViewModel> {
        val hrfcasts: List<HourlyForecast>?

        val now = ZonedDateTime.now().withZoneSameInstant(locData.tzOffset)
        hrfcasts = if (forecasts?.isNotEmpty() == true) {
            forecasts
        } else {
            val hrInterval = WeatherManager.instance.getHourlyForecastInterval()
            settingsManager.getHourlyForecastsByQueryOrderByDateByLimitFilterByDate(locData.query, forecastLength, now.minusHours((hrInterval * 0.5).toLong()).truncatedTo(ChronoUnit.HOURS))
        }

        if (hrfcasts?.isNotEmpty() == true) {
            return ArrayList<HourlyForecastItemViewModel>(forecastLength).apply {
                var count = 0
                for (fcast in hrfcasts) {
                    add(HourlyForecastItemViewModel(fcast))
                    count++

                    if (count >= forecastLength) break
                }
            }
        }

        return emptyList()
    }

    fun resizeWidget(context: Context, info: WidgetProviderInfo,
                     appWidgetManager: AppWidgetManager, appWidgetId: Int,
                     newOptions: Bundle) {
        if (!settingsManager.useFollowGPS() && WidgetUtils.isGPS(appWidgetId)) return

        val updateViews = RemoteViews(context.packageName, info.widgetLayoutId)

        // Set sizes for views
        updateViewSizes(context, info, updateViews, newOptions)

        // Rebuild extras
        resizeExtras(context, info, updateViews, appWidgetId, newOptions)

        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, updateViews)
    }

    suspend fun refreshClock(context: Context, info: WidgetProviderInfo, appWidgetIds: IntArray) {
        val widgetType: WidgetType = info.widgetType

        if (WidgetUtils.isClockWidget(widgetType)) {
            val mAppWidgetManager = AppWidgetManager.getInstance(context)

            val views = RemoteViews(context.packageName, info.widgetLayoutId)
            for (appWidgetId in appWidgetIds) {
                if (!settingsManager.useFollowGPS() && WidgetUtils.isGPS(appWidgetId)) return
                val locationData = getLocation(context, appWidgetId)

                // Widget dimensions
                val newOptions = mAppWidgetManager.getAppWidgetOptions(appWidgetId)
                buildClock(context, info, locationData, views, appWidgetId, newOptions)
            }
        }
    }

    private fun buildClock(context: Context, info: WidgetProviderInfo, locData: LocationData?, views: RemoteViews, appWidgetId: Int, newOptions: Bundle) {
        if (!settingsManager.useFollowGPS() && WidgetUtils.isGPS(appWidgetId)) return

        updateClockSize(context, info, views, appWidgetId, newOptions)

        if (locData != null && WidgetUtils.useTimeZone(appWidgetId)) {
            views.setString(R.id.clock_panel, "setTimeZone", locData.tzLong)
        } else {
            views.setString(R.id.clock_panel, "setTimeZone", null)
        }
    }

    private fun updateClockSize(context: Context, info: WidgetProviderInfo, views: RemoteViews, appWidgetId: Int, newOptions: Bundle) {
        val widgetType: WidgetType = info.widgetType
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        val maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val maxCellHeight = WidgetUtils.getCellsForSize(maxHeight)
        val maxCellWidth = WidgetUtils.getCellsForSize(maxWidth)
        val cellHeight = WidgetUtils.getCellsForSize(minHeight)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)
        val forceSmallHeight = cellHeight == maxCellHeight
        val isSmallHeight = maxCellHeight.toFloat() / cellHeight <= 1.5f
        val isSmallWidth = maxCellWidth.toFloat() / cellWidth <= 1.5f

        // Update clock widgets
        val useAmPm = !(widgetType == WidgetType.Widget4x2Clock || widgetType == WidgetType.Widget4x2Huawei)
        val timeStr12hr = SpannableString(context.getText(if (useAmPm) R.string.clock_12_hours_ampm_format else R.string.clock_12_hours_format))
        if (useAmPm) {
            val start12hr = timeStr12hr.length - 2
            timeStr12hr.setSpan(RelativeSizeSpan(0.875f), start12hr, timeStr12hr.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        views.setCharSequence(R.id.clock_panel, "setFormat12Hour",
                timeStr12hr)
        views.setCharSequence(R.id.clock_panel, "setFormat24Hour",
                context.getText(R.string.clock_24_hours_format))

        if (widgetType == WidgetType.Widget4x2Huawei) {
            views.setTextViewTextSize(R.id.clock_panel, TypedValue.COMPLEX_UNIT_SP, if (cellWidth <= 3) 48f else 60f)
        } else if (widgetType == WidgetType.Widget4x2Clock) {
            views.setTextViewTextSize(R.id.clock_panel, TypedValue.COMPLEX_UNIT_SP, if (isSmallHeight && cellHeight <= 2) 60f else 66f)
        } else {
            var clockTextSize = context.resources.getDimensionPixelSize(R.dimen.clock_text_size).toFloat() // 36sp
            if (isSmallHeight && cellHeight <= 2 || cellWidth < 4) {
                clockTextSize *= 8f / 9 // 32sp
                if (cellWidth < 4 && widgetType == WidgetType.Widget4x2) {
                    clockTextSize *= 7f / 8 // 28sp
                }
            }
            views.setTextViewTextSize(R.id.clock_panel, TypedValue.COMPLEX_UNIT_PX, clockTextSize)
        }
    }

    suspend fun refreshDate(context: Context, info: WidgetProviderInfo, appWidgetIds: IntArray) {
        val widgetType: WidgetType = info.widgetType

        if (WidgetUtils.isDateWidget(widgetType)) {
            val mAppWidgetManager = AppWidgetManager.getInstance(context)
            val views = RemoteViews(context.packageName, info.widgetLayoutId)

            for (appWidgetId in appWidgetIds) {
                if (!settingsManager.useFollowGPS() && WidgetUtils.isGPS(appWidgetId)) return
                val locationData = getLocation(context, appWidgetId)

                // Widget dimensions
                val newOptions = mAppWidgetManager.getAppWidgetOptions(appWidgetId)
                buildDate(context, info, locationData, views, appWidgetId, newOptions)
            }
        }
    }

    private fun buildDate(context: Context, info: WidgetProviderInfo, locData: LocationData?, views: RemoteViews, appWidgetId: Int, newOptions: Bundle) {
        if (!settingsManager.useFollowGPS() && WidgetUtils.isGPS(appWidgetId)) return

        updateDateSize(context, info, views, appWidgetId, newOptions)

        if (locData != null && WidgetUtils.useTimeZone(appWidgetId)) {
            views.setString(R.id.date_panel, "setTimeZone", locData.tzLong)
        } else {
            views.setString(R.id.date_panel, "setTimeZone", null)
        }
    }

    private fun updateDateSize(context: Context, info: WidgetProviderInfo, views: RemoteViews, appWidgetId: Int, newOptions: Bundle) {
        val widgetType = info.widgetType
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        val maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val maxCellHeight = WidgetUtils.getCellsForSize(maxHeight)
        val maxCellWidth = WidgetUtils.getCellsForSize(maxWidth)
        val cellHeight = WidgetUtils.getCellsForSize(minHeight)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)
        val forceSmallHeight = cellHeight == maxCellHeight
        val isSmallHeight = maxCellHeight.toFloat() / cellHeight <= 1.5f
        val isSmallWidth = maxCellWidth.toFloat() / cellWidth <= 1.5f

        if (widgetType == WidgetType.Widget2x2) {
            var dateTextSize = context.resources.getDimensionPixelSize(R.dimen.date_text_size).toFloat() // 16sp
            if (isSmallHeight && cellHeight <= 2 || cellWidth < 4) dateTextSize *= 0.875f // 14sp
            views.setTextViewTextSize(R.id.date_panel, TypedValue.COMPLEX_UNIT_PX, dateTextSize)
        } else if (widgetType == WidgetType.Widget4x1Google) {
            var dateTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24f, context.resources.displayMetrics)
            if (isSmallHeight && cellHeight <= 2) {
                dateTextSize *= 5 / 6f // 20sp
            }
            views.setTextViewTextSize(R.id.date_panel, TypedValue.COMPLEX_UNIT_PX, dateTextSize)
        }

        val datePattern = if (widgetType == WidgetType.Widget2x2 && cellWidth >= 3 ||
                widgetType == WidgetType.Widget4x2Clock && cellWidth >= 4 ||
                widgetType == WidgetType.Widget4x2Huawei && cellWidth >= 4) {
            DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_LONG_DATE_FORMAT)
        } else if (widgetType == WidgetType.Widget4x1Google || widgetType == WidgetType.Widget4x2Clock) {
            DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_WDAY_ABBR_MONTH_FORMAT)
        } else if (widgetType == WidgetType.Widget4x2) {
            DateTimeUtils.getBestPatternForSkeleton(if (cellWidth > 4) DateTimeConstants.SKELETON_ABBR_WDAY_MONTH_FORMAT else DateTimeConstants.SKELETON_SHORT_DATE_FORMAT)
        } else {
            DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_SHORT_DATE_FORMAT)
        }
        views.setCharSequence(R.id.date_panel, "setFormat12Hour", datePattern)
        views.setCharSequence(R.id.date_panel, "setFormat24Hour", datePattern)
    }

    private fun getCalendarAppIntent(context: Context): PendingIntent {
        val componentName = WidgetUtils.getCalendarAppComponent(context)
        return if (componentName != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(componentName.packageName)
            PendingIntent.getActivity(context, 0, launchIntent, 0)
        } else {
            val onClickIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
            PendingIntent.getActivity(context, 0, onClickIntent, 0)
        }
    }

    private fun getClockAppIntent(context: Context): PendingIntent {
        val componentName = WidgetUtils.getClockAppComponent(context)
        return if (componentName != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(componentName.packageName)
            PendingIntent.getActivity(context, 0, launchIntent, 0)
        } else {
            val onClickIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            PendingIntent.getActivity(context, 0, onClickIntent, 0)
        }
    }

    private fun getShowNextIntent(context: Context?, info: WidgetProviderInfo, appWidgetId: Int): PendingIntent {
        val showNext = Intent()
                .setComponent(info.componentName)
                .setAction(WeatherWidgetProvider.ACTION_SHOWNEXTFORECAST)
                .putExtra(WeatherWidgetProvider.EXTRA_WIDGET_ID, appWidgetId)
        return PendingIntent.getBroadcast(context, appWidgetId, showNext, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun setOnClickIntent(context: Context, location: LocationData?, updateViews: RemoteViews?) {
        updateViews?.setOnClickPendingIntent(R.id.widget, getOnClickIntent(context, location))
    }

    private fun getOnClickIntent(context: Context, location: LocationData?): PendingIntent {
        // When user clicks on widget, launch to WeatherNow page
        val onClickIntent = Intent(context.applicationContext, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (settingsManager.getHomeData() != location) {
            onClickIntent.putExtra(Constants.KEY_DATA, JSONParser.serializer(location, LocationData::class.java))
            onClickIntent.putExtra(Constants.FRAGTAG_HOME, false)
        }

        return PendingIntent.getActivity(context, location?.hashCode()
                ?: SystemClock.uptimeMillis().toInt(), onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun setOnSettingsClickIntent(context: Context, updateViews: RemoteViews?, location: LocationData?, appWidgetId: Int) {
        if (updateViews != null) {
            // When user clicks on widget, launch to Config activity
            val onClickIntent = Intent(context.applicationContext, WeatherWidgetConfigActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            if (WidgetUtils.isGPS(appWidgetId)) {
                onClickIntent.putExtra(WeatherWidgetProvider.EXTRA_LOCATIONQUERY, Constants.KEY_GPS)
            } else {
                onClickIntent.putExtra(WeatherWidgetProvider.EXTRA_LOCATIONNAME, location?.name)
                onClickIntent.putExtra(WeatherWidgetProvider.EXTRA_LOCATIONQUERY, location?.query)
            }
            onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            val clickPendingIntent = PendingIntent.getActivity(context, appWidgetId, onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            updateViews.setOnClickPendingIntent(R.id.settings_button, clickPendingIntent)
        }
    }

    private fun Context.isNightMode(): Boolean {
        val currentNightMode: Int = this.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun Context.getMaxBitmapSize(): Float {
        val metrics = this.resources.displayMetrics
        return metrics.heightPixels * metrics.widthPixels * 4 * 0.75f
    }

    private fun getResIdentifier(res: Class<*>, fieldName: String): Int? {
        return try {
            val field = res.getField(fieldName)
            val resId = field.getInt(null)
            resId
        } catch (e: Exception) {
            null
        }
    }
}