package com.thewizrd.weather_api.meteofrance.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ForecastResponse(

	@Json(name = "update_time")
	val updateTime: String? = null,

	@Json(name = "geometry")
	val geometry: Geometry? = null,

	@Json(name = "type")
	val type: String? = null,

	@Json(name = "properties")
	val properties: ForecastProperties? = null
)

@JsonClass(generateAdapter = true)
data class DailyForecastItem(

	@Json(name = "daily_weather_icon")
	val dailyWeatherIcon: String? = null,

	@Json(name = "sunset_time")
	val sunsetTime: String? = null,

	@Json(name = "daily_weather_description")
	val dailyWeatherDescription: String? = null,

	@Json(name = "uv_index")
	val uvIndex: Float? = null,

	@Json(name = "T_max")
	val tMax: Float? = null,

	@Json(name = "total_precipitation_24h")
	val totalPrecipitation24h: Float? = null,

	@Json(name = "relative_humidity_min")
	val relativeHumidityMin: Int? = null,

	@Json(name = "relative_humidity_max")
	val relativeHumidityMax: Int? = null,

	@Json(name = "sunrise_time")
	val sunriseTime: String? = null,

	@Json(name = "time")
	val time: String? = null,

	@Json(name = "T_min")
	val tMin: Float? = null,

	@Json(name = "T_sea")
	val tSea: Float? = null
)

@JsonClass(generateAdapter = true)
data class ProbabilityForecastItem(

	@Json(name = "freezing_hazard")
	val freezingHazard: Int? = null,

	@Json(name = "snow_hazard_3h")
	val snowHazard3h: Float? = null,

	@Json(name = "snow_hazard_6h")
	val snowHazard6h: Float? = null,

	@Json(name = "rain_hazard_3h")
	val rainHazard3h: Float? = null,

	@Json(name = "rain_hazard_6h")
	val rainHazard6h: Float? = null,

	@Json(name = "time")
	val time: String? = null,

	@Json(name = "storm_hazard")
	val stormHazard: Int? = null
)

@JsonClass(generateAdapter = true)
data class ForecastItem(

	@Json(name = "weather_icon")
	val weatherIcon: String? = null,

	@Json(name = "rain_24h")
	val rain24h: Float? = null,

	@Json(name = "T")
	val t: Float? = null,

	@Json(name = "rain_12h")
	val rain12h: Float? = null,

	@Json(name = "weather_confidence_index")
	val weatherConfidenceIndex: Int? = null,

	@Json(name = "wind_speed")
	val windSpeed: Float? = null,

	@Json(name = "snow_6h")
	val snow6h: Float? = null,

	@Json(name = "snow_12h")
	val snow12h: Float? = null,

	@Json(name = "T_windchill")
	val tWindchill: Float? = null,

	@Json(name = "snow_24h")
	val snow24h: Float? = null,

	@Json(name = "snow_3h")
	val snow3h: Float? = null,

	@Json(name = "weather_description")
	val weatherDescription: String? = null,

	@Json(name = "rain_1h")
	val rain1h: Float? = null,

	@Json(name = "snow_1h")
	val snow1h: Float? = null,

	@Json(name = "rain_3h")
	val rain3h: Float? = null,

	@Json(name = "total_cloud_cover")
	val totalCloudCover: Int? = null,

	@Json(name = "wind_direction")
	val windDirection: Int? = null,

	@Json(name = "wind_speed_gust")
	val windSpeedGust: Float? = null,

	@Json(name = "P_sea")
	val pSea: Float? = null,

	@Json(name = "rain_snow_limit")
	val rainSnowLimit: String? = null,

	@Json(name = "wind_icon")
	val windIcon: String? = null,

	@Json(name = "rain_6h")
	val rain6h: Float? = null,

	@Json(name = "iso0")
	val iso0: Int? = null,

	@Json(name = "time")
	val time: String? = null,

	@Json(name = "relative_humidity")
	val relativeHumidity: Int? = null
)

@JsonClass(generateAdapter = true)
data class ForecastProperties(

	@Json(name = "bulletin_cote")
	val bulletinCote: Int? = null,

	@Json(name = "altitude")
	val altitude: Int? = null,

	@Json(name = "country")
	val country: String? = null,

	@Json(name = "french_department")
	val frenchDepartment: String? = null,

	@Json(name = "insee")
	val insee: String? = null,

	@Json(name = "timezone")
	val timezone: String? = null,

	@Json(name = "probability_forecast")
	val probabilityForecast: List<ProbabilityForecastItem>? = null,

	@Json(name = "name")
	val name: String? = null,

	@Json(name = "forecast")
	val forecast: List<ForecastItem>? = null,

	@Json(name = "daily_forecast")
	val dailyForecast: List<DailyForecastItem>? = null,

	@Json(name = "rain_product_available")
	val rainProductAvailable: Int? = null
)
