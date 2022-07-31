package com.thewizrd.simpleweather.widgets.remoteviews

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.core.view.drawToBitmap
import com.thewizrd.common.controls.WeatherUiModel
import com.thewizrd.common.helpers.ColorsUtils
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.dpToPx
import com.thewizrd.shared_resources.utils.ContextUtils.getOrientation
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.ContextUtils.isLargeTablet
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.graphs.*
import com.thewizrd.simpleweather.controls.viewmodels.ForecastGraphViewModel
import com.thewizrd.simpleweather.controls.viewmodels.RangeBarGraphMapper
import com.thewizrd.simpleweather.controls.viewmodels.createAQIGraphData
import com.thewizrd.simpleweather.widgets.WeatherWidgetProvider4x2ForecastGraph
import com.thewizrd.simpleweather.widgets.WidgetGraphType
import com.thewizrd.simpleweather.widgets.WidgetProviderInfo
import com.thewizrd.simpleweather.widgets.WidgetUtils
import com.thewizrd.simpleweather.widgets.WidgetUtils.getMaxBitmapSize
import com.thewizrd.simpleweather.widgets.preferences.*
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.min

class WeatherWidget4x2GraphCreator(context: Context) : WidgetRemoteViewCreator(context) {
    override val info: WidgetProviderInfo
        get() = WeatherWidgetProvider4x2ForecastGraph.Info.getInstance()

    override suspend fun buildUpdate(
        appWidgetId: Int,
        weather: WeatherUiModel,
        location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        return buildLayout(appWidgetId, weather, location, newOptions).apply {
            buildForecastGraph(
                this, appWidgetId, location,
                weather.weatherData, newOptions
            )
        }
    }

    private suspend fun buildLayout(
        appWidgetId: Int,
        weather: WeatherUiModel,
        location: LocationData,
        newOptions: Bundle
    ): RemoteViews {
        // Build an update that holds the updated widget contents
        val updateViews = RemoteViews(context.packageName, info.widgetLayoutId)

        val txtSizeMultiplier =
            newOptions.get(KEY_TEXTSIZE) as? Float ?: WidgetUtils.getCustomTextSizeMultiplier(
                appWidgetId
            )

        val textColor =
            newOptions.get(KEY_TXTCOLORCODE) as? Int ?: WidgetUtils.getTextColor(appWidgetId)

        updateViews.setTextColor(
            R.id.location_name,
            textColor
        )

        updateViews.setInt(R.id.refresh_button, "setColorFilter", textColor)
        updateViews.setInt(R.id.settings_button, "setColorFilter", textColor)

        // original icon size: 24dp
        val scaledIconSize = (context.dpToPx(16f) * txtSizeMultiplier).toInt()

        // Refresh icon
        updateViews.setImageViewBitmap(R.id.refresh_button, null)

        updateViews.setInt(R.id.refresh_button, "setMaxWidth", scaledIconSize)
        updateViews.setInt(R.id.refresh_button, "setMaxHeight", scaledIconSize)

        updateViews.setImageViewResource(R.id.refresh_button, R.drawable.ic_refresh)

        // Setting icon
        updateViews.setImageViewBitmap(R.id.settings_button, null)

        updateViews.setInt(R.id.settings_button, "setMaxWidth", scaledIconSize)
        updateViews.setInt(R.id.settings_button, "setMaxHeight", scaledIconSize)

        updateViews.setImageViewResource(R.id.settings_button, R.drawable.ic_outline_settings_24)

        // Location Name
        updateViews.setTextViewText(R.id.location_name, weather.location)
        updateViews.setTextViewTextSize(
            R.id.location_name,
            TypedValue.COMPLEX_UNIT_SP,
            14f * txtSizeMultiplier
        )

        updateViews.setViewVisibility(
            R.id.location_name,
            if (newOptions.get(KEY_HIDELOCNAME) as? Boolean ?: WidgetUtils.isLocationNameHidden(
                    appWidgetId
                )
            ) View.GONE else View.VISIBLE
        )
        updateViews.setViewVisibility(
            R.id.settings_button,
            if (newOptions.get(KEY_HIDESETTINGSBTN) as? Boolean
                    ?: WidgetUtils.isSettingsButtonHidden(appWidgetId)
            ) View.GONE else View.VISIBLE
        )
        updateViews.setViewVisibility(
            R.id.refresh_button,
            if (newOptions.get(KEY_HIDEREFRESHBTN) as? Boolean ?: WidgetUtils.isRefreshButtonHidden(
                    appWidgetId
                )
            ) View.GONE else View.VISIBLE
        )

        setOnClickIntent(location, updateViews)
        setOnSettingsClickIntent(updateViews, location, appWidgetId)
        setOnRefreshClickIntent(updateViews, appWidgetId)

        return updateViews
    }

