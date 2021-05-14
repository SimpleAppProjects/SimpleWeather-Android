package com.thewizrd.simpleweather.images

import com.thewizrd.shared_resources.weatherdata.WeatherBackground
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.images.model.ImageData

object ImageDataUtils {
    suspend fun getDefaultImageData(backgroundCode: String?, weather: Weather): ImageData {
        val wm = WeatherManager.instance

        // Fallback to assets
        // day, night, rain, snow
        val imageData = ImageData()
        when (backgroundCode) {
            WeatherBackground.SNOW, WeatherBackground.SNOW_WINDY -> {
                imageData.imageURL = "file:///android_asset/backgrounds/snow.jpg"
                imageData.color = "#ffb8d0f0"
            }
            WeatherBackground.RAIN, WeatherBackground.RAIN_NIGHT -> {
                imageData.imageURL = "file:///android_asset/backgrounds/rain.jpg"
                imageData.color = "#ff102030"
            }
            WeatherBackground.TSTORMS_DAY, WeatherBackground.TSTORMS_NIGHT, WeatherBackground.STORMS -> {
                imageData.imageURL = "file:///android_asset/backgrounds/storms.jpg"
                imageData.color = "#ff182830"
            }
            WeatherBackground.FOG -> {
                imageData.imageURL = "file:///android_asset/backgrounds/fog.jpg"
                imageData.color = "#ff808080"
            }
            WeatherBackground.PARTLYCLOUDY_DAY -> {
                imageData.imageURL = "file:///android_asset/backgrounds/day-partlycloudy.jpg"
                imageData.color = "#ff88b0c8"
            }
            WeatherBackground.PARTLYCLOUDY_NIGHT -> {
                imageData.imageURL = "file:///android_asset/backgrounds/night-partlycloudy.jpg"
                imageData.color = "#ff182020"
            }
            WeatherBackground.MOSTLYCLOUDY_DAY -> {
                imageData.imageURL = "file:///android_asset/backgrounds/day-cloudy.jpg"
                imageData.color = "#ff88b0c8"
            }
            WeatherBackground.MOSTLYCLOUDY_NIGHT -> {
                imageData.imageURL = "file:///android_asset/backgrounds/night-cloudy.jpg"
                imageData.color = "#ff182020"
            }
            else -> if (wm.isNight(weather)) {
                imageData.imageURL = "file:///android_asset/backgrounds/night.jpg"
                imageData.color = "#ff182020"
            } else {
                imageData.imageURL = "file:///android_asset/backgrounds/day.jpg"
                imageData.color = "#ff88b0c8"
            }
        }

        return imageData
    }
}