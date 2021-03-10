package com.thewizrd.simpleweather.controls.viewmodels

import androidx.lifecycle.ViewModel
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Settings
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.weatherdata.Forecast
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.GraphTemperature
import com.thewizrd.simpleweather.controls.graphs.XLabelData
import com.thewizrd.simpleweather.controls.graphs.YEntryData

class RangeBarGraphViewModel(forecasts: Collection<Forecast>) : ViewModel() {
    val labelData: MutableList<XLabelData>
    val tempData: MutableList<GraphTemperature>

    init {
        val context = App.instance.appContext
        val isFahrenheit = Units.FAHRENHEIT == Settings.getTemperatureUnit()

        val wim = WeatherIconsManager.getInstance()

        labelData = ArrayList(12)
        tempData = ArrayList(12)

        for (forecast in forecasts) {
            val graphTempData = GraphTemperature()
            val date = forecast.date.format(DateTimeUtils.ofPatternForUserLocale(context.getString(R.string.forecast_date_format)))

            // Temp Data
            val xTemp = XLabelData(date, wim.getWeatherIconResource(forecast.getIcon()), 0)
            if (forecast.highF != null && forecast.highC != null) {
                val value = if (isFahrenheit) Math.round(forecast.highF) else Math.round(forecast.highC)
                val hiTemp = String.format(LocaleUtils.getLocale(), "%d°", value)
                graphTempData.hiTempData = YEntryData(value.toFloat(), hiTemp)
            }
            if (forecast.lowF != null && forecast.lowC != null) {
                val value = if (isFahrenheit) Math.round(forecast.lowF) else Math.round(forecast.lowC)
                val loTemp = String.format(LocaleUtils.getLocale(), "%d°", value)
                graphTempData.loTempData = YEntryData(value.toFloat(), loTemp)
            }

            labelData.add(xTemp)
            tempData.add(graphTempData)
        }
    }
}