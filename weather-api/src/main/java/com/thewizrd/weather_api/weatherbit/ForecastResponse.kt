package com.thewizrd.weather_api.weatherbit

import com.google.gson.annotations.SerializedName
import com.vimeo.stag.UseStag

@UseStag(UseStag.FieldOption.ALL)
data class ForecastResponse(

    @field:SerializedName("country_code")
    var countryCode: String? = null,

    @field:SerializedName("city_name")
    var cityName: String? = null,

    @field:SerializedName("data")
    var data: List<ForecastDataItem?>? = null,

    @field:SerializedName("timezone")
    var timezone: String? = null,

    @field:SerializedName("lon")
    var lon: Float? = null,

    @field:SerializedName("state_code")
    var stateCode: String? = null,

    @field:SerializedName("lat")
    var lat: Float? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class ForecastWeather(

    @field:SerializedName("code")
    var code: Int? = null,

    @field:SerializedName("icon")
    var icon: String? = null,

    @field:SerializedName("description")
    var description: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class ForecastDataItem(

    @field:SerializedName("pres")
    var pres: Float? = null,

    @field:SerializedName("moon_phase")
    var moonPhase: Float? = null,

    @field:SerializedName("wind_cdir")
    var windCdir: String? = null,

    @field:SerializedName("moonrise_ts")
    var moonriseTs: Int? = null,

    @field:SerializedName("clouds")
    var clouds: Int? = null,

    @field:SerializedName("low_temp")
    var lowTemp: Float? = null,

    @field:SerializedName("wind_spd")
    var windSpd: Float? = null,

    @field:SerializedName("ozone")
    var ozone: Float? = null,

    @field:SerializedName("pop")
    var pop: Int? = null,

    @field:SerializedName("valid_date")
    var validDate: String? = null,

    @field:SerializedName("datetime")
    var datetime: String? = null,

    @field:SerializedName("precip")
    var precip: Float? = null,

    @field:SerializedName("sunrise_ts")
    var sunriseTs: Int? = null,

    @field:SerializedName("min_temp")
    var minTemp: Float? = null,

    @field:SerializedName("weather")
    var weather: ForecastWeather? = null,

    @field:SerializedName("app_max_temp")
    var appMaxTemp: Float? = null,

    @field:SerializedName("max_temp")
    var maxTemp: Float? = null,

    @field:SerializedName("snow_depth")
    var snowDepth: Float? = null,

    @field:SerializedName("sunset_ts")
    var sunsetTs: Int? = null,

    @field:SerializedName("max_dhi")
    var maxDhi: Any? = null,

    @field:SerializedName("clouds_mid")
    var cloudsMid: Int? = null,

    @field:SerializedName("vis")
    var vis: Float? = null,

    @field:SerializedName("uv")
    var uv: Float? = null,

    @field:SerializedName("high_temp")
    var highTemp: Float? = null,

    @field:SerializedName("temp")
    var temp: Float? = null,

    @field:SerializedName("clouds_hi")
    var cloudsHi: Int? = null,

    @field:SerializedName("app_min_temp")
    var appMinTemp: Float? = null,

    @field:SerializedName("moon_phase_lunation")
    var moonPhaseLunation: Float? = null,

    @field:SerializedName("dewpt")
    var dewpt: Float? = null,

    @field:SerializedName("wind_dir")
    var windDir: Int? = null,

    @field:SerializedName("wind_gust_spd")
    var windGustSpd: Float? = null,

    @field:SerializedName("clouds_low")
    var cloudsLow: Int? = null,

    @field:SerializedName("rh")
    var rh: Int? = null,

    @field:SerializedName("slp")
    var slp: Float? = null,

    @field:SerializedName("snow")
    var snow: Float? = null,

    @field:SerializedName("wind_cdir_full")
    var windCdirFull: String? = null,

    @field:SerializedName("moonset_ts")
    var moonsetTs: Int? = null,

    @field:SerializedName("ts")
    var ts: Int? = null
)
