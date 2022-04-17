package com.thewizrd.shared_resources.weatherdata.weatherbit

import com.google.gson.annotations.SerializedName
import com.vimeo.stag.UseStag

@UseStag(UseStag.FieldOption.ALL)
data class CurrentResponse(

	@field:SerializedName("alerts")
	var alerts: List<AlertsItem?>? = null,

	@field:SerializedName("data")
	var data: List<CurrentDataItem?>? = null,

	@field:SerializedName("count")
	var count: Int? = null,

	@field:SerializedName("minutely")
	var minutely: List<MinutelyItem?>? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class CurrentDataItem(

	@field:SerializedName("sunrise")
	var sunrise: String? = null,

	@field:SerializedName("pod")
	var pod: String? = null,

	@field:SerializedName("pres")
	var pres: Float? = null,

	@field:SerializedName("timezone")
	var timezone: String? = null,

	@field:SerializedName("ob_time")
	var obTime: String? = null,

	@field:SerializedName("wind_cdir")
	var windCdir: String? = null,

	@field:SerializedName("lon")
	var lon: Float? = null,

	@field:SerializedName("clouds")
	var clouds: Int? = null,

	@field:SerializedName("wind_spd")
	var windSpd: Float? = null,

	@field:SerializedName("city_name")
	var cityName: String? = null,

	@field:SerializedName("h_angle")
	var hAngle: Float? = null,

	@field:SerializedName("datetime")
	var datetime: String? = null,

	@field:SerializedName("precip")
	var precip: Float? = null,

	@field:SerializedName("weather")
	var weather: CurrentWeather? = null,

	@field:SerializedName("station")
	var station: String? = null,

	@field:SerializedName("elev_angle")
	var elevAngle: Float? = null,

	@field:SerializedName("dni")
	var dni: Float? = null,

	@field:SerializedName("lat")
	var lat: Float? = null,

	@field:SerializedName("vis")
	var vis: Float? = null,

	@field:SerializedName("uv")
	var uv: Float? = null,

	@field:SerializedName("temp")
	var temp: Float? = null,

	@field:SerializedName("dhi")
	var dhi: Float? = null,

	@field:SerializedName("ghi")
	var ghi: Float? = null,

	@field:SerializedName("app_temp")
	var appTemp: Float? = null,

	@field:SerializedName("dewpt")
	var dewpt: Float? = null,

	@field:SerializedName("wind_dir")
	var windDir: Int? = null,

	@field:SerializedName("solar_rad")
	var solarRad: Float? = null,

	@field:SerializedName("country_code")
	var countryCode: String? = null,

	@field:SerializedName("rh")
	var rh: Float? = null,

	@field:SerializedName("slp")
	var slp: Float? = null,

	@field:SerializedName("snow")
	var snow: Float? = null,

	@field:SerializedName("sunset")
	var sunset: String? = null,

	@field:SerializedName("aqi")
	var aqi: Int? = null,

	@field:SerializedName("state_code")
	var stateCode: String? = null,

	@field:SerializedName("wind_cdir_full")
	var windCdirFull: String? = null,

	@field:SerializedName("ts")
	var ts: Int? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class AlertsItem(

	@field:SerializedName("severity")
	var severity: String? = null,

	@field:SerializedName("ends_local")
	var endsLocal: String? = null,

	@field:SerializedName("regions")
	var regions: List<String?>? = null,

	@field:SerializedName("expires_local")
	var expiresLocal: String? = null,

	@field:SerializedName("description")
	var description: String? = null,

	@field:SerializedName("onset_utc")
	var onsetUtc: String? = null,

	@field:SerializedName("title")
	var title: String? = null,

	@field:SerializedName("expires_utc")
	var expiresUtc: String? = null,

	@field:SerializedName("uri")
	var uri: String? = null,

	@field:SerializedName("onset_local")
	var onsetLocal: String? = null,

	@field:SerializedName("effective_local")
	var effectiveLocal: String? = null,

	@field:SerializedName("ends_utc")
	var endsUtc: String? = null,

	@field:SerializedName("effective_utc")
	var effectiveUtc: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class MinutelyItem(

	@field:SerializedName("temp")
	var temp: Float? = null,

	@field:SerializedName("timestamp_local")
	var timestampLocal: String? = null,

	@field:SerializedName("precip")
	var precip: Float? = null,

	@field:SerializedName("timestamp_utc")
	var timestampUtc: String? = null,

	@field:SerializedName("snow")
	var snow: Float? = null,

	@field:SerializedName("ts")
	var ts: Int? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class CurrentWeather(

	@field:SerializedName("code")
	var code: Int? = null,

	@field:SerializedName("icon")
	var icon: String? = null,

	@field:SerializedName("description")
	var description: String? = null
)
