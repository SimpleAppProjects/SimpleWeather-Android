package com.thewizrd.shared_resources.weatherdata.model

open class AirQualityData internal constructor() {
    var current: AirQuality? = null
    var aqiForecast: List<AirQuality>? = null
}