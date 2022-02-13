package com.thewizrd.simpleweather.controls.viewmodels

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.thewizrd.shared_resources.icons.AVDIconsProviderInterface
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.controls.graphs.RangeBarGraphData
import com.thewizrd.simpleweather.controls.graphs.RangeBarGraphDataSet
import com.thewizrd.simpleweather.controls.graphs.RangeBarGraphEntry
import com.thewizrd.simpleweather.controls.graphs.YEntryData
import kotlin.math.roundToInt

object RangeBarGraphMapper {
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

    @JvmStatic
    fun createGraphData(
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
            if (forecast.highF == null && forecast.lowF == null) {
                continue
            }

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
}