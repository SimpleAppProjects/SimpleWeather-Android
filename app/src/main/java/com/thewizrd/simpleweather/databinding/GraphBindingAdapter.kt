package com.thewizrd.simpleweather.databinding

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import com.thewizrd.shared_resources.icons.AVDIconsProviderInterface
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.MinutelyForecast
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.graphs.*
import com.thewizrd.simpleweather.controls.viewmodels.ForecastGraphViewModel
import kotlin.math.roundToInt

object GraphBindingAdapter {
    @JvmStatic
    @BindingAdapter("graphData")
    fun updateForecastGraph(view: ForecastGraphPanel, graphData: LineViewData?) {
        view.setGraphData(graphData)
    }

    @JvmStatic
    @BindingAdapter("graphData")
    fun updateForecastGraph(view: RangeBarGraphPanel, graphData: RangeBarGraphData?) {
        view.setGraphData(graphData)
    }

    @JvmStatic
    @BindingAdapter("minForecastData")
    fun updateMinForecastGraph(view: ForecastGraphPanel, forecastData: List<MinutelyForecast>?) {
        if (!forecastData.isNullOrEmpty()) {
            val vm = ForecastGraphViewModel()
            vm.setMinutelyForecastData(forecastData)
            view.setGraphData(vm.graphData)
        } else {
            view.setGraphData(null)
        }
    }

    @JvmStatic
    @BindingAdapter("forecastData")
    fun updateForecastGraph(view: ForecastGraphPanel, forecastData: List<HourlyForecast>?) {
        if (!forecastData.isNullOrEmpty()) {
            val vm = ForecastGraphViewModel().apply {
                setForecastData(
                    forecastData,
                    ForecastGraphViewModel.ForecastGraphType.PRECIPITATION
                )
            }
            view.setGraphData(vm.graphData)
        } else {
            view.setGraphData(null)
        }
    }

    @JvmStatic
    @BindingAdapter("forecastData")
    fun updateForecastGraph(view: RangeBarGraphPanel, forecastData: List<Forecast>?) {
        view.setGraphData(createGraphData(view.context, forecastData))
    }

    @JvmStatic
    private fun createGraphData(
        context: Context,
        forecastData: List<Forecast>?
    ): RangeBarGraphData? {
        if (forecastData == null) return null

        val isFahrenheit = Units.FAHRENHEIT == App.instance.settingsManager.getTemperatureUnit()

        val entryData = ArrayList<RangeBarGraphEntry>(forecastData.size)

        for (forecast in forecastData) {
            val entry = RangeBarGraphEntry()
            val date =
                forecast.date.format(DateTimeUtils.ofPatternForUserLocale(context.getString(R.string.forecast_date_format)))

            entry.xLabel = date
            entry.xIcon = createIconDrawable(context, forecast.icon)

            // Temp Data
            if (forecast.highF != null && forecast.highC != null) {
                val value =
                    if (isFahrenheit) forecast.highF.roundToInt() else forecast.highC.roundToInt()
                val hiTemp = String.format(LocaleUtils.getLocale(), "%d°", value)
                entry.hiTempData = YEntryData(value.toFloat(), hiTemp)
            }
            if (forecast.lowF != null && forecast.lowC != null) {
                val value =
                    if (isFahrenheit) Math.round(forecast.lowF) else Math.round(forecast.lowC)
                val loTemp = String.format(LocaleUtils.getLocale(), "%d°", value)
                entry.loTempData = YEntryData(value.toFloat(), loTemp)
            }

            entryData.add(entry)
        }

        return RangeBarGraphData(RangeBarGraphDataSet(entryData))
    }

    @JvmStatic
    private fun createIconDrawable(context: Context, icon: String): Drawable? {
        val settingsMgr = App.instance.settingsManager
        val iconsSource = settingsMgr.getIconsProvider()
        val wip = WeatherIconsManager.getProvider(iconsSource)

        return if (wip is AVDIconsProviderInterface) {
            val avdProvider = wip as AVDIconsProviderInterface
            avdProvider.getAnimatedDrawable(context, icon)
        } else {
            ContextCompat.getDrawable(context, wip.getWeatherIconResource(icon))
        }
    }
}