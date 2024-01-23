package com.thewizrd.weather_api.metno

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MoonResponse(

	@Json(name = "licenseURL")
	val licenseURL: String? = null,

	@Json(name = "copyright")
	val copyright: String? = null,

	@Json(name = "geometry")
	val geometry: Geometry? = null,

	@Json(name = "type")
	val type: String? = null,

	@Json(name = "when")
	val _when: When? = null,

	@Json(name = "properties")
	val properties: MoonProperties? = null
)

@JsonClass(generateAdapter = true)
data class Moonset(

	@Json(name = "azimuth")
	val azimuth: Float? = null,

	@Json(name = "time")
	val time: String? = null
)

@JsonClass(generateAdapter = true)
data class MoonProperties(

	@Json(name = "moonset")
	val moonset: Moonset? = null,

	@Json(name = "moonphase")
	val moonphase: Float? = null,

	@Json(name = "moonrise")
	val moonrise: Moonrise? = null,

	@Json(name = "body")
	val body: String? = null,

	@Json(name = "low_moon")
	val lowMoon: LowMoon? = null,

	@Json(name = "high_moon")
	val highMoon: HighMoon? = null
)

@JsonClass(generateAdapter = true)
data class Moonrise(

	@Json(name = "azimuth")
	val azimuth: Float? = null,

	@Json(name = "time")
	val time: String? = null
)

@JsonClass(generateAdapter = true)
data class LowMoon(

	@Json(name = "visible")
	val visible: Boolean? = null,

	@Json(name = "time")
	val time: String? = null,

	@Json(name = "disc_centre_elevation")
	val discCentreElevation: Float? = null
)

@JsonClass(generateAdapter = true)
data class HighMoon(

	@Json(name = "visible")
	val visible: Boolean? = null,

	@Json(name = "time")
	val time: String? = null,

	@Json(name = "disc_centre_elevation")
	val discCentreElevation: Float? = null
)
