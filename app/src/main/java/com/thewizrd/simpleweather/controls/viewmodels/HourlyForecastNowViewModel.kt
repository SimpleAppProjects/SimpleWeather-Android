package com.thewizrd.simpleweather.controls.viewmodels

import android.text.format.DateFormat
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.simpleweather.App
import java.text.DecimalFormat

class HourlyForecastNowViewModel(forecast: HourlyForecast) : ViewModel() {
    var date: String
    @DrawableRes
    var icon: Int = 0
    var temperature: String
    var condition: String
    var popChance: String = ""
    var windSpeed: String = ""
    var windDirection: Int = 0

    init {
        val context = App.instance.appContext
        val settingsManager = App.instance.settingsManager
        val isFahrenheit = Units.FAHRENHEIT == settingsManager.getTemperatureUnit()

        val df = DecimalFormat.getInstance(LocaleUtils.getLocale()) as DecimalFormat
        df.applyPattern("0.##")

        val wm = WeatherManager.instance
        val wim = WeatherIconsManager.getInstance()

        date = if (DateFormat.is24HourFormat(context)) {
            val skeleton = if (ContextUtils.isLargeTablet(context)) {
                DateTimeConstants.SKELETON_DAYOFWEEK_AND_24HR
            } else {
                DateTimeConstants.SKELETON_24HR
            }
            forecast.date.format(DateTimeUtils.ofPatternForUserLocale(DateTimeUtils.getBestPatternForSkeleton(skeleton)))
        } else {
            val pattern = if (ContextUtils.isLargeTablet(context)) {
                DateTimeConstants.ABBREV_DAYOFWEEK_AND_12HR_AMPM
            } else {
                DateTimeConstants.ABBREV_12HR_AMPM
            }
            forecast.date.format(DateTimeUtils.ofPatternForUserLocale(pattern))
        }

        icon = wim.getWeatherIconResource(forecast.icon)

        try {
            temperature = if (forecast.highF != null && forecast.highC != null) {
                val value = if (isFahrenheit) Math.round(forecast.highF) else Math.round(forecast.highC)
                String.format(LocaleUtils.getLocale(), "%d°", value)
            } else {
                WeatherIcons.PLACEHOLDER
            }
        } catch (nFe: NumberFormatException) {
            temperature = WeatherIcons.PLACEHOLDER
            Logger.writeLine(Log.ERROR, nFe)
        }

        condition = if (wm.supportsWeatherLocale()) forecast.condition else wm.getWeatherCondition(forecast.icon)

        if (forecast.windMph != null && forecast.windKph != null && forecast.windMph >= 0 && forecast.windDegrees != null && forecast.windDegrees >= 0) {
            val unit = settingsManager.getSpeedUnit()
            val speedVal: Int
            val speedUnit: String

            when (unit) {
                Units.MILES_PER_HOUR -> {
                    speedVal = Math.round(forecast.extras.windMph)
                    speedUnit = context.getString(R.string.unit_mph)
                }
                Units.KILOMETERS_PER_HOUR -> {
                    speedVal = Math.round(forecast.extras.windKph)
                    speedUnit = context.getString(R.string.unit_kph)
                }
                Units.METERS_PER_SECOND -> {
                    speedVal = Math.round(ConversionMethods.kphToMsec(forecast.extras.windKph))
                    speedUnit = context.getString(R.string.unit_msec)
                }
                else -> {
                    speedVal = Math.round(forecast.extras.windMph)
                    speedUnit = context.getString(R.string.unit_mph)
                }
            }

            windDirection = NumberUtils.getValueOrDefault(forecast.windDegrees, 0) + 180

            windSpeed = String.format(LocaleUtils.getLocale(), "%d %s", speedVal, speedUnit)
        }

        if (forecast.extras.pop != null && forecast.extras.pop >= 0) {
            popChance = forecast.extras.pop.toString() + "%"
        }
    }
}