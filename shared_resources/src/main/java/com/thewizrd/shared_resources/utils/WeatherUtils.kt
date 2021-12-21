@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("WeatherUtils")

package com.thewizrd.shared_resources.utils

import android.graphics.Color
import android.text.format.DateFormat
import androidx.annotation.ColorInt
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.weatherdata.model.Weather
import java.time.ZonedDateTime

fun getLastBuildDate(weather: Weather): String {
    val context = SimpleLibrary.instance.app.appContext
    val date: String
    val prefix: String
    val update_time = weather.updateTime.toLocalDateTime()

    var timeformat = if (DateFormat.is24HourFormat(context)) {
        update_time.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_24HR))
    } else {
        update_time.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_12HR_AMPM))
    }

    timeformat = String.format("%s %s", timeformat, weather.location.tzShort)

    if (update_time.dayOfWeek == ZonedDateTime.now().dayOfWeek) {
        prefix = context.getString(R.string.update_prefix_day)
        date = String.format("%s %s", prefix, timeformat)
    } else {
        prefix = context.getString(R.string.update_prefix)
        date = String.format("%s %s %s", prefix, update_time.format(
                DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK)), timeformat)
    }

    return date
}

fun getPressureStateIcon(state: String?): String {
    val state = state ?: ""

    return when (state) {
        "1",
        "+",
        "Rising" -> {
            "\uf058\uf058"
        }
        "2",
        "-",
        "Falling" -> {
            "\uf044\uf044"
        }
        else -> {
            ""
        }
    }
}

fun getFeelsLikeTemp(temp_f: Float, wind_mph: Float, humidity_percent: Int): Float {
    return if (temp_f < 50) {
        calculateWindChill(temp_f, wind_mph)
    } else if (temp_f > 80) {
        calculateHeatIndex(temp_f, humidity_percent)
    } else {
        temp_f
    }
}

fun calculateWindChill(temp_f: Float, wind_mph: Float): Float {
    return if (temp_f < 50) {
        (35.74f + 0.6215f * temp_f - 35.75f * Math.pow(wind_mph.toDouble(), 0.16) + 0.4275f * temp_f * Math.pow(wind_mph.toDouble(), 0.16)).toFloat()
    } else {
        temp_f
    }
}

fun calculateHeatIndex(temp_f: Float, humidity: Int): Float {
    if (temp_f > 80) {
        var HI = (-42.379
                  + (2.04901523 * temp_f)
                  + (10.14333127 * humidity)
                  - (0.22475541 * temp_f * humidity)
                  - (0.00683783 * Math.pow(temp_f.toDouble(), 2.0))
                  - (0.05481717 * Math.pow(humidity.toDouble(), 2.0))
                  + (0.00122874 * Math.pow(temp_f.toDouble(), 2.0) * humidity)
                  + (0.00085282 * temp_f * Math.pow(humidity.toDouble(), 2.0))
                  - (0.00000199 * Math.pow(temp_f.toDouble(), 2.0) * Math.pow(humidity.toDouble(), 2.0)))

        if (humidity < 13 && temp_f > 80 && temp_f < 112) {
            val adj = (13 - humidity) / 4f * Math.sqrt(((17 - Math.abs(temp_f - 95)) / 17).toDouble())
            HI -= adj
        } else if (humidity > 85 && temp_f > 80 && temp_f < 87) {
            val adj = ((humidity - 85) / 10f * ((87 - temp_f) / 5)).toDouble()
            HI += adj
        }

        return if (HI > 80 && HI > temp_f) {
            HI.toFloat()
        } else {
            temp_f
        }
    } else {
        return temp_f
    }
}

fun calculateDewpointF(temp_f: Float, humidity: Int): Float {
    return ConversionMethods.CtoF(calculateDewpointC(ConversionMethods.FtoC(temp_f), humidity))
}

fun calculateDewpointC(temp_c: Float, humidity: Int): Float {
    return (243.04f * (Math.log((humidity / 100f).toDouble()) + ((17.625f * temp_c) / (243.04f + temp_c))) / (17.625f - Math.log((humidity / 100f).toDouble()) - ((17.625f * temp_c) / (243.04f + temp_c)))).toFloat()
}

@ColorInt
@JvmOverloads
fun getColorFromTempF(temp_f: Float, @ColorInt defaultColor: Int = Colors.SIMPLEBLUE): Int {
    return if (temp_f <= 47.5) {
        Colors.LIGHTSKYBLUE
    } else if (temp_f >= 85) {
        Colors.RED
    } else if (temp_f >= 70) {
        Colors.ORANGE
    } else {
        defaultColor
    }
}

@ColorInt
@JvmOverloads
fun getColorFromUVIndex(index: Float, defaultColor: Int = Colors.ORANGE): Int {
    return when {
        index < 3 -> {
            Colors.LIMEGREEN
        }
        index < 6 -> {
            Colors.YELLOW
        }
        index < 8 -> {
            Colors.ORANGE
        }
        index < 11 -> {
            Color.rgb(0xBD, 0x00, 0x35) // Maroon
        }
        index >= 11 -> {
            Color.rgb(0xAA, 0x00, 0xFF) // Purple
        }
        else -> defaultColor
    }
}