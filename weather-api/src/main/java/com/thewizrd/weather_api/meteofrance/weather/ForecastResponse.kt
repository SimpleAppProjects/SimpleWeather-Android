package com.thewizrd.weather_api.meteofrance.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ForecastResponse(

    @field:Json(name = "updated_on")
    var updatedOn: Long? = null,

    @field:Json(name = "probability_forecast")
    var probabilityForecast: List<ProbabilityForecastItem?>? = null,

    @field:Json(name = "forecast")
    var forecast: List<ForecastItem?>? = null,

    @field:Json(name = "position")
    var position: ForecastPosition? = null,

    @field:Json(name = "daily_forecast")
    var dailyForecast: List<DailyForecastItem?>? = null
)

@JsonClass(generateAdapter = true)
data class Rain(

    @field:Json(name = "6h")
    var jsonMember6h: Float? = null,

    @field:Json(name = "3h")
    var jsonMember3h: Float? = null,

    @field:Json(name = "1h")
    var jsonMember1h: Float? = null
)

@JsonClass(generateAdapter = true)
data class Weather12H(

    @field:Json(name = "icon")
    var icon: String? = null,

    @field:Json(name = "desc")
    var desc: String? = null
)

@JsonClass(generateAdapter = true)
data class ForecastItem(

    @field:Json(name = "dt")
    var dt: Long? = null,

    @field:Json(name = "rain")
    var rain: Rain? = null,

    @field:Json(name = "T")
    var T: ForecastTemp? = null,

    @field:Json(name = "snow")
    var snow: Snow? = null,

    @field:Json(name = "weather")
    var weather: Weather? = null,

    @field:Json(name = "humidity")
    var humidity: Int? = null,

    @field:Json(name = "rain snow limit")
    var rainSnowLimit: String? = null,

    @field:Json(name = "iso0")
    var iso0: Int? = null,

    @field:Json(name = "sea_level")
    var seaLevel: Float? = null,

    @field:Json(name = "clouds")
    var clouds: Int? = null,

    @field:Json(name = "wind")
    var wind: Wind? = null
)

@JsonClass(generateAdapter = true)
data class ForecastPosition(

    @field:Json(name = "bulletin_cote")
    var bulletinCote: Int? = null,

    @field:Json(name = "country")
    var country: String? = null,

    @field:Json(name = "insee")
    var insee: String? = null,

    @field:Json(name = "timezone")
    var timezone: String? = null,

    @field:Json(name = "name")
    var name: String? = null,

    @field:Json(name = "lon")
    var lon: Float? = null,

    @field:Json(name = "dept")
    var dept: String? = null,

    @field:Json(name = "alti")
    var alti: Int? = null,

    @field:Json(name = "lat")
    var lat: Float? = null,

    @field:Json(name = "rain_product_available")
    var rainProductAvailable: Int? = null
)

@JsonClass(generateAdapter = true)
data class Weather(

    @field:Json(name = "icon")
    var icon: String? = null,

    @field:Json(name = "desc")
    var desc: String? = null
)

@JsonClass(generateAdapter = true)
data class Precipitation(

    @field:Json(name = "24h")
    var jsonMember24h: Float? = null
)

@JsonClass(generateAdapter = true)
data class DailyForecastTemp(

    @field:Json(name = "min")
    var min: Float? = null,

    @field:Json(name = "max")
    var max: Float? = null,

    @field:Json(name = "sea")
    var sea: Float? = null
)

@JsonClass(generateAdapter = true)
data class ForecastTemp(

    @field:Json(name = "value")
    var value: Float? = null,

    @field:Json(name = "windchill")
    var windchill: Float? = null
)

@JsonClass(generateAdapter = true)
data class Sun(

    @field:Json(name = "set")
    var set: Long? = null,

    @field:Json(name = "rise")
    var rise: Long? = null
)

@JsonClass(generateAdapter = true)
data class DailyForecastItem(

    @field:Json(name = "dt")
    var dt: Long? = null,

    @field:Json(name = "precipitation")
    var precipitation: Precipitation? = null,

    @field:Json(name = "uv")
    var uv: Float? = null,

    @field:Json(name = "T")
    var T: DailyForecastTemp? = null,

    @field:Json(name = "weather12H")
    var weather12H: Weather12H? = null,

    @field:Json(name = "humidity")
    var humidity: Humidity? = null,

    @field:Json(name = "sun")
    var sun: Sun? = null
)

@JsonClass(generateAdapter = true)
data class Snow(

    @field:Json(name = "6h")
    var jsonMember6h: Float? = null,

    @field:Json(name = "3h")
    var jsonMember3h: Float? = null,

    @field:Json(name = "1h")
    var jsonMember1h: Float? = null
)

@JsonClass(generateAdapter = true)
data class Humidity(

    @field:Json(name = "min")
    var min: Int? = null,

    @field:Json(name = "max")
    var max: Int? = null
)

@JsonClass(generateAdapter = true)
data class ProbabilityForecastItem(

    @field:Json(name = "dt")
    var dt: Long? = null,

    @field:Json(name = "rain")
    var rain: Rain? = null,

    @field:Json(name = "freezing")
    var freezing: Int? = null,

    @field:Json(name = "snow")
    var snow: Snow? = null
)

@JsonClass(generateAdapter = true)
data class Wind(

    @field:Json(name = "icon")
    var icon: String? = null,

    @field:Json(name = "speed")
    var speed: Float? = null,

    @field:Json(name = "gust")
    var gust: Float? = null,

    @field:Json(name = "direction")
    var direction: Int? = null
)
