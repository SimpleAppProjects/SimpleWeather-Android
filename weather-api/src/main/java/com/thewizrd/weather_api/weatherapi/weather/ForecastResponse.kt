package com.thewizrd.weather_api.weatherapi.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ForecastResponse(

    @field:Json(name = "alerts")
		var alerts: Alerts? = null,

    @field:Json(name = "current")
		var current: Current? = null,

    @field:Json(name = "location")
		var location: Location? = null,

    @field:Json(name = "forecast")
		var forecast: Forecast? = null
)

@JsonClass(generateAdapter = true)
data class AlertItem(

    @field:Json(name = "severity")
		var severity: String? = null,

    @field:Json(name = "note")
		var note: String? = null,

    @field:Json(name = "expires")
		var expires: String? = null,

    @field:Json(name = "certainty")
		var certainty: String? = null,

    @field:Json(name = "areas")
		var areas: String? = null,

    @field:Json(name = "effective")
		var effective: String? = null,

    @field:Json(name = "urgency")
		var urgency: String? = null,

    @field:Json(name = "instruction")
		var instruction: String? = null,

    @field:Json(name = "category")
		var category: String? = null,

    @field:Json(name = "event")
		var event: String? = null,

    @field:Json(name = "headline")
		var headline: String? = null,

    @field:Json(name = "msgtype")
		var msgtype: String? = null,

    @field:Json(name = "desc")
		var desc: String? = null
)

@JsonClass(generateAdapter = true)
data class HourItem(

    @field:Json(name = "feelslike_c")
		var feelslikeC: Float? = null,

    @field:Json(name = "feelslike_f")
		var feelslikeF: Float? = null,

    @field:Json(name = "wind_degree")
		var windDegree: Int? = null,

    @field:Json(name = "windchill_f")
		var windchillF: Float? = null,

    @field:Json(name = "windchill_c")
		var windchillC: Float? = null,

    @field:Json(name = "temp_c")
		var tempC: Float? = null,

    @field:Json(name = "temp_f")
		var tempF: Float? = null,

    @field:Json(name = "cloud")
		var cloud: Int? = null,

    @field:Json(name = "wind_kph")
		var windKph: Float? = null,

    @field:Json(name = "wind_mph")
		var windMph: Float? = null,

    @field:Json(name = "humidity")
		var humidity: Int? = null,

    @field:Json(name = "dewpoint_f")
		var dewpointF: Float? = null,

    @field:Json(name = "will_it_rain")
		var willItRain: Int? = null,

    @field:Json(name = "uv")
		var uv: Float? = null,

    @field:Json(name = "heatindex_f")
		var heatindexF: Float? = null,

    @field:Json(name = "dewpoint_c")
		var dewpointC: Float? = null,

    @field:Json(name = "is_day")
    @get:JvmName("getIsDay")
		@set:JvmName("setIsDay")
		var isDay: Int? = null,

    @field:Json(name = "precip_in")
		var precipIn: Float? = null,

    @field:Json(name = "heatindex_c")
		var heatindexC: Float? = null,

    @field:Json(name = "wind_dir")
		var windDir: String? = null,

    @field:Json(name = "gust_mph")
		var gustMph: Float? = null,

    @field:Json(name = "pressure_in")
		var pressureIn: Float? = null,

    @field:Json(name = "chance_of_rain")
		var chanceOfRain: String? = null,

    @field:Json(name = "gust_kph")
		var gustKph: Float? = null,

    @field:Json(name = "precip_mm")
		var precipMm: Float? = null,

    @field:Json(name = "condition")
		var condition: Condition? = null,

    @field:Json(name = "will_it_snow")
		var willItSnow: Int? = null,

    @field:Json(name = "vis_km")
		var visKm: Float? = null,

    @field:Json(name = "time_epoch")
		var timeEpoch: Int? = null,

    @field:Json(name = "time")
		var time: String? = null,

    @field:Json(name = "chance_of_snow")
		var chanceOfSnow: String? = null,

    @field:Json(name = "pressure_mb")
		var pressureMb: Float? = null,

    @field:Json(name = "vis_miles")
		var visMiles: Float? = null
)

@JsonClass(generateAdapter = true)
data class Astro(

    @field:Json(name = "moonset")
		var moonset: String? = null,

    @field:Json(name = "moon_illumination")
		var moonIllumination: String? = null,

    @field:Json(name = "sunrise")
		var sunrise: String? = null,

    @field:Json(name = "moon_phase")
		var moonPhase: String? = null,

    @field:Json(name = "sunset")
		var sunset: String? = null,

    @field:Json(name = "moonrise")
		var moonrise: String? = null
)

@JsonClass(generateAdapter = true)
data class Alerts(

    @field:Json(name = "alert")
		var alert: List<AlertItem>? = null
)

