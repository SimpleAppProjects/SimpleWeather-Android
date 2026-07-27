package com.thewizrd.simpleweather.controls.viewmodels

import android.text.format.DateFormat
import android.util.Log
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.NumberUtils.getValueOrDefault
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.weather_api.weatherModule
import java.text.DecimalFormat
import kotlin.math.roundToInt

class HourlyForecastNowViewModel(forecast: HourlyForecast) {
    var date: String
    var shortDate: String
    var icon: String
    var temperature: String
    var condition: String
    var popChance: String = ""
    var windSpeed: String = ""
    var windDirection: Int = 0

    val popChanceIcon = WeatherIcons.UMBRELLA
    val windIcon = WeatherIcons.DIRECTION_UP

    init {
        val context = appLib.context
        val settingsManager = appLib.settingsManager
        val isFahrenheit = Units.FAHRENHEIT == settingsManager.getTemperatureUnit()

        val df = DecimalFormat.getInstance(LocaleUtils.getLocale()) as DecimalFormat
        df.applyPattern("0.##")

        val wm = weatherModule.weatherManager
        val is24hr = DateFormat.is24HourFormat(context)

        date = if (is24hr) {
            val skeleton = DateTimeConstants.SKELETON_DAYOFWEEK_AND_24HR
            forecast.date.format(
                DateTimeUtils.ofPatternForUserLocale(
                    DateTimeUtils.getBestPatternForSkeleton(
                        skeleton
                    )
                )
            )
        } else {
            val pattern = DateTimeConstants.ABBREV_DAYOFWEEK_AND_12HR_AMPM
            forecast.date.format(DateTimeUtils.ofPatternForUserLocale(pattern))
        }

        shortDate = if (is24hr) {
            val skeleton = DateTimeConstants.SKELETON_24HR
            forecast.date.format(
                DateTimeUtils.ofPatternForUserLocale(
                    DateTimeUtils.getBestPatternForSkeleton(
                        skeleton
                    )
                )
            )
        } else {
            val pattern = DateTimeConstants.ABBREV_12HR_AMPM
            forecast.date.format(DateTimeUtils.ofPatternForUserLocale(pattern))
        }

        icon = forecast.icon

        try {
            temperature = if (forecast.highF != null && forecast.highC != null) {
                val value =
                    if (isFahrenheit) Math.round(forecast.highF) else Math.round(forecast.highC)
                String.format(LocaleUtils.getLocale(), "%d°", value)
            } else {
                WeatherIcons.PLACEHOLDER
            }
        } catch (nFe: NumberFormatException) {
            temperature = WeatherIcons.PLACEHOLDER
            Logger.writeLine(Log.ERROR, nFe)
        }

        condition = if (wm.supportsWeatherLocale()) forecast.condition
            ?: context.getString(R.string.weather_notavailable) else wm.getWeatherCondition(forecast.icon)

        if (forecast.extras?.windMph != null && forecast.extras?.windKph != null && forecast.extras.windMph >= 0 && forecast.extras?.windDegrees != null && forecast.extras.windDegrees >= 0) {
            val unit = settingsManager.getSpeedUnit()
            val speedVal: Int
            val speedUnit: String

            when (unit) {
                Units.MILES_PER_HOUR -> {
                    speedVal = forecast.extras.windMph.roundToInt()
                    speedUnit = context.getString(R.string.unit_mph)
                }
                Units.KILOMETERS_PER_HOUR -> {
                    speedVal = forecast.extras.windKph.roundToInt()
                    speedUnit = context.getString(R.string.unit_kph)
                }
                Units.METERS_PER_SECOND -> {
                    speedVal = ConversionMethods.kphToMsec(forecast.extras.windKph).roundToInt()
                    speedUnit = context.getString(R.string.unit_msec)
                }

                Units.KNOTS -> {
                    speedVal = ConversionMethods.mphToKts(forecast.extras.windMph).roundToInt()
                    speedUnit = context.getString(R.string.unit_knots)
                }
                else -> {
                    speedVal = forecast.extras.windMph.roundToInt()
                    speedUnit = context.getString(R.string.unit_mph)
                }
            }

            windDirection = forecast.extras?.windDegrees.getValueOrDefault(0) + 180

            windSpeed = String.format(LocaleUtils.getLocale(), "%d %s", speedVal, speedUnit)
        }

        if (forecast.extras.pop != null && forecast.extras.pop >= 0) {
            popChance = forecast.extras.pop.toString() + "%"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HourlyForecastNowViewModel

        if (date != other.date) return false
        if (shortDate != other.shortDate) return false
        if (icon != other.icon) return false
        if (temperature != other.temperature) return false
        if (condition != other.condition) return false
        if (popChance != other.popChance) return false
        if (windSpeed != other.windSpeed) return false
        if (windDirection != other.windDirection) return false

        return true
    }

    override fun hashCode(): Int {
        var result = date.hashCode()
        result = 31 * result + shortDate.hashCode()
        result = 31 * result + icon.hashCode()
        result = 31 * result + temperature.hashCode()
        result = 31 * result + condition.hashCode()
        result = 31 * result + popChance.hashCode()
        result = 31 * result + windSpeed.hashCode()
        result = 31 * result + windDirection
        return result
    }
}