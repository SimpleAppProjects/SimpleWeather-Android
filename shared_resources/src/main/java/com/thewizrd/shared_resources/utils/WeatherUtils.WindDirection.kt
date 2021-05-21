@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("WeatherUtils")

package com.thewizrd.shared_resources.utils

import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.SimpleLibrary

fun getWindDirection(angle: Float): String {
    val context = SimpleLibrary.instance.app.appContext

    return if (angle >= 348.75 && angle <= 11.25) {
        context.getString(R.string.wind_dir_n)
    } else if (angle >= 11.25 && angle <= 33.75) {
        context.getString(R.string.wind_dir_nne)
    } else if (angle >= 33.75 && angle <= 56.25) {
        context.getString(R.string.wind_dir_ne)
    } else if (angle >= 56.25 && angle <= 78.75) {
        context.getString(R.string.wind_dir_ene)
    } else if (angle >= 78.75 && angle <= 101.25) {
        context.getString(R.string.wind_dir_e)
    } else if (angle >= 101.25 && angle <= 123.75) {
        context.getString(R.string.wind_dir_ese)
    } else if (angle >= 123.75 && angle <= 146.25) {
        context.getString(R.string.wind_dir_se)
    } else if (angle >= 146.25 && angle <= 168.75) {
        context.getString(R.string.wind_dir_sse)
    } else if (angle >= 168.75 && angle <= 191.25) {
        context.getString(R.string.wind_dir_s)
    } else if (angle >= 191.25 && angle <= 213.75) {
        context.getString(R.string.wind_dir_ssw)
    } else if (angle >= 213.75 && angle <= 236.25) {
        context.getString(R.string.wind_dir_sw)
    } else if (angle >= 236.25 && angle <= 258.75) {
        context.getString(R.string.wind_dir_wsw)
    } else if (angle >= 258.75 && angle <= 281.25) {
        context.getString(R.string.wind_dir_w)
    } else if (angle >= 281.25 && angle <= 303.75) {
        context.getString(R.string.wind_dir_wnw)
    } else if (angle >= 303.75 && angle <= 326.25) {
        context.getString(R.string.wind_dir_nw)
    } else /* if (angle >= 326.25 && angle <= 348.75) */ {
        context.getString(R.string.wind_dir_nnw)
    }
}

/* Used by NWS */
fun getWindDirection(direction: String): Int {
    return when (direction) {
        "N" -> 0
        "NNE" -> 22
        "NE" -> 45
        "ENE" -> 67
        "E" -> 90
        "ESE" -> 112
        "SE" -> 135
        "SSE" -> 157
        "S" -> 180
        "SSW" -> 202
        "SW" -> 225
        "WSW" -> 247
        "W" -> 270
        "WNW" -> 292
        "NW" -> 315
        else -> 337
    }
}