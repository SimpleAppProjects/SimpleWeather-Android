@file:JvmMultifileClass
@file:JvmName("WeatherUtils")

package com.thewizrd.shared_resources.utils

import android.graphics.Color
import android.text.format.DateFormat
import androidx.annotation.ColorInt
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.weatherdata.model.BaseForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

fun getLastBuildDate(weather: Weather): String {
    val context = sharedDeps.context
    val date: String
    val prefix: String
    val updateTime = weather.updateTime!!.toLocalDateTime()

    var timeformat = if (DateFormat.is24HourFormat(context)) {
        updateTime.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_24HR))
    } else {
        updateTime.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_12HR_AMPM))
    }

    timeformat = String.format("%s %s", timeformat, weather.location?.tzShort)

    if (updateTime.dayOfWeek == ZonedDateTime.now().dayOfWeek) {
        prefix = context.getString(R.string.update_prefix_day)
        date = String.format("%s %s", prefix, timeformat)
    } else {
        prefix = context.getString(R.string.update_prefix)
        date = String.format(
            "%s %s %s",
            prefix,
            updateTime.format(
                DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK)), timeformat)
    }

    return date
}

fun getPressureStateIcon(state: String?): String {
    return when (state ?: "") {
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
        (35.74f + 0.6215f * temp_f - 35.75f * wind_mph.pow(0.16f) + 0.4275f * temp_f * wind_mph.pow(
            0.16f
        ))
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
                - (0.00683783 * temp_f.pow(2.0f))
                - (0.05481717 * humidity.toFloat().pow(2.0f))
                + (0.00122874 * temp_f.pow(2.0f) * humidity)
                + (0.00085282 * temp_f * humidity.toFloat().pow(2.0f))
                - (0.00000199 * temp_f.pow(2.0f) * humidity.toFloat().pow(2.0f)))

        if (humidity < 13 && temp_f > 80 && temp_f < 112) {
            val adj = (13 - humidity) / 4f * sqrt(((17 - abs(temp_f - 95)) / 17))
            HI -= adj
        } else if (humidity > 85 && temp_f > 80 && temp_f < 87) {
            val adj = ((humidity - 85) / 10f * ((87 - temp_f) / 5))
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

fun Weather.calculateFeelsLikeTemp() {
    if (this.condition == null) return
    if (this.atmosphere == null) return

    val currentTempF = this.condition?.tempF ?: return
    val currentHumidity = this.atmosphere?.humidity ?: return
    val currentWindMph = this.condition?.windMph ?: return
    val currentFeelsLikeF = this.condition?.feelslikeF

    if (currentFeelsLikeF == null) {
        val feelsLikeF = getFeelsLikeTemp(currentTempF, currentWindMph, currentHumidity)
        this.condition?.feelslikeF = feelsLikeF
        this.condition?.feelslikeC = ConversionMethods.FtoC(feelsLikeF)
    }
}

fun BaseForecast.calculateFeelsLikeTemp() {
    if (this.extras == null) return

    val tempF = this.highF ?: return
    val humidity = this.extras?.humidity ?: return
    val windMph = this.extras?.windMph ?: return
    val currentFeelsLikeF = this.extras?.feelslikeF

    if (currentFeelsLikeF == null) {
        val feelsLikeF = getFeelsLikeTemp(tempF, windMph, humidity)
        this.extras?.feelslikeF = feelsLikeF
        this.extras?.feelslikeC = ConversionMethods.FtoC(feelsLikeF)
    }
}

fun calculateDewpointF(temp_f: Float, humidity: Int): Float {
    return ConversionMethods.CtoF(calculateDewpointC(ConversionMethods.FtoC(temp_f), humidity))
}

fun calculateDewpointC(temp_c: Float, humidity: Int): Float {
    return (243.04f * (ln((humidity / 100f)) + ((17.625f * temp_c) / (243.04f + temp_c))) / (17.625f - ln(
        (humidity / 100f)
    ) - ((17.625f * temp_c) / (243.04f + temp_c))))
}

fun Weather.calculateDewpoint() {
    if (this.atmosphere == null) return

    val currentTempC = this.condition?.tempC ?: return
    val currentTempF = this.condition?.tempF ?: return
    val currentHumidity = this.atmosphere?.humidity ?: return
    val currentDewPointC = this.atmosphere?.dewpointC
    val currentDewPointF = this.atmosphere?.dewpointF

    if (currentDewPointC == null || currentDewPointF == null) {
        this.atmosphere?.dewpointC = calculateDewpointC(currentTempC, currentHumidity)
        this.atmosphere?.dewpointF = calculateDewpointF(currentTempF, currentHumidity)
    }
}

fun BaseForecast.calculateDewpoint() {
    if (this.extras == null) return

    val tempC = this.highC ?: return
    val tempF = this.highF ?: return
    val humidity = this.extras?.humidity ?: return
    val dewPointC = this.extras?.dewpointC
    val dewPointF = this.extras?.dewpointF

    if (dewPointC == null || dewPointF == null) {
        this.extras?.dewpointC = calculateDewpointC(tempC, humidity)
        this.extras?.dewpointF = calculateDewpointF(tempF, humidity)
    }
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