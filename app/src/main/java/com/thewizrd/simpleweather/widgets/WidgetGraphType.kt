package com.thewizrd.simpleweather.widgets

import android.util.SparseArray

enum class WidgetGraphType(val value: Int) {
    Forecast(0),
    HourlyForecast(1),
    Precipitation(2),
    Wind(3),
    Humidity(4),
    UVIndex(5),
    AirQuality(6),
    Minutely(7);

    companion object {
        private val map = SparseArray<WidgetGraphType>()

        init {
            for (type in values()) {
                map.put(type.value, type)
            }
        }

        fun valueOf(value: Int): WidgetGraphType {
            return map[value, Forecast]
        }
    }
}