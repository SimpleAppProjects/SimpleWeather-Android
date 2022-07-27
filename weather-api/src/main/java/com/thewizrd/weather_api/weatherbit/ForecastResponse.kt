package com.thewizrd.weather_api.weatherbit

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ForecastResponse(

    @field:Json(name = "country_code")
    var countryCode: String? = null,

    @field:Json(name = "city_name")
    var cityName: String? = null,

    @field:Json(name = "data")
    var data: List<ForecastDataItem?>? = null,

    @field:Json(name = "timezone")
    var timezone: String? = null,

    @field:Json(name = "lon")
    var lon: Float? = null,

    @field:Json(name = "state_code")
    var stateCode: String? = null,

    @field:Json(name = "lat")
    var lat: Float? = null
)

@JsonClass(generateAdapter = true)
data class ForecastWeather(

    @field:Json(name = "code")
    var code: Int? = null,

    @field:Json(name = "icon")
    var icon: String? = null,

    @field:Json(name = "description")
    var description: String? = null
)

@JsonClass(generateAdapter = true)
data class ForecastDataItem(

    @field:Json(name = "pres")
    var pres: Float? = null,

    @field:Json(name = "moon_phase")
    var moonPhase: Float? = null,

    @field:Json(name = "wind_cdir")
    var windCdir: String? = null,

    @field:Json(name = "moonrise_ts")
    var moonriseTs: Int? = null,

    @field:Json(name = "clouds")
    var clouds: Int? = null,

    @field:Json(name = "low_temp")
    var lowTemp: Float? = null,

    @field:Json(name = "wind_spd")
    var windSpd: Float? = null,

    @field:Json(name = "ozone")
    var ozone: Float? = null,

    @field:Json(name = "pop")
    var pop: Int? = null,

    @field:Json(name = "valid_date")
    var validDate: String? = null,

    @field:Json(name = "datetime")
    var datetime: String? = null,

    @field:Json(name = "precip")
    var precip: Float? = null,

    @field:Json(name = "sunrise_ts")
    var sunriseTs: Int? = null,

    @field:Json(name = "min_temp")
    var minTemp: Float? = null,

    @field:Json(name = "weather")
    var weather: ForecastWeather? = null,

    @field:Json(name = "app_max_temp")
    var appMaxTemp: Float? = null,

    @field:Json(name = "max_temp")
    var maxTemp: Float? = null,

    @field:Json(name = "snow_depth")
    var snowDepth: Float? = null,

    @field:Json(name = "sunset_ts")
    var sunsetTs: Int? = null,

    @field:Json(name = "max_dhi")
    var maxDhi: Any? = null,

    @field:Json(name = "clouds_mid")
    var cloudsMid: Int? = null,

    @field:Json(name = "vis")
    var vis: Float? = null,

    @field:Json(name = "uv")
    var uv: Float? = null,

    @field:Json(name = "high_temp")
    var highTemp: Float? = null,

    @field:Json(name = "temp")
    var temp: Float? = null,

    @field:Json(name = "clouds_hi")
    var cloudsHi: Int? = null,

    @field:Json(name = "app_min_temp")
    var appMinTemp: Float? = null,

    @field:Json(name = "moon_phase_lunation")
    var moonPhaseLunation: Float? = null,

    @field:Json(name = "dewpt")
    var dewpt: Float? = null,

    @field:Json(name = "wind_dir")
    var windDir: Int? = null,

    @field:Json(name = "wind_gust_spd")
    var windGustSpd: Float? = null,

    @field:Json(name = "clouds_low")
    var cloudsLow: Int? = null,

    @field:Json(name = "rh")
    var rh: Int? = null,

    @field:Json(name = "slp")
    var slp: Float? = null,

    @field:Json(name = "snow")
    var snow: Float? = null,

    @field:Json(name = "wind_cdir_full")
    var windCdirFull: String? = null,

    @field:Json(name = "moonset_ts")
    var moonsetTs: Int? = null,

    @field:Json(name = "ts")
    var ts: Int? = null
)
