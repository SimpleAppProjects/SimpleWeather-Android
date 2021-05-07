@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("WeatherUtils")

package com.thewizrd.shared_resources.utils

import com.thewizrd.shared_resources.weatherdata.Beaufort.BeaufortScale
import java.math.BigDecimal
import java.math.RoundingMode

fun getBeaufortScale(mph: Int): BeaufortScale {
    return if (mph >= 1 && mph <= 3) {
        BeaufortScale.B1
    } else if (mph >= 4 && mph <= 7) {
        BeaufortScale.B2
    } else if (mph >= 8 && mph <= 12) {
        BeaufortScale.B3
    } else if (mph >= 13 && mph <= 18) {
        BeaufortScale.B4
    } else if (mph >= 19 && mph <= 24) {
        BeaufortScale.B5
    } else if (mph >= 25 && mph <= 31) {
        BeaufortScale.B6
    } else if (mph >= 32 && mph <= 38) {
        BeaufortScale.B7
    } else if (mph >= 39 && mph <= 46) {
        BeaufortScale.B8
    } else if (mph >= 47 && mph <= 54) {
        BeaufortScale.B9
    } else if (mph >= 55 && mph <= 63) {
        BeaufortScale.B10
    } else if (mph >= 64 && mph <= 72) {
        BeaufortScale.B11
    } else if (mph >= 73) {
        BeaufortScale.B12
    } else {
        BeaufortScale.B0
    }
}

fun getBeaufortScale(metersPerSecond: Float): BeaufortScale {
    val mps = BigDecimal(metersPerSecond.toDouble()).setScale(1, RoundingMode.HALF_UP).toFloat()

    return if (mps >= 0.5f && mps <= 1.5f) {
        BeaufortScale.B1
    } else if (mps >= 1.6f && mps <= 3.3f) {
        BeaufortScale.B2
    } else if (mps >= 3.4f && mps <= 5.5f) {
        BeaufortScale.B3
    } else if (mps >= 5.5f && mps <= 7.9f) {
        BeaufortScale.B4
    } else if (mps >= 8f && mps <= 10.7f) {
        BeaufortScale.B5
    } else if (mps >= 10.8f && mps <= 13.8f) {
        BeaufortScale.B6
    } else if (mps >= 13.9f && mps <= 17.1f) {
        BeaufortScale.B7
    } else if (mps >= 17.2f && mps <= 20.7f) {
        BeaufortScale.B8
    } else if (mps >= 20.8f && mps <= 24.4f) {
        BeaufortScale.B9
    } else if (mps >= 24.5 && mps <= 28.4f) {
        BeaufortScale.B10
    } else if (mps >= 28.5f && mps <= 32.6f) {
        BeaufortScale.B11
    } else if (mps >= 32.7f) {
        BeaufortScale.B12
    } else {
        BeaufortScale.B0
    }
}