    private suspend fun buildForecastGraph(
        updateViews: RemoteViews,
        appWidgetId: Int,
        locData: LocationData,
        weather: Weather?,
        newOptions: Bundle
    ) {
        val maxBitmapSize = context.getMaxBitmapSize()

        // Widget dimensions
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)

        var width = if (context.isLargeTablet()) {
            min(context.resources.displayMetrics.widthPixels, context.dpToPx(600f).toInt())
        } else if (context.getOrientation() == Configuration.ORIENTATION_LANDSCAPE) {
            min(context.resources.displayMetrics.heightPixels, context.dpToPx(560f).toInt())
        } else {
            context.resources.displayMetrics.widthPixels
        }
        var height = width / 2

        if (height * width * 4 * 1.5f >= maxBitmapSize) {
            width = context.dpToPx(minWidth.toFloat()).toInt()
            height = context.dpToPx(minHeight.toFloat()).toInt()
        }

        val background = newOptions.getSerializable(KEY_BGCOLOR) as? WidgetUtils.WidgetBackground
            ?: WidgetUtils.getWidgetBackground(appWidgetId)
        val textColor = if (background == WidgetUtils.WidgetBackground.CUSTOM) {
            newOptions.get(KEY_TXTCOLORCODE) as? Int ?: WidgetUtils.getTextColor(appWidgetId)
        } else {
            Colors.WHITE
        }

        val graphType = newOptions.getSerializable(KEY_GRAPHTYPEOPTION) as? WidgetGraphType
            ?: WidgetUtils.getWidgetGraphType(appWidgetId)
        val graphView = buildGraphView(
            context,
            appWidgetId,
            locData,
            weather,
            graphType,
            background,
            textColor,
            newOptions
        )

        if (graphView != null) {
            val specWidth = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
            val specHeight = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            graphView.measure(specWidth, specHeight)
            graphView.layout(0, 0, graphView.measuredWidth, graphView.measuredHeight)

            val bitmap = withContext(Dispatchers.Main.immediate) {
                graphView.drawToBitmap()
            }
            updateViews.setImageViewBitmap(R.id.graph_view, bitmap)

            var showLabel = true
            when (graphType) {
                WidgetGraphType.Forecast -> {
                    showLabel = false
                }
                WidgetGraphType.HourlyForecast -> {
                    updateViews.setTextViewText(
                        R.id.graph_label,
                        context.getString(R.string.label_hourlyforecast)
                    )
                }
                WidgetGraphType.Precipitation -> {
                    updateViews.setTextViewText(
                        R.id.graph_label,
                        context.getString(R.string.label_precipitation)
                    )
                }
                WidgetGraphType.Wind -> {
                    updateViews.setTextViewText(
                        R.id.graph_label,
                        context.getString(R.string.label_wind)
                    )
                }
                WidgetGraphType.Humidity -> {
                    updateViews.setTextViewText(
                        R.id.graph_label,
                        context.getString(R.string.label_humidity)
                    )
                }
                WidgetGraphType.UVIndex -> {
                    updateViews.setTextViewText(
                        R.id.graph_label,
                        context.getString(R.string.label_uv)
                    )
                }
                WidgetGraphType.AirQuality -> {
                    updateViews.setTextViewText(
                        R.id.graph_label,
                        context.getString(R.string.label_airquality_short)
                    )
                }
                WidgetGraphType.Minutely -> {
                    updateViews.setTextViewText(
                        R.id.graph_label,
                        context.getString(R.string.label_precipitation)
                    )
                }
            }

            val txtSizeMultiplier =
                newOptions.get(KEY_TEXTSIZE) as? Float ?: WidgetUtils.getCustomTextSizeMultiplier(
                    appWidgetId
                )

            updateViews.setTextViewTextSize(
                R.id.graph_label,
                TypedValue.COMPLEX_UNIT_SP,
                14f * txtSizeMultiplier
            )

            updateViews.setViewVisibility(R.id.graph_view, View.VISIBLE)
            updateViews.setViewVisibility(
                R.id.graph_label,
                if (showLabel) View.VISIBLE else View.GONE
            )
            updateViews.setViewVisibility(R.id.no_data_view, View.GONE)

            if (showLabel) {
                updateViews.setTextColor(R.id.graph_label, textColor)
            }
        } else {
            updateViews.setViewVisibility(R.id.graph_view, View.GONE)
            updateViews.setViewVisibility(R.id.no_data_view, View.VISIBLE)
            updateViews.setViewVisibility(R.id.graph_label, View.GONE)
        }
    }

    private suspend fun buildGraphView(
        context: Context,
        appWidgetId: Int,
        locData: LocationData,
        weather: Weather?,
        graphType: WidgetGraphType,
        background: WidgetUtils.WidgetBackground,
        @ColorInt textColor: Int,
        newOptions: Bundle
    ): View? {
        val backgroundColor =
            if (background == WidgetUtils.WidgetBackground.CUSTOM) {
                newOptions.get(KEY_BGCOLORCODE) as? Int ?: WidgetUtils.getBackgroundColor(
                    appWidgetId
                )
            } else {
                Colors.BLACK
            }
        val viewCtx = context.getThemeContextOverride(
            ColorsUtils.isSuperLight(backgroundColor)
        )

        val txtSizeMultiplier =
            newOptions.get(KEY_TEXTSIZE) as? Float ?: WidgetUtils.getCustomTextSizeMultiplier(
                appWidgetId
            )
        val icoSizeMultiplier =
            newOptions.get(KEY_ICONSIZE) as? Float ?: WidgetUtils.getCustomIconSizeMultiplier(
                appWidgetId
            )

        val graphTextSize =
            context.resources.getDimensionPixelSize(R.dimen.forecast_condition_size) * txtSizeMultiplier
        val graphIconSize = context.dpToPx(30f) * icoSizeMultiplier

        if (graphType == WidgetGraphType.Forecast) {
            val fcastData = (weather?.forecast
                ?: settingsManager.getWeatherForecastData(locData.query)?.forecast)?.take(6)

            return fcastData?.let {
                RangeBarGraphView(viewCtx).apply {
                    setDrawDataLabels(true)
                    setDrawIconLabels(true)
                    setBottomTextColor(textColor)
                    setFillParentWidth(true)

                    setBottomTextSize(graphTextSize)
                    setIconSize(graphIconSize)

                    data = RangeBarGraphMapper.createGraphData(viewCtx, it)
                }
            }
        } else if (graphType == WidgetGraphType.AirQuality) {
            val now = LocalDate.now(locData.tzOffset)

            val aqiFcastData = (weather?.aqiForecast
                ?: settingsManager.getWeatherForecastData(locData.query)?.aqiForecast)?.take(7)
                ?.filterNot { item ->
                    item.date.isBefore(now)
                }

            return aqiFcastData?.let {
                BarGraphView(viewCtx).apply {
                    setDrawDataLabels(true)
                    setDrawIconLabels(false)
                    setBottomTextColor(textColor)
                    setFillParentWidth(true)

                    setBottomTextSize(graphTextSize)
                    setIconSize(graphIconSize)

                    data = it.createAQIGraphData(viewCtx)
                }
            }
        } else if (graphType == WidgetGraphType.Minutely) {
            val now = ZonedDateTime.now().withZoneSameInstant(locData.tzOffset)
                .truncatedTo(ChronoUnit.MINUTES).let {
                it.withMinute(it.minute - (it.minute % 10))
            }
            val minutelyData = (
                    weather?.minForecast
                        ?: settingsManager.getWeatherForecastData(locData.query)?.minForecast
                    )?.filter { !it.date.isBefore(now) }
                ?.filter { it.date.minute % 10 == 0 }

            if (minutelyData.isNullOrEmpty()) {
                return null
            }

            val graphData = ForecastGraphViewModel().apply {
                setMinutelyForecastData(minutelyData)
            }.graphData

            return if (!graphData.isEmpty && !graphData.getDataSetByIndex(0).isNullOrEmpty()) {
                if (graphData is LineViewData) {
                    LineView(viewCtx).apply {
                        setDrawGridLines(false)
                        setDrawDotLine(false)
                        setDrawDataLabels(true)
                        setDrawIconLabels(false)
                        setDrawGraphBackground(true)
                        setDrawDotPoints(false)
                        setFillParentWidth(false)
                        setBottomTextColor(textColor)

                        setBottomTextSize(graphTextSize)
                        setIconSize(graphIconSize)

                        data = graphData
                    }
                } else if (graphData is BarGraphData) {
                    BarGraphView(viewCtx).apply {
                        setDrawDataLabels(true)
                        setDrawIconLabels(false)
                        setBottomTextColor(textColor)

                        setBottomTextSize(graphTextSize)
                        setIconSize(graphIconSize)

                        data = graphData
                    }
                } else {
                    null
                }
            } else {
                null
            }
        } else {
            val now = ZonedDateTime.now().withZoneSameInstant(locData.tzOffset)
            val hrInterval = weatherModule.weatherManager.getHourlyForecastInterval()
            val hrfcastData = (
                    weather?.hrForecast?.take(12)
                        ?: settingsManager.getHourlyForecastsByQueryOrderByDateByLimitFilterByDate(
                            locData.query, 12,
                            now.minusHours((hrInterval * 0.5).toLong())
                                .truncatedTo(ChronoUnit.HOURS)
                        )
                    )

            if (hrfcastData.isNullOrEmpty()) {
                return null
            }

            val forecastGraphType = when (graphType) {
                WidgetGraphType.Precipitation -> ForecastGraphViewModel.ForecastGraphType.PRECIPITATION
                WidgetGraphType.Wind -> ForecastGraphViewModel.ForecastGraphType.WIND
                WidgetGraphType.Humidity -> ForecastGraphViewModel.ForecastGraphType.HUMIDITY
                WidgetGraphType.UVIndex -> ForecastGraphViewModel.ForecastGraphType.UVINDEX
                else -> ForecastGraphViewModel.ForecastGraphType.TEMPERATURE
            }

            val graphData = ForecastGraphViewModel().apply {
                setForecastData(hrfcastData, forecastGraphType)
            }.graphData

            return if (!graphData.isEmpty && !graphData.getDataSetByIndex(0).isNullOrEmpty()) {
                if (graphData is LineViewData) {
                    LineView(viewCtx).apply {
                        setDrawGridLines(false)
                        setDrawDotLine(false)
                        setDrawDataLabels(true)
                        setDrawIconLabels(false)
                        setDrawGraphBackground(true)
                        setDrawDotPoints(false)
                        setFillParentWidth(true)
                        setBottomTextColor(textColor)

                        setBottomTextSize(graphTextSize)
                        setIconSize(graphIconSize)

                        data = graphData
                    }
                } else if (graphData is BarGraphData) {
                    BarGraphView(viewCtx).apply {
                        setDrawDataLabels(true)
                        setDrawIconLabels(false)
                        setBottomTextColor(textColor)

                        setBottomTextSize(graphTextSize)
                        setIconSize(graphIconSize)

                        data = graphData
                    }
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    override fun resizeWidget(
        info: WidgetProviderInfo,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val updateViews = RemoteViews(context.packageName, info.widgetLayoutId)

        resizeWidgetBackground(info, appWidgetId, updateViews, newOptions)

        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, updateViews)
    }
}