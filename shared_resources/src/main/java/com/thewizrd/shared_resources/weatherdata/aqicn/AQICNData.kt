package com.thewizrd.shared_resources.weatherdata.aqicn

import com.thewizrd.shared_resources.weatherdata.model.AirQuality

class AQICNData(root: Rootobject) : AirQuality() {
    init {
        index = root.data.aqi
    }

    val uviForecast: List<UviItem>? = root.data?.forecast?.daily?.uvi
}