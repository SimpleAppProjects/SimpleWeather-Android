package com.thewizrd.weather_api.meteomatics.weather

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherResponse(

    @field:Json(name = "dateGenerated")
	var dateGenerated: String? = null,

    @field:Json(name = "data")
	var data: List<DataItem?>? = null,

    @field:Json(name = "version")
	var version: String? = null,

    @field:Json(name = "user")
	var user: String? = null,

    @field:Json(name = "status")
	var status: String? = null
)

@JsonClass(generateAdapter = true)
data class DatesItem(

    @field:Json(name = "date")
	var date: String? = null,

    @field:Json(name = "value")
	var value: String? = null
)

@JsonClass(generateAdapter = true)
data class DataItem(

    @field:Json(name = "parameter")
	var parameter: String? = null,

    @field:Json(name = "coordinates")
	var coordinates: List<CoordinatesItem?>? = null
)

@JsonClass(generateAdapter = true)
data class CoordinatesItem(

    @field:Json(name = "lon")
	var lon: Double? = null,

    @field:Json(name = "dates")
	var dates: List<DatesItem?>? = null,

    @field:Json(name = "lat")
	var lat: Double? = null
)
