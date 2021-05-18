package com.thewizrd.shared_resources.weatherdata.weatherapi

import com.google.gson.annotations.SerializedName
import com.vimeo.stag.UseStag

@UseStag(UseStag.FieldOption.ALL)
data class ForecastResponse(

		@field:SerializedName("alerts")
		var alerts: Alerts? = null,

		@field:SerializedName("current")
		var current: Current? = null,

		@field:SerializedName("location")
		var location: Location? = null,

		@field:SerializedName("forecast")
		var forecast: Forecast? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class AlertItem(

		@field:SerializedName("severity")
		var severity: String? = null,

		@field:SerializedName("note")
		var note: String? = null,

		@field:SerializedName("expires")
		var expires: String? = null,

		@field:SerializedName("certainty")
		var certainty: String? = null,

		@field:SerializedName("areas")
		var areas: String? = null,

		@field:SerializedName("effective")
		var effective: String? = null,

		@field:SerializedName("urgency")
		var urgency: String? = null,

		@field:SerializedName("instruction")
		var instruction: String? = null,

		@field:SerializedName("category")
		var category: String? = null,

		@field:SerializedName("event")
		var event: String? = null,

		@field:SerializedName("headline")
		var headline: String? = null,

		@field:SerializedName("msgtype")
		var msgtype: String? = null,

		@field:SerializedName("desc")
		var desc: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class HourItem(

		@field:SerializedName("feelslike_c")
		var feelslikeC: Float? = null,

		@field:SerializedName("feelslike_f")
		var feelslikeF: Float? = null,

		@field:SerializedName("wind_degree")
		var windDegree: Int? = null,

		@field:SerializedName("windchill_f")
		var windchillF: Float? = null,

		@field:SerializedName("windchill_c")
		var windchillC: Float? = null,

		@field:SerializedName("temp_c")
		var tempC: Float? = null,

		@field:SerializedName("temp_f")
		var tempF: Float? = null,

		@field:SerializedName("cloud")
		var cloud: Int? = null,

		@field:SerializedName("wind_kph")
		var windKph: Float? = null,

		@field:SerializedName("wind_mph")
		var windMph: Float? = null,

		@field:SerializedName("humidity")
		var humidity: Int? = null,

		@field:SerializedName("dewpoint_f")
		var dewpointF: Float? = null,

		@field:SerializedName("will_it_rain")
		var willItRain: Int? = null,

		@field:SerializedName("uv")
		var uv: Float? = null,

		@field:SerializedName("heatindex_f")
		var heatindexF: Float? = null,

		@field:SerializedName("dewpoint_c")
		var dewpointC: Float? = null,

		@field:SerializedName("is_day")
		@get:JvmName("getIsDay")
		@set:JvmName("setIsDay")
		var isDay: Int? = null,

		@field:SerializedName("precip_in")
		var precipIn: Float? = null,

		@field:SerializedName("heatindex_c")
		var heatindexC: Float? = null,

		@field:SerializedName("wind_dir")
		var windDir: String? = null,

		@field:SerializedName("gust_mph")
		var gustMph: Float? = null,

		@field:SerializedName("pressure_in")
		var pressureIn: Float? = null,

		@field:SerializedName("chance_of_rain")
		var chanceOfRain: String? = null,

		@field:SerializedName("gust_kph")
		var gustKph: Float? = null,

		@field:SerializedName("precip_mm")
		var precipMm: Float? = null,

		@field:SerializedName("condition")
		var condition: Condition? = null,

		@field:SerializedName("will_it_snow")
		var willItSnow: Int? = null,

		@field:SerializedName("vis_km")
		var visKm: Float? = null,

		@field:SerializedName("time_epoch")
		var timeEpoch: Int? = null,

		@field:SerializedName("time")
		var time: String? = null,

		@field:SerializedName("chance_of_snow")
		var chanceOfSnow: String? = null,

		@field:SerializedName("pressure_mb")
		var pressureMb: Float? = null,

		@field:SerializedName("vis_miles")
		var visMiles: Float? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Astro(

		@field:SerializedName("moonset")
		var moonset: String? = null,

		@field:SerializedName("moon_illumination")
		var moonIllumination: String? = null,

		@field:SerializedName("sunrise")
		var sunrise: String? = null,

		@field:SerializedName("moon_phase")
		var moonPhase: String? = null,

		@field:SerializedName("sunset")
		var sunset: String? = null,

		@field:SerializedName("moonrise")
		var moonrise: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Alerts(

		@field:SerializedName("alert")
		var alert: List<AlertItem>? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Day(

		@field:SerializedName("avgvis_km")
		var avgvisKm: Float? = null,

		@field:SerializedName("uv")
		var uv: Float? = null,

		@field:SerializedName("avgtemp_f")
		var avgtempF: Float? = null,

		@field:SerializedName("avgtemp_c")
		var avgtempC: Float? = null,

		@field:SerializedName("daily_chance_of_snow")
		var dailyChanceOfSnow: String? = null,

		@field:SerializedName("maxtemp_c")
		var maxtempC: Float? = null,

		@field:SerializedName("maxtemp_f")
		var maxtempF: Float? = null,

		@field:SerializedName("mintemp_c")
		var mintempC: Float? = null,

		@field:SerializedName("avgvis_miles")
		var avgvisMiles: Float? = null,

		@field:SerializedName("daily_will_it_rain")
		var dailyWillItRain: Int? = null,

		@field:SerializedName("mintemp_f")
		var mintempF: Float? = null,

		@field:SerializedName("totalprecip_in")
		var totalprecipIn: Float? = null,

		@field:SerializedName("avghumidity")
		var avghumidity: Float? = null,

		@field:SerializedName("condition")
		var condition: Condition? = null,

		@field:SerializedName("maxwind_kph")
		var maxwindKph: Float? = null,

		@field:SerializedName("maxwind_mph")
		var maxwindMph: Float? = null,

		@field:SerializedName("daily_chance_of_rain")
		var dailyChanceOfRain: String? = null,

		@field:SerializedName("totalprecip_mm")
		var totalprecipMm: Float? = null,

		@field:SerializedName("daily_will_it_snow")
		var dailyWillItSnow: Int? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Forecast(

		@field:SerializedName("forecastday")
		var forecastday: List<ForecastdayItem>? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Current(

		@field:SerializedName("feelslike_c")
		var feelslikeC: Float? = null,

		@field:SerializedName("uv")
		var uv: Float? = null,

		@field:SerializedName("last_updated")
		var lastUpdated: String? = null,

		@field:SerializedName("feelslike_f")
		var feelslikeF: Float? = null,

		@field:SerializedName("wind_degree")
		var windDegree: Int? = null,

		@field:SerializedName("last_updated_epoch")
		var lastUpdatedEpoch: Int? = null,

		@field:SerializedName("is_day")
		@get:JvmName("getIsDay")
		@set:JvmName("setIsDay")
		var isDay: Int? = null,

		@field:SerializedName("precip_in")
		var precipIn: Float? = null,

		@field:SerializedName("air_quality")
		var airQuality: AirQuality? = null,

		@field:SerializedName("wind_dir")
		var windDir: String? = null,

		@field:SerializedName("gust_mph")
		var gustMph: Float? = null,

		@field:SerializedName("temp_c")
		var tempC: Float? = null,

		@field:SerializedName("pressure_in")
		var pressureIn: Float? = null,

		@field:SerializedName("gust_kph")
		var gustKph: Float? = null,

		@field:SerializedName("temp_f")
		var tempF: Float? = null,

		@field:SerializedName("precip_mm")
		var precipMm: Float? = null,

		@field:SerializedName("cloud")
		var cloud: Int? = null,

		@field:SerializedName("wind_kph")
		var windKph: Float? = null,

		@field:SerializedName("condition")
		var condition: Condition? = null,

		@field:SerializedName("wind_mph")
		var windMph: Float? = null,

		@field:SerializedName("vis_km")
		var visKm: Float? = null,

		@field:SerializedName("humidity")
		var humidity: Int? = null,

		@field:SerializedName("pressure_mb")
		var pressureMb: Float? = null,

		@field:SerializedName("vis_miles")
		var visMiles: Float? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Condition(

		@field:SerializedName("code")
		var code: Int? = null,

		@field:SerializedName("icon")
		var icon: String? = null,

		@field:SerializedName("text")
		var text: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class ForecastdayItem(

		@field:SerializedName("date")
		var date: String? = null,

		@field:SerializedName("astro")
		var astro: Astro? = null,

		@field:SerializedName("date_epoch")
		var dateEpoch: Int? = null,

		@field:SerializedName("hour")
		var hour: List<HourItem>? = null,

		@field:SerializedName("day")
		var day: Day? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Location(

		@field:SerializedName("localtime")
		var localtime: String? = null,

		@field:SerializedName("country")
		var country: String? = null,

		@field:SerializedName("localtime_epoch")
		var localtimeEpoch: Int? = null,

		@field:SerializedName("name")
		var name: String? = null,

		@field:SerializedName("lon")
		var lon: Float? = null,

		@field:SerializedName("region")
		var region: String? = null,

		@field:SerializedName("lat")
		var lat: Float? = null,

		@field:SerializedName("tz_id")
		var tzId: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class AirQuality(

		@field:SerializedName("no2")
		var no2: Double? = null,

		@field:SerializedName("o3")
		var o3: Double? = null,

		@field:SerializedName("us-epa-index")
		var usEpaIndex: Int? = null,

		@field:SerializedName("so2")
		var so2: Double? = null,

		@field:SerializedName("pm2_5")
		var pm25: Double? = null,

		@field:SerializedName("pm10")
		var pm10: Double? = null,

		@field:SerializedName("co")
		var co: Double? = null,

		@field:SerializedName("gb-defra-index")
		var gbDefraIndex: Int? = null
)
