package com.thewizrd.common.weatherdata

import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.weatherdata.model.Weather

sealed class WeatherResult {
    open val data: Weather? = null

    data class Success(override val data: Weather, val isSavedData: Boolean = false) :
        WeatherResult()

    data class WeatherWithError(
        override val data: Weather,
        val isSavedData: Boolean = true,
        val exception: WeatherException
    ) : WeatherResult()

    data class NoWeather(override val data: Weather? = null, val isSavedData: Boolean = false) :
        WeatherResult()

    data class Error(val exception: WeatherException) : WeatherResult()
}

fun Weather?.toWeatherResult(isSavedData: Boolean = false): WeatherResult {
    return if (this == null) {
        WeatherResult.NoWeather(isSavedData = isSavedData)
    } else {
        WeatherResult.Success(this, isSavedData)
    }
}