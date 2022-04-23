package com.thewizrd.simpleweather.controls

import android.text.format.DateFormat
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.weatherdata.model.MinutelyForecast
import java.text.DecimalFormat

class MinutelyForecastViewModel(minutely: MinutelyForecast) {
    val date: String
    val rainAmount: String

    init {
        val context = appLib.context
        val settingsMgr = appLib.settingsManager
        val df = DecimalFormat.getInstance(LocaleUtils.getLocale()) as DecimalFormat
        df.applyPattern("0.##")

        val fmt = if (DateFormat.is24HourFormat(context)) {
            DateTimeUtils.ofPatternForUserLocale(
                DateTimeUtils.getBestPatternForSkeleton(
                    DateTimeConstants.SKELETON_24HR
                )
            )
        } else {
            DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_12HR_AMPM)
        }
        date = minutely.date.format(fmt)

        if (minutely.rainMm != null) {
            val unit = settingsMgr.getPrecipitationUnit()
            val precipValue: Float
            val precipUnit: String

            when (unit) {
                Units.INCHES -> {
                    precipValue = ConversionMethods.mmToIn(minutely.rainMm)
                    precipUnit = context.getString(R.string.unit_in)
                }
                Units.MILLIMETERS -> {
                    precipValue = minutely.rainMm
                    precipUnit = context.getString(R.string.unit_mm)
                }
                else -> {
                    precipValue = ConversionMethods.mmToIn(minutely.rainMm)
                    precipUnit = context.getString(R.string.unit_in)
                }
            }

            rainAmount = String.format(
                LocaleUtils.getLocale(),
                "%s %s",
                df.format(precipValue.toDouble()),
                precipUnit
            )
        } else {
            rainAmount = WeatherIcons.EM_DASH
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MinutelyForecastViewModel

        if (date != other.date) return false
        if (rainAmount != other.rainAmount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = date.hashCode()
        result = 31 * result + rainAmount.hashCode()
        return result
    }
}