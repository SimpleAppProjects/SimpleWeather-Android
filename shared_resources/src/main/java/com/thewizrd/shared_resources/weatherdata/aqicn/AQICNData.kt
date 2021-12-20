package com.thewizrd.shared_resources.weatherdata.aqicn

import com.thewizrd.shared_resources.utils.AirQualityUtils.getIndexFromData
import com.thewizrd.shared_resources.weatherdata.model.AirQuality
import com.thewizrd.shared_resources.weatherdata.model.AirQualityData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class AQICNData internal constructor(root: Rootobject) : AirQualityData() {
    init {
        current = AirQuality().apply {
            no2 = root.data?.iaqi?.no2?.v?.roundToInt()
            o3 = root.data?.iaqi?.o3?.v?.roundToInt()
            pm25 = root.data?.iaqi?.pm25?.v?.roundToInt()
            so2 = root.data?.iaqi?.so2?.v?.roundToInt()
            pm10 = root.data?.iaqi?.pm10?.v?.roundToInt()
            co = root.data?.iaqi?.co?.v?.roundToInt()

            index = root.data?.aqi ?: getIndexFromData()
        }
        aqiForecast = root.createAQIForecasts()
    }

    val uviForecast: List<UviItem>? = root.data?.forecast?.daily?.uvi

    private fun Rootobject.createAQIForecasts(): List<AirQuality>? {
        val dailyData = this.data?.forecast?.daily
        val maxAmtFcasts = maxOf(
                this.data?.forecast?.daily?.o3?.size ?: 0,
                this.data?.forecast?.daily?.pm10?.size ?: 0,
                this.data?.forecast?.daily?.pm25?.size ?: 0
        )

        if (dailyData != null && maxAmtFcasts > 0) {
            val aqiForecasts = ArrayList<AirQuality>(maxAmtFcasts)
            val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)

            dailyData.o3?.forEach {
                // 2021-12-17
                val itemDate = LocalDate.parse(it.day, dtf)

                val existing = aqiForecasts.find { aqi -> aqi.date.isEqual(itemDate) }

                if (existing != null) {
                    existing.o3 = it.avg
                } else {
                    aqiForecasts.add(AirQuality().apply {
                        date = itemDate
                        o3 = it.avg
                    })
                }
            }

            dailyData.pm25?.forEach {
                // 2021-12-17
                val itemDate = LocalDate.parse(it.day, dtf)

                val existing = aqiForecasts.find { aqi -> aqi.date.isEqual(itemDate) }

                if (existing != null) {
                    existing.pm25 = it.avg
                } else {
                    aqiForecasts.add(AirQuality().apply {
                        date = itemDate
                        pm25 = it.avg
                    })
                }
            }

            dailyData.pm10?.forEach {
                // 2021-12-17
                val itemDate = LocalDate.parse(it.day, dtf)

                val existing = aqiForecasts.find { aqi -> aqi.date.isEqual(itemDate) }

                if (existing != null) {
                    existing.pm10 = it.avg
                } else {
                    aqiForecasts.add(AirQuality().apply {
                        date = itemDate
                        pm10 = it.avg
                    })
                }
            }

            /*
            aqiForecasts.forEach {
                it.index = it.getIndexFromData()
            }*/

            aqiForecasts.sortBy {
                it.index = it.getIndexFromData()
                it.date
            }

            return aqiForecasts
        }

        return null
    }
}