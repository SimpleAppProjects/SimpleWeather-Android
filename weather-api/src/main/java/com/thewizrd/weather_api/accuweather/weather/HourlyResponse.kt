package com.thewizrd.weather_api.accuweather.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HourlyResponse(

        var hourlyResponse: List<HourlyResponseItem?>? = null
)

@JsonClass(generateAdapter = true)
data class WetBulbTemperature(

    @field:Json(name = "UnitType")
        var unitType: Int? = null,

    @field:Json(name = "Value")
        var value: Float? = null,

    @field:Json(name = "Unit")
        var unit: String? = null
)

@JsonClass(generateAdapter = true)
data class DewPoint(

    @field:Json(name = "UnitType")
        var unitType: Int? = null,

    @field:Json(name = "Value")
        var value: Float? = null,

    @field:Json(name = "Unit")
        var unit: String? = null
)

@JsonClass(generateAdapter = true)
data class Visibility(

    @field:Json(name = "UnitType")
        var unitType: Int? = null,

    @field:Json(name = "Value")
        var value: Float? = null,

    @field:Json(name = "Unit")
        var unit: String? = null
)

@JsonClass(generateAdapter = true)
data class Temperature(

    @field:Json(name = "UnitType")
        var unitType: Int? = null,

    @field:Json(name = "Value")
        var value: Float? = null,

    @field:Json(name = "Unit")
        var unit: String? = null
)

@JsonClass(generateAdapter = true)
data class HourlyResponseItem(

    @field:Json(name = "RainProbability")
        var rainProbability: Int? = null,

    @field:Json(name = "Wind")
        var wind: Wind? = null,

    @field:Json(name = "Temperature")
        var temperature: Temperature? = null,

    @field:Json(name = "SnowProbability")
        var snowProbability: Int? = null,

    @field:Json(name = "Snow")
        var snow: Snow? = null,

    @field:Json(name = "TotalLiquid")
        var totalLiquid: TotalLiquid? = null,

    @field:Json(name = "Ceiling")
        var ceiling: Ceiling? = null,

    @field:Json(name = "DateTime")
        var dateTime: String? = null,

    @field:Json(name = "RealFeelTemperature")
        var realFeelTemperature: RealFeelTemperature? = null,

    @field:Json(name = "Rain")
        var rain: Rain? = null,

    @field:Json(name = "PrecipitationProbability")
        var precipitationProbability: Int? = null,

    @field:Json(name = "HasPrecipitation")
        var hasPrecipitation: Boolean? = null,

    @field:Json(name = "RelativeHumidity")
        var relativeHumidity: Int? = null,

    @field:Json(name = "UVIndexText")
        var uVIndexText: String? = null,

    @field:Json(name = "IconPhrase")
        var iconPhrase: String? = null,

    @field:Json(name = "CloudCover")
        var cloudCover: Int? = null,

    @field:Json(name = "WindGust")
        var windGust: WindGust? = null,

    @field:Json(name = "UVIndex")
        var uVIndex: Float? = null,

    @field:Json(name = "WeatherIcon")
        var weatherIcon: Int? = null,

    @field:Json(name = "Ice")
        var ice: Ice? = null,

    @field:Json(name = "DewPoint")
        var dewPoint: DewPoint? = null,

    @field:Json(name = "IndoorRelativeHumidity")
        var indoorRelativeHumidity: Int? = null,

    @field:Json(name = "IceProbability")
        var iceProbability: Int? = null,

    @field:Json(name = "EpochDateTime")
        var epochDateTime: Long? = null,

    @field:Json(name = "WetBulbTemperature")
        var wetBulbTemperature: WetBulbTemperature? = null,

    @field:Json(name = "Visibility")
        var visibility: Visibility? = null,

    @field:Json(name = "IsDaylight")
        var isDaylight: Boolean? = null,

    @field:Json(name = "Link")
        var link: String? = null,

    @field:Json(name = "MobileLink")
        var mobileLink: String? = null
)

@JsonClass(generateAdapter = true)
data class Ceiling(

    @field:Json(name = "UnitType")
        var unitType: Int? = null,

    @field:Json(name = "Value")
        var value: Float? = null,

    @field:Json(name = "Unit")
        var unit: String? = null
)

@JsonClass(generateAdapter = true)
data class RealFeelTemperature(

    @field:Json(name = "UnitType")
        var unitType: Int? = null,

    @field:Json(name = "Value")
        var value: Float? = null,

    @field:Json(name = "Unit")
        var unit: String? = null
)
