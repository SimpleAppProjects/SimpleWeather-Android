package com.thewizrd.simpleweather.viewmodels

import android.text.format.DateFormat
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.appLib
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.weatherdata.model.MinutelyForecast
import java.text.DecimalFormat

class MinutelyForecastViewModel(minutely: MinutelyForecast) {
    val date: String
    val precipAmount: String
    val isSnow: Boolean

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

        val raimMm = minutely.rainMm ?: 0f
        val snowMm = minutely.snowMm ?: 0f
        isSnow = snowMm > raimMm
        val precipValueMm = if (isSnow) snowMm else raimMm

        val unit = settingsMgr.getPrecipitationUnit()
        val precipValue: Float
        val precipUnit: String

        when (unit) {
            Units.INCHES -> {
                precipValue = ConversionMethods.mmToIn(precipValueMm)
                precipUnit = context.getString(R.string.unit_in)
            }

            Units.MILLIMETERS -> {
                precipValue = precipValueMm
                precipUnit = context.getString(R.string.unit_mm)
            }

            else -> {
                precipValue = ConversionMethods.mmToIn(precipValueMm)
                precipUnit = context.getString(R.string.unit_in)
            }
        }

        precipAmount = String.format(
            LocaleUtils.getLocale(),
            "%s %s",
            df.format(precipValue.toDouble()),
            precipUnit
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MinutelyForecastViewModel

        if (date != other.date) return false
        if (precipAmount != other.precipAmount) return false
        if (isSnow != other.isSnow) return false

        return true
    }

    override fun hashCode(): Int {
        var result = date.hashCode()
        result = 31 * result + precipAmount.hashCode()
        result = 31 * result + isSnow.hashCode()
        return result
    }
}