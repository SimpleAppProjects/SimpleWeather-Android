package com.thewizrd.simpleweather.viewmodels

import com.thewizrd.common.controls.WeatherUiModel
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.simpleweather.controls.ImageDataViewModel
import com.thewizrd.simpleweather.images.getBackgroundColor
import com.thewizrd.simpleweather.images.getImageData

suspend fun WeatherUiModel.getImageData(): ImageDataViewModel? {
    return this.weatherData?.getImageData()
}

fun WeatherUiModel.getBackgroundColor(): Int {
    return this.weatherData?.getBackgroundColor() ?: Colors.SIMPLEBLUE
}