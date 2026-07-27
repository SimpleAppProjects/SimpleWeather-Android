package com.thewizrd.weather_api.meteofrance.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CurrentsResponse(

	@Json(name = "update_time")
	val updateTime: String? = null,

	@Json(name = "geometry")
	val geometry: Geometry? = null,

	@Json(name = "type")
	val type: String? = null,

	@Json(name = "properties")
	val properties: CurrentProperties? = null
)

@JsonClass(generateAdapter = true)
data class CurrentProperties(

	@Json(name = "timezone")
	val timezone: String? = null,

	@Json(name = "gridded")
	val gridded: Gridded? = null
)

@JsonClass(generateAdapter = true)
data class Gridded(

	@Json(name = "T")
	val t: Float? = null,

	@Json(name = "weather_description")
	val weatherDescription: String? = null,

	@Json(name = "wind_icon")
	val windIcon: String? = null,

	@Json(name = "wind_speed")
	val windSpeed: Float? = null,

	@Json(name = "wind_direction")
	val windDirection: Int? = null,

	@Json(name = "time")
	val time: String? = null,

	@Json(name = "weather_icon")
	val weatherIcon: String? = null,

	@Json(name = "wind_speed_gust")
	val windSpeedGust: Float? = null
)