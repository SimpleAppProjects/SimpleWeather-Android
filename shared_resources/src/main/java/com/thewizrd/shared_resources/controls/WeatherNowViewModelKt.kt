package com.thewizrd.shared_resources.controls

import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.getBackgroundColor
import com.thewizrd.shared_resources.utils.getImageData

suspend fun WeatherNowViewModel.getImageData(): ImageDataViewModel? {
    return this.weatherData?.getImageData()
}

fun WeatherNowViewModel.getBackgroundColor(): Int {
    return this.weatherData?.getBackgroundColor() ?: Colors.SIMPLEBLUE
}