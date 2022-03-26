package com.thewizrd.simpleweather.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.controls.BaseForecastItemViewModel
import com.thewizrd.shared_resources.controls.ForecastItemViewModel
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel
import com.thewizrd.shared_resources.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.helpers.ColorsUtils
import com.thewizrd.shared_resources.helpers.toImmutableCompatFlag
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.ImageUtils
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.main.MainActivity
import com.thewizrd.simpleweather.widgets.remoteviews.CustomBackgroundWidgetRemoteViewCreator
import com.thewizrd.simpleweather.widgets.remoteviews.WidgetRemoteViewCreator
import kotlinx.coroutines.*
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.min

object WidgetUpdaterHelper {
    private const val TAG = "WidgetUpdaterHelper"
    private val settingsManager = App.instance.settingsManager
    private const val MAX_FORECASTS = 6

    @JvmStatic
    fun widgetsExist(): Boolean {
        for (widgetType in WidgetType.values()) {
            val info = WidgetUtils.getWidgetProviderInfoFromType(widgetType)
            if (info?.hasInstances == true) return true
        }

        return false
    }

    suspend fun refreshWidgets(context: Context) {
        coroutineScope {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val jobs = mutableListOf<Deferred<*>>()

            for (widgetType in WidgetType.values()) {
                val info = WidgetUtils.getWidgetProviderInfoFromType(widgetType)

                if (info != null) {
                    jobs.add(
                        async {
                            runCatching {
                                refreshWidget(context, info, appWidgetManager, info.appWidgetIds)
                            }
                        }
                    )
                }
            }

            jobs.awaitAll()
        }
    }

