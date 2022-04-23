package com.thewizrd.weather_api.meteofrance.weather

import com.google.gson.annotations.SerializedName
import com.vimeo.stag.UseStag

@UseStag(UseStag.FieldOption.ALL)
data class ForecastResponse(

    @field:SerializedName("updated_on")
    var updatedOn: Long? = null,

    @field:SerializedName("probability_forecast")
    var probabilityForecast: List<ProbabilityForecastItem?>? = null,

    @field:SerializedName("forecast")
    var forecast: List<ForecastItem?>? = null,

    @field:SerializedName("position")
    var position: ForecastPosition? = null,

    @field:SerializedName("daily_forecast")
    var dailyForecast: List<DailyForecastItem?>? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Rain(

    @field:SerializedName("6h")
    var jsonMember6h: Float? = null,

    @field:SerializedName("3h")
    var jsonMember3h: Float? = null,

    @field:SerializedName("1h")
    var jsonMember1h: Float? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Weather12H(

    @field:SerializedName("icon")
    var icon: String? = null,

    @field:SerializedName("desc")
    var desc: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class ForecastItem(

    @field:SerializedName("dt")
    var dt: Long? = null,

    @field:SerializedName("rain")
    var rain: Rain? = null,

    @field:SerializedName("T")
    var T: ForecastTemp? = null,

    @field:SerializedName("snow")
    var snow: Snow? = null,

    @field:SerializedName("weather")
    var weather: Weather? = null,

    @field:SerializedName("humidity")
    var humidity: Int? = null,

    @field:SerializedName("rain snow limit")
    var rainSnowLimit: String? = null,

    @field:SerializedName("iso0")
    var iso0: Int? = null,

    @field:SerializedName("sea_level")
    var seaLevel: Float? = null,

    @field:SerializedName("clouds")
    var clouds: Int? = null,

    @field:SerializedName("wind")
    var wind: Wind? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class ForecastPosition(

    @field:SerializedName("bulletin_cote")
    var bulletinCote: Int? = null,

    @field:SerializedName("country")
    var country: String? = null,

    @field:SerializedName("insee")
    var insee: String? = null,

    @field:SerializedName("timezone")
    var timezone: String? = null,

    @field:SerializedName("name")
    var name: String? = null,

    @field:SerializedName("lon")
    var lon: Float? = null,

    @field:SerializedName("dept")
    var dept: String? = null,

    @field:SerializedName("alti")
    var alti: Int? = null,

    @field:SerializedName("lat")
    var lat: Float? = null,

    @field:SerializedName("rain_product_available")
    var rainProductAvailable: Int? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Weather(

    @field:SerializedName("icon")
    var icon: String? = null,

    @field:SerializedName("desc")
    var desc: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Precipitation(

    @field:SerializedName("24h")
    var jsonMember24h: Float? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class DailyForecastTemp(

    @field:SerializedName("min")
    var min: Float? = null,

    @field:SerializedName("max")
    var max: Float? = null,

    @field:SerializedName("sea")
    var sea: Float? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class ForecastTemp(

    @field:SerializedName("value")
    var value: Float? = null,

    @field:SerializedName("windchill")
    var windchill: Float? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Sun(

    @field:SerializedName("set")
    var set: Long? = null,

    @field:SerializedName("rise")
    var rise: Long? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class DailyForecastItem(

    @field:SerializedName("dt")
    var dt: Long? = null,

    @field:SerializedName("precipitation")
    var precipitation: Precipitation? = null,

    @field:SerializedName("uv")
    var uv: Float? = null,

    @field:SerializedName("T")
    var T: DailyForecastTemp? = null,

    @field:SerializedName("weather12H")
    var weather12H: Weather12H? = null,

    @field:SerializedName("humidity")
    var humidity: Humidity? = null,

    @field:SerializedName("sun")
    var sun: Sun? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Snow(

    @field:SerializedName("6h")
    var jsonMember6h: Float? = null,

    @field:SerializedName("3h")
    var jsonMember3h: Float? = null,

    @field:SerializedName("1h")
    var jsonMember1h: Float? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Humidity(

    @field:SerializedName("min")
    var min: Int? = null,

    @field:SerializedName("max")
    var max: Int? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class ProbabilityForecastItem(

    @field:SerializedName("dt")
    var dt: Long? = null,

    @field:SerializedName("rain")
    var rain: Rain? = null,

    @field:SerializedName("freezing")
    var freezing: Int? = null,

    @field:SerializedName("snow")
    var snow: Snow? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Wind(

    @field:SerializedName("icon")
    var icon: String? = null,

    @field:SerializedName("speed")
    var speed: Float? = null,

    @field:SerializedName("gust")
    var gust: Float? = null,

    @field:SerializedName("direction")
    var direction: Int? = null
)