@JsonClass(generateAdapter = true)
data class Day(

    @field:Json(name = "avgvis_km")
		var avgvisKm: Float? = null,

    @field:Json(name = "uv")
		var uv: Float? = null,

    @field:Json(name = "avgtemp_f")
		var avgtempF: Float? = null,

    @field:Json(name = "avgtemp_c")
		var avgtempC: Float? = null,

    @field:Json(name = "daily_chance_of_snow")
		var dailyChanceOfSnow: String? = null,

    @field:Json(name = "maxtemp_c")
		var maxtempC: Float? = null,

    @field:Json(name = "maxtemp_f")
		var maxtempF: Float? = null,

    @field:Json(name = "mintemp_c")
		var mintempC: Float? = null,

    @field:Json(name = "avgvis_miles")
		var avgvisMiles: Float? = null,

    @field:Json(name = "daily_will_it_rain")
		var dailyWillItRain: Int? = null,

    @field:Json(name = "mintemp_f")
		var mintempF: Float? = null,

    @field:Json(name = "totalprecip_in")
		var totalprecipIn: Float? = null,

    @field:Json(name = "avghumidity")
		var avghumidity: Float? = null,

    @field:Json(name = "condition")
		var condition: Condition? = null,

    @field:Json(name = "maxwind_kph")
		var maxwindKph: Float? = null,

    @field:Json(name = "maxwind_mph")
		var maxwindMph: Float? = null,

    @field:Json(name = "daily_chance_of_rain")
		var dailyChanceOfRain: String? = null,

    @field:Json(name = "totalprecip_mm")
		var totalprecipMm: Float? = null,

    @field:Json(name = "daily_will_it_snow")
		var dailyWillItSnow: Int? = null
)

@JsonClass(generateAdapter = true)
data class Forecast(

    @field:Json(name = "forecastday")
		var forecastday: List<ForecastdayItem>? = null
)

@JsonClass(generateAdapter = true)
data class Current(

    @field:Json(name = "feelslike_c")
		var feelslikeC: Float? = null,

    @field:Json(name = "uv")
		var uv: Float? = null,

    @field:Json(name = "last_updated")
		var lastUpdated: String? = null,

    @field:Json(name = "feelslike_f")
		var feelslikeF: Float? = null,

    @field:Json(name = "wind_degree")
		var windDegree: Int? = null,

    @field:Json(name = "last_updated_epoch")
		var lastUpdatedEpoch: Int? = null,

    @field:Json(name = "is_day")
    @get:JvmName("getIsDay")
		@set:JvmName("setIsDay")
		var isDay: Int? = null,

    @field:Json(name = "precip_in")
		var precipIn: Float? = null,

    @field:Json(name = "air_quality")
		var airQuality: AirQuality? = null,

    @field:Json(name = "wind_dir")
		var windDir: String? = null,

    @field:Json(name = "gust_mph")
		var gustMph: Float? = null,

    @field:Json(name = "temp_c")
		var tempC: Float? = null,

    @field:Json(name = "pressure_in")
		var pressureIn: Float? = null,

    @field:Json(name = "gust_kph")
		var gustKph: Float? = null,

    @field:Json(name = "temp_f")
		var tempF: Float? = null,

    @field:Json(name = "precip_mm")
		var precipMm: Float? = null,

    @field:Json(name = "cloud")
		var cloud: Int? = null,

    @field:Json(name = "wind_kph")
		var windKph: Float? = null,

    @field:Json(name = "condition")
		var condition: Condition? = null,

    @field:Json(name = "wind_mph")
		var windMph: Float? = null,

    @field:Json(name = "vis_km")
		var visKm: Float? = null,

    @field:Json(name = "humidity")
		var humidity: Int? = null,

    @field:Json(name = "pressure_mb")
		var pressureMb: Float? = null,

    @field:Json(name = "vis_miles")
		var visMiles: Float? = null
)

@JsonClass(generateAdapter = true)
data class Condition(

    @field:Json(name = "code")
		var code: Int? = null,

    @field:Json(name = "icon")
		var icon: String? = null,

    @field:Json(name = "text")
		var text: String? = null
)

@JsonClass(generateAdapter = true)
data class ForecastdayItem(

    @field:Json(name = "date")
		var date: String? = null,

    @field:Json(name = "astro")
		var astro: Astro? = null,

    @field:Json(name = "date_epoch")
		var dateEpoch: Int? = null,

    @field:Json(name = "hour")
		var hour: List<HourItem>? = null,

    @field:Json(name = "day")
		var day: Day? = null
)

@JsonClass(generateAdapter = true)
data class Location(

    @field:Json(name = "localtime")
		var localtime: String? = null,

    @field:Json(name = "country")
		var country: String? = null,

    @field:Json(name = "localtime_epoch")
		var localtimeEpoch: Int? = null,

    @field:Json(name = "name")
		var name: String? = null,

    @field:Json(name = "lon")
		var lon: Float? = null,

    @field:Json(name = "region")
		var region: String? = null,

    @field:Json(name = "lat")
		var lat: Float? = null,

    @field:Json(name = "tz_id")
		var tzId: String? = null
)

@JsonClass(generateAdapter = true)
data class AirQuality(

    @field:Json(name = "no2")
		var no2: Double? = null,

    @field:Json(name = "o3")
		var o3: Double? = null,

    @field:Json(name = "us-epa-index")
		var usEpaIndex: Int? = null,

    @field:Json(name = "so2")
		var so2: Double? = null,

    @field:Json(name = "pm2_5")
		var pm25: Double? = null,

    @field:Json(name = "pm10")
		var pm10: Double? = null,

    @field:Json(name = "co")
		var co: Double? = null,

    @field:Json(name = "gb-defra-index")
		var gbDefraIndex: Int? = null
)