    private fun resetWidget(context: Context, appWidgetId: Int, appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(context)) {
        val views = RemoteViews(context.packageName, R.layout.app_widget_configure_layout)

        val configureIntent = Intent(context, WeatherWidgetConfigActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        configureIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

        val clickPendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            configureIntent,
            PendingIntent.FLAG_UPDATE_CURRENT.toImmutableCompatFlag()
        )
        views.setOnClickPendingIntent(R.id.widget, clickPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    suspend fun resetGPSWidgets(context: Context) {
        val appWidgetIds = WidgetUtils.getWidgetIds(Constants.KEY_GPS)
        resetGPSWidgets(context, appWidgetIds)
    }

    internal suspend fun resetGPSWidgets(context: Context, appWidgetIds: List<Int>) {
        coroutineScope {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val jobs = mutableListOf<Deferred<*>>()

            for (appWidgetId in appWidgetIds) {
                jobs.add(
                    async(Dispatchers.Default) {
                        resetWidget(context, appWidgetId, appWidgetManager)
                    }
                )
            }

            jobs.awaitAll()
        }
    }

    suspend fun refreshWidgets(context: Context, location_query: String) {
        coroutineScope {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = WidgetUtils.getWidgetIds(location_query)
            val jobs = mutableListOf<Deferred<*>>()

            for (appWidgetId in appWidgetIds) {
                jobs.add(
                    async(Dispatchers.Default) {
                        val widgetType = WidgetUtils.getWidgetTypeFromID(appWidgetId)
                        val info = WidgetUtils.getWidgetProviderInfoFromType(widgetType)
                            ?: return@async

                        refreshWidget(context, info, appWidgetManager, IntArray(1) { appWidgetId })
                    }
                )
            }

            jobs.awaitAll()
        }
    }

    suspend fun refreshWidget(context: Context, info: WidgetProviderInfo, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val creator = WidgetUtils.getRemoteViewCreator(context, appWidgetId)
            val newOptions = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val views = creator.buildUpdate(appWidgetId, newOptions)

            if (views != null) {
                // Push update for this widget to the home screen
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } else {
                Logger.writeLine(
                    Log.DEBUG,
                    "%s: provider: %s; widgetId: %d; RemoteViews not provided",
                    TAG,
                    info.className,
                    appWidgetId
                )
                resetWidget(context, appWidgetId, appWidgetManager)
            }
        }
    }

    internal suspend fun buildUpdate(
        context: Context, info: WidgetProviderInfo,
        appWidgetId: Int, location: LocationData,
        weather: WeatherNowViewModel, newOptions: Bundle,
        loadBackground: Boolean = true
    ): RemoteViews {
        val creator = WidgetUtils.getRemoteViewCreator(context, appWidgetId)

        if (creator is CustomBackgroundWidgetRemoteViewCreator) {
            creator.loadBackground = loadBackground
        }

        return if (creator is WidgetRemoteViewCreator) {
            creator.buildUpdate(appWidgetId, weather, location, newOptions).apply {
                creator.buildExtras(appWidgetId, this, weather, location, newOptions)
            }
        } else {
            creator.buildUpdate(appWidgetId, newOptions)
                ?: throw IllegalStateException("RemoteViews not provided")
        }
    }

    internal suspend fun buildForecast(
        context: Context, info: WidgetProviderInfo,
        updateViews: RemoteViews, appWidgetId: Int,
        locData: LocationData, weather: Weather?,
        newOptions: Bundle
    ) {
        if (info.widgetType != WidgetType.Widget4x2MaterialYou) {
            updateViews.removeAllViews(R.id.forecast_layout)
        }
        if (info.widgetType == WidgetType.Widget4x2MaterialYou || info.widgetType == WidgetType.Widget4x4MaterialYou) {
            updateViews.removeAllViews(R.id.hrforecast_layout)
        }

        // Determine forecast size
        buildForecastPanel(
            context, info, updateViews, appWidgetId,
            locData, weather, newOptions
        )
    }

    private suspend fun buildForecastPanel(
        context: Context, info: WidgetProviderInfo,
        updateViews: RemoteViews, appWidgetId: Int,
        locData: LocationData, weather: Weather?,
        newOptions: Bundle
    ) {
        if (WidgetUtils.isForecastWidget(info.widgetType)) {
            // Background & Text Color
            val background = WidgetUtils.getWidgetBackground(appWidgetId)
            val style = WidgetUtils.getBackgroundStyle(appWidgetId)
            val textColor =
                WidgetUtils.getPanelTextColor(appWidgetId, background, style)

            var forecastPanel: RemoteViews? = null
            var hrForecastPanel: RemoteViews? = null

            val forecasts = getForecasts(locData, weather?.forecast, MAX_FORECASTS)
            val hourlyForecasts = getHourlyForecasts(locData, weather?.hrForecast, MAX_FORECASTS)
            val forecastOption = if (info.widgetType == WidgetType.Widget4x2MaterialYou) {
                WidgetUtils.ForecastOption.HOURLY
            } else {
                WidgetUtils.getForecastOption(appWidgetId)
            }

            val forecastLayoutId: Int
            val hrForecastLayoutId: Int
            if (info.widgetType == WidgetType.Widget4x4MaterialYou) {
                forecastLayoutId = R.layout.app_widget_forecast_layout_container_material
                hrForecastLayoutId = R.layout.app_widget_hrforecast_layout_container_material
            } else if (info.widgetType == WidgetType.Widget4x2MaterialYou) {
                forecastLayoutId = R.layout.app_widget_forecast_layout_container_material
                hrForecastLayoutId = R.layout.app_widget_hrforecast_layout_container_material
            } else if (info.widgetType == WidgetType.Widget4x1 || style != WidgetUtils.WidgetBackgroundStyle.PANDA) {
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

            for (i in 0 until MAX_FORECASTS) {
                if (forecastPanel != null && i < forecasts.size) {
                    setForecastItem(
                        context,
                        appWidgetId,
                        info,
                        forecastPanel,
                        background,
                        style,
                        forecasts[i],
                        i,
                        textColor
                    )
                }

                if (hrForecastPanel != null && i < hourlyForecasts.size) {
                    setForecastItem(
                        context,
                        appWidgetId,
                        info,
                        hrForecastPanel,
                        background,
                        style,
                        hourlyForecasts[i],
                        i,
                        textColor
                    )
                }

                if (i >= forecasts.size && i >= hourlyForecasts.size)
                    break
            }

            // Needed to determine how many items to show when resizing
            WidgetUtils.setMaxForecastLength(appWidgetId, forecasts.size)
            WidgetUtils.setMaxHrForecastLength(appWidgetId, hourlyForecasts.size)

            if (forecastPanel != null) {
                updateViews.addView(R.id.forecast_layout, forecastPanel)
            }
            if (hrForecastPanel != null) {
                if (info.widgetType == WidgetType.Widget4x2MaterialYou || info.widgetType == WidgetType.Widget4x4MaterialYou) {
                    updateViews.addView(R.id.hrforecast_layout, hrForecastPanel)
                } else {
                    updateViews.addView(R.id.forecast_layout, hrForecastPanel)
                }
            }

            updateForecastSizes(context, info, appWidgetId, updateViews, newOptions)

            if (info.widgetType != WidgetType.Widget4x2MaterialYou && info.widgetType != WidgetType.Widget4x4MaterialYou) {
                if (forecastPanel != null && hrForecastPanel != null && WidgetUtils.isTap2Switch(
                        appWidgetId
                    )
                ) {
                    updateViews.setOnClickPendingIntent(
                        R.id.forecast_layout,
                        getShowNextIntent(context, info, appWidgetId)
                    )
                } else {
                    updateViews.setOnClickPendingIntent(
                        R.id.forecast_layout,
                        getOnClickIntent(context, WidgetUtils.getLocationData(appWidgetId))
                    )
                }
            }
        }
    }

    private fun setForecastItem(
        context: Context, appWidgetId: Int,
        info: WidgetProviderInfo,
        forecastPanel: RemoteViews,
        background: WidgetUtils.WidgetBackground,
        style: WidgetUtils.WidgetBackgroundStyle,
        forecast: BaseForecastItemViewModel,
        forecastIdx: Int,
        textColor: Int
    ) {
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

        forecastPanel.setTextViewText(
            dateId,
            if (info.widgetType == WidgetType.Widget4x4MaterialYou && forecast is ForecastItemViewModel) {
                forecast.longDate
            } else {
                forecast.shortDate
            }
        )
        forecastPanel.setTextViewText(hiId, forecast.hiTemp)
        if (forecast is ForecastItemViewModel) {
            forecastPanel.setTextViewText(loId, forecast.loTemp)
        }

        if (info.widgetType != WidgetType.Widget4x2MaterialYou && info.widgetType != WidgetType.Widget4x4MaterialYou && (background != WidgetUtils.WidgetBackground.CURRENT_CONDITIONS || style != WidgetUtils.WidgetBackgroundStyle.PANDA)) {
            forecastPanel.setTextColor(dateId, textColor)
            forecastPanel.setTextColor(hiId, textColor)
            if (forecast is ForecastItemViewModel) {
                forecastPanel.setTextColor(dividerId, textColor)
                forecastPanel.setTextColor(loId, textColor)
            }
        }

        // WeatherIcon
        val wim = WeatherIconsManager.getInstance()
        val weatherIconResId = wim.getWeatherIconResource(forecast.weatherIcon)
        if (info.widgetType == WidgetType.Widget4x2MaterialYou || info.widgetType == WidgetType.Widget4x4MaterialYou) {
            forecastPanel.setImageViewResource(iconId, weatherIconResId)
            if (!wim.isFontIcon) {
                // Remove tint
                forecastPanel.setInt(iconId, "setColorFilter", 0x0)
            }
        } else if (!WidgetUtils.isBackgroundOptionalWidget(info.widgetType) || background == WidgetUtils.WidgetBackground.TRANSPARENT) {
            forecastPanel.setImageViewBitmap(
                iconId,
                ImageUtils.bitmapFromDrawable(
                    context.getThemeContextOverride(false),
                    weatherIconResId
                )
            )
        } else if (background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS && style == WidgetUtils.WidgetBackgroundStyle.PANDA) {
            forecastPanel.setImageViewResource(iconId, weatherIconResId)
        } else {
            // Custom background color
            val backgroundColor = if (background == WidgetUtils.WidgetBackground.CUSTOM) {
                WidgetUtils.getBackgroundColor(appWidgetId)
            } else {
                if (style == WidgetUtils.WidgetBackgroundStyle.LIGHT) Colors.WHITE else Colors.BLACK
            }

            forecastPanel.setImageViewBitmap(
                iconId,
                ImageUtils.bitmapFromDrawable(
                    context.getThemeContextOverride(
                        ColorsUtils.isSuperLight(backgroundColor)
                    ),
                    weatherIconResId
                )
            )
        }

        if (forecast is HourlyForecastItemViewModel && info.widgetType != WidgetType.Widget4x2MaterialYou && info.widgetType != WidgetType.Widget4x4MaterialYou) {
            forecastPanel.setViewVisibility(dividerId, View.GONE)
            forecastPanel.setViewVisibility(loId, View.GONE)
        }

        forecastPanel.setViewVisibility(viewId, View.VISIBLE)
    }

    internal fun updateForecastSizes(
        context: Context, info: WidgetProviderInfo,
        appWidgetId: Int,
        views: RemoteViews, newOptions: Bundle
    ) {
        // Widget dimensions
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        val maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val maxCellWidth = WidgetUtils.getCellsForSize(maxWidth)
        val cellHeight = WidgetUtils.getCellsForSize(minHeight)
        val cellWidth = WidgetUtils.getCellsForSize(minWidth)
        val hasExtraWidth = (maxCellWidth - cellWidth) > 0

        val txtSizeMultiplier = WidgetUtils.getCustomTextSizeMultiplier(appWidgetId)
        val icoSizeMultiplier = WidgetUtils.getCustomIconSizeMultiplier(appWidgetId)

        if (info.widgetType == WidgetType.Widget4x2MaterialYou || info.widgetType == WidgetType.Widget4x4MaterialYou) {
            val topHeight = 170 // dp
            val remainingCellHeight = WidgetUtils.getCellsForSize(maxHeight - topHeight)

            views.setViewVisibility(
                R.id.hrforecast_layout,
                if (cellWidth > 3) View.VISIBLE else View.GONE
            )

            val forecastLength = if (cellHeight >= 2) {
                minOf(
                    remainingCellHeight,
                    WidgetUtils.getMaxForecastLength(appWidgetId),
                    MAX_FORECASTS
                )
            } else {
                0
            }
            val hrForecastLength = if (cellWidth > 2) {
                minOf(cellWidth, WidgetUtils.getMaxHrForecastLength(appWidgetId), MAX_FORECASTS - 1)
            } else {
                0
            }

            for (i in 0 until MAX_FORECASTS) {
                val fcastViewId = getResIdentifier(R.id::class.java, "forecast${i + 1}") ?: 0
                val hrfcastViewId = getResIdentifier(R.id::class.java, "hrforecast${i + 1}") ?: 0

                if (fcastViewId != 0 && info.widgetType == WidgetType.Widget4x4MaterialYou) {
                    views.setViewVisibility(
                        fcastViewId,
                        if (i < forecastLength) View.VISIBLE else View.GONE
                    )
                }
                if (hrfcastViewId != 0) {
                    views.setViewVisibility(
                        hrfcastViewId,
                        if (i < hrForecastLength) View.VISIBLE else View.GONE
                    )
                }
            }
        } else {
            val forecastLength = WidgetUtils.getForecastLength(info.widgetType, cellWidth)
            val maxForecastLength =
                min(forecastLength, WidgetUtils.getMaxForecastLength(appWidgetId))
            val maxHrForecastLength =
                min(forecastLength, WidgetUtils.getMaxHrForecastLength(appWidgetId))

            var maxIconSize = context.dpToPx(40f).toInt()
            if (info.widgetType == WidgetType.Widget4x1) {
                if (WidgetUtils.isLocationNameHidden(appWidgetId) && WidgetUtils.isSettingsButtonHidden(
                        appWidgetId
                    )
                ) {
                    maxIconSize = (maxIconSize * 6 / 5f).toInt() // 48dp
                }
            }

            var datePadding = 0
            var textSize = 12f
            if (info.widgetType === WidgetType.Widget4x1) {
                datePadding = if (cellHeight <= 1) {
                    0
                } else {
                    context.dpToPx(2f).toInt()
                }

                if (cellHeight > 1 && (hasExtraWidth || cellWidth > 4)) textSize = 14f
            }

            for (i in 0 until MAX_FORECASTS) {
                val fcastViewId = getResIdentifier(R.id::class.java, "forecast${i + 1}") ?: break
                val fcastDateId =
                    getResIdentifier(R.id::class.java, "forecast${i + 1}_date") ?: break
                val fcastHiId = getResIdentifier(R.id::class.java, "forecast${i + 1}_hi") ?: break
                val fcastIconId =
                    getResIdentifier(R.id::class.java, "forecast${i + 1}_icon") ?: break
                val fcastLoId = getResIdentifier(R.id::class.java, "forecast${i + 1}_lo") ?: break
                val fcastDividerId = getResIdentifier(R.id::class.java, "forecast${i + 1}_divider")
                    ?: break

                val hrfcastViewId =
                    getResIdentifier(R.id::class.java, "hrforecast${i + 1}") ?: break
                val hrfcastDateId = getResIdentifier(R.id::class.java, "hrforecast${i + 1}_date")
                    ?: break
                val hrfcastHiId =
                    getResIdentifier(R.id::class.java, "hrforecast${i + 1}_hi") ?: break
                val hrfcastIconId = getResIdentifier(R.id::class.java, "hrforecast${i + 1}_icon")
                    ?: break

                views.setInt(fcastIconId, "setMaxWidth", (maxIconSize * icoSizeMultiplier).toInt())
                views.setInt(fcastIconId, "setMaxHeight", (maxIconSize * icoSizeMultiplier).toInt())

                views.setInt(
                    hrfcastIconId,
                    "setMaxWidth",
                    (maxIconSize * icoSizeMultiplier).toInt()
                )
                views.setInt(
                    hrfcastIconId,
                    "setMaxHeight",
                    (maxIconSize * icoSizeMultiplier).toInt()
                )

                views.setViewPadding(
                    fcastDateId,
                    datePadding,
                    datePadding,
                    datePadding,
                    datePadding
                )
                views.setViewPadding(
                    hrfcastDateId,
                    datePadding,
                    datePadding,
                    datePadding,
                    datePadding
                )

                views.setTextViewTextSize(
                    fcastDateId,
                    TypedValue.COMPLEX_UNIT_SP,
                    textSize * txtSizeMultiplier
                )
                views.setTextViewTextSize(
                    fcastHiId,
                    TypedValue.COMPLEX_UNIT_SP,
                    textSize * txtSizeMultiplier
                )
                views.setTextViewTextSize(
                    fcastDividerId,
                    TypedValue.COMPLEX_UNIT_SP,
                    textSize * txtSizeMultiplier
                )
                views.setTextViewTextSize(
                    fcastLoId,
                    TypedValue.COMPLEX_UNIT_SP,
                    textSize * txtSizeMultiplier
                )

                views.setTextViewTextSize(
                    hrfcastDateId,
                    TypedValue.COMPLEX_UNIT_SP,
                    textSize * txtSizeMultiplier
                )
                views.setTextViewTextSize(
                    hrfcastHiId,
                    TypedValue.COMPLEX_UNIT_SP,
                    textSize * txtSizeMultiplier
                )

                views.setViewVisibility(
                    fcastViewId,
                    if (i < maxForecastLength) View.VISIBLE else View.GONE
                )
                views.setViewVisibility(
                    hrfcastViewId,
                    if (i < maxHrForecastLength) View.VISIBLE else View.GONE
                )
            }
        }
    }

    private suspend fun getForecasts(
        locData: LocationData,
        forecasts: List<Forecast>?,
        forecastLength: Int
    ): List<ForecastItemViewModel> {
        val fcasts = if (forecasts?.isNotEmpty() == true) {
            forecasts
        } else {
            val fcastData = settingsManager.getWeatherForecastData(locData.query)
            fcastData?.forecast
        }

        if (fcasts?.isNotEmpty() == true) {
            return ArrayList<ForecastItemViewModel>(forecastLength).also {
                for (i in 0 until min(forecastLength, fcasts.size)) {
                    it.add(ForecastItemViewModel(fcasts[i]))
                }
            }
        }

        return emptyList()
    }

    private suspend fun getHourlyForecasts(
        locData: LocationData,
        forecasts: List<HourlyForecast>?,
        forecastLength: Int
    ): List<HourlyForecastItemViewModel> {
        val hrfcasts: List<HourlyForecast>?

        val now = ZonedDateTime.now().withZoneSameInstant(locData.tzOffset)
        hrfcasts = if (forecasts?.isNotEmpty() == true) {
            forecasts
        } else {
            val hrInterval = WeatherManager.instance.getHourlyForecastInterval()
            settingsManager.getHourlyForecastsByQueryOrderByDateByLimitFilterByDate(
                locData.query,
                forecastLength,
                now.minusHours((hrInterval * 0.5).toLong()).truncatedTo(ChronoUnit.HOURS)
            )
        }

        if (!hrfcasts.isNullOrEmpty()) {
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
        // Responsive widget layouts handled by RemoteViews directly for S+
        val isS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        if (isS && (info.widgetType == WidgetType.Widget2x2PillMaterialYou ||
                    info.widgetType == WidgetType.Widget4x2MaterialYou ||
                    info.widgetType == WidgetType.Widget4x4MaterialYou)
        ) return

        if (!settingsManager.useFollowGPS() && WidgetUtils.isGPS(appWidgetId)) return

        WidgetUtils.getRemoteViewCreator(context, appWidgetId).run {
            this.resizeWidget(info, appWidgetManager, appWidgetId, newOptions)
        }
    }

    private fun getShowNextIntent(context: Context?, info: WidgetProviderInfo, appWidgetId: Int): PendingIntent {
        val showNext = Intent()
                .setComponent(info.componentName)
                .setAction(WeatherWidgetProvider.ACTION_SHOWNEXTFORECAST)
                .putExtra(WeatherWidgetProvider.EXTRA_WIDGET_ID, appWidgetId)
        return PendingIntent.getBroadcast(
            context,
            appWidgetId,
            showNext,
            PendingIntent.FLAG_UPDATE_CURRENT.toImmutableCompatFlag()
        )
    }

    private suspend fun getOnClickIntent(context: Context, location: LocationData?): PendingIntent {
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

    internal fun getResIdentifier(res: Class<*>, fieldName: String): Int? {
        return try {
            val field = res.getField(fieldName)
            val resId = field.getInt(null)
            resId
        } catch (e: Exception) {
            null
        }
    }
}