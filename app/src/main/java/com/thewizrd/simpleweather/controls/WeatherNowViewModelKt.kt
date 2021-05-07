package com.thewizrd.simpleweather.controls

import com.thewizrd.shared_resources.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.simpleweather.images.getBackgroundColor
import com.thewizrd.simpleweather.images.getImageData

suspend fun WeatherNowViewModel.getImageData(): ImageDataViewModel? {
    return this.weatherData?.getImageData()
}

fun WeatherNowViewModel.getBackgroundColor(): Int {
    return this.weatherData?.getBackgroundColor() ?: Colors.SIMPLEBLUE
}