package com.thewizrd.weather_api.weatherbit

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CurrentResponse(

    @field:Json(name = "alerts")
	var alerts: List<AlertsItem?>? = null,

    @field:Json(name = "data")
	var data: List<CurrentDataItem?>? = null,

    @field:Json(name = "count")
	var count: Int? = null,

    @field:Json(name = "minutely")
	var minutely: List<MinutelyItem?>? = null
)

@JsonClass(generateAdapter = true)
data class CurrentDataItem(

    @field:Json(name = "sunrise")
	var sunrise: String? = null,

    @field:Json(name = "pod")
	var pod: String? = null,

    @field:Json(name = "pres")
	var pres: Float? = null,

    @field:Json(name = "timezone")
	var timezone: String? = null,

    @field:Json(name = "ob_time")
	var obTime: String? = null,

    @field:Json(name = "wind_cdir")
	var windCdir: String? = null,

    @field:Json(name = "lon")
	var lon: Float? = null,

    @field:Json(name = "clouds")
	var clouds: Int? = null,

    @field:Json(name = "wind_spd")
	var windSpd: Float? = null,

    @field:Json(name = "city_name")
	var cityName: String? = null,

    @field:Json(name = "h_angle")
	var hAngle: Float? = null,

    @field:Json(name = "datetime")
	var datetime: String? = null,

    @field:Json(name = "precip")
	var precip: Float? = null,

    @field:Json(name = "weather")
	var weather: CurrentWeather? = null,

    @field:Json(name = "station")
	var station: String? = null,

    @field:Json(name = "elev_angle")
	var elevAngle: Float? = null,

    @field:Json(name = "dni")
	var dni: Float? = null,

    @field:Json(name = "lat")
	var lat: Float? = null,

    @field:Json(name = "vis")
	var vis: Float? = null,

    @field:Json(name = "uv")
	var uv: Float? = null,

    @field:Json(name = "temp")
	var temp: Float? = null,

    @field:Json(name = "dhi")
	var dhi: Float? = null,

    @field:Json(name = "ghi")
	var ghi: Float? = null,

    @field:Json(name = "app_temp")
	var appTemp: Float? = null,

    @field:Json(name = "dewpt")
	var dewpt: Float? = null,

    @field:Json(name = "wind_dir")
	var windDir: Int? = null,

    @field:Json(name = "solar_rad")
	var solarRad: Float? = null,

    @field:Json(name = "country_code")
	var countryCode: String? = null,

    @field:Json(name = "rh")
	var rh: Float? = null,

    @field:Json(name = "slp")
	var slp: Float? = null,

    @field:Json(name = "snow")
	var snow: Float? = null,

    @field:Json(name = "sunset")
	var sunset: String? = null,

    @field:Json(name = "aqi")
	var aqi: Int? = null,

    @field:Json(name = "state_code")
	var stateCode: String? = null,

    @field:Json(name = "wind_cdir_full")
	var windCdirFull: String? = null,

    @field:Json(name = "ts")
	var ts: Int? = null
)

@JsonClass(generateAdapter = true)
data class AlertsItem(

    @field:Json(name = "severity")
	var severity: String? = null,

    @field:Json(name = "ends_local")
	var endsLocal: String? = null,

    @field:Json(name = "regions")
	var regions: List<String?>? = null,

    @field:Json(name = "expires_local")
	var expiresLocal: String? = null,

    @field:Json(name = "description")
	var description: String? = null,

    @field:Json(name = "onset_utc")
	var onsetUtc: String? = null,

    @field:Json(name = "title")
	var title: String? = null,

    @field:Json(name = "expires_utc")
	var expiresUtc: String? = null,

    @field:Json(name = "uri")
	var uri: String? = null,

    @field:Json(name = "onset_local")
	var onsetLocal: String? = null,

    @field:Json(name = "effective_local")
	var effectiveLocal: String? = null,

    @field:Json(name = "ends_utc")
	var endsUtc: String? = null,

    @field:Json(name = "effective_utc")
	var effectiveUtc: String? = null
)

@JsonClass(generateAdapter = true)
data class MinutelyItem(

    @field:Json(name = "temp")
	var temp: Float? = null,

    @field:Json(name = "timestamp_local")
	var timestampLocal: String? = null,

    @field:Json(name = "precip")
	var precip: Float? = null,

    @field:Json(name = "timestamp_utc")
	var timestampUtc: String? = null,

    @field:Json(name = "snow")
	var snow: Float? = null,

    @field:Json(name = "ts")
	var ts: Int? = null
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(

    @field:Json(name = "code")
	var code: Int? = null,

    @field:Json(name = "icon")
	var icon: String? = null,

    @field:Json(name = "description")
	var description: String? = null
)
