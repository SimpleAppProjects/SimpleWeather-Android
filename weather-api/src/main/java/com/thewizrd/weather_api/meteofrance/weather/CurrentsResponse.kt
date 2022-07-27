package com.thewizrd.weather_api.meteofrance.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CurrentsResponse(

    @field:Json(name = "updated_on")
    var updatedOn: Long? = null,

    @field:Json(name = "observation")
    var observation: Observation? = null,

    @field:Json(name = "position")
    var position: CurrentsPosition? = null
)

@JsonClass(generateAdapter = true)
data class CurrentsPosition(

    @field:Json(name = "timezone")
    var timezone: String? = null,

    @field:Json(name = "lon")
    var lon: Float? = null,

    @field:Json(name = "lat")
    var lat: Float? = null
)

@JsonClass(generateAdapter = true)
data class Observation(

    @field:Json(name = "T")
    var T: Float? = null,

    @field:Json(name = "weather")
    var weather: Weather? = null,

    @field:Json(name = "wind")
    var wind: Wind? = null
)
/*
@JsonClass(generateAdapter = true)
data class Weather(

	@field:Json(name = "icon")
	var icon: String? = null,

	@field:Json(name = "desc")
	var desc: String? = null
)
@JsonClass(generateAdapter = true)
data class Wind(

	@field:Json(name = "icon")
	var icon: String? = null,

	@field:Json(name = "speed")
	var speed: Int? = null,

	@field:Json(name = "direction")
	var direction: Int? = null
)
*/