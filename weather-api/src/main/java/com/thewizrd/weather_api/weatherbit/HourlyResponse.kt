package com.thewizrd.weather_api.weatherbit

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HourlyResponse(

	@field:Json(name = "country_code")
	var countryCode: String? = null,

	@field:Json(name = "city_name")
	var cityName: String? = null,

	@field:Json(name = "data")
	var data: List<HourlyForecastDataItem?>? = null,

	@field:Json(name = "timezone")
	var timezone: String? = null,

	@field:Json(name = "lon")
	var lon: Float? = null,

	@field:Json(name = "state_code")
	var stateCode: String? = null,

	@field:Json(name = "lat")
	var lat: Float? = null
)

/*
@JsonClass(generateAdapter = true)
data class Weather(

	@field:Json(name="code")
	var code: Int? = null,

	@field:Json(name="icon")
	var icon: String? = null,

	@field:Json(name="description")
	var description: String? = null
)
 */

@JsonClass(generateAdapter = true)
data class HourlyForecastDataItem(

	@field:Json(name = "pres")
	var pres: Float? = null,

	@field:Json(name = "pod")
	var pod: String? = null,

	@field:Json(name = "wind_cdir")
	var windCdir: String? = null,

	@field:Json(name = "clouds")
	var clouds: Int? = null,

	@field:Json(name = "wind_spd")
	var windSpd: Float? = null,

	@field:Json(name = "pop")
	var pop: Int? = null,

	@field:Json(name = "datetime")
	var datetime: String? = null,

	@field:Json(name = "timestamp_local")
	var timestampLocal: String? = null,

	@field:Json(name = "precip")
	var precip: Float? = null,

	@field:Json(name = "timestamp_utc")
	var timestampUtc: String? = null,

	@field:Json(name = "weather")
	var weather: ForecastWeather? = null,

	@field:Json(name = "snow_depth")
	var snowDepth: Float? = null,

	@field:Json(name = "dni")
	var dni: Int? = null,

	@field:Json(name = "uv")
	var uv: Float? = null,

	@field:Json(name = "vis")
	var vis: Float? = null,

	@field:Json(name = "temp")
	var temp: Float? = null,

	@field:Json(name = "dhi")
	var dhi: Float? = null,

	@field:Json(name = "app_temp")
	var appTemp: Float? = null,

	@field:Json(name = "ghi")
	var ghi: Float? = null,

	@field:Json(name = "dewpt")
	var dewpt: Float? = null,

	@field:Json(name = "wind_dir")
	var windDir: Int? = null,

	@field:Json(name = "solar_rad")
	var solarRad: Float? = null,

	@field:Json(name = "wind_gust_spd")
	var windGustSpd: Float? = null,

	@field:Json(name = "snow")
	var snow: Float? = null,

	@field:Json(name = "rh")
	var rh: Int? = null,

	@field:Json(name = "slp")
	var slp: Float? = null,

	@field:Json(name = "wind_cdir_full")
	var windCdirFull: String? = null,

	@field:Json(name = "ts")
	var ts: Int? = null
)
