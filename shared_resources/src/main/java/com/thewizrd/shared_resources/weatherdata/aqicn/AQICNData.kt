package com.thewizrd.shared_resources.weatherdata.aqicn

import com.thewizrd.shared_resources.weatherdata.AirQuality

class AQICNData(root: Rootobject) : AirQuality(root) {
    val uviForecast: List<UviItem>? = root.data?.forecast?.daily?.uvi
}