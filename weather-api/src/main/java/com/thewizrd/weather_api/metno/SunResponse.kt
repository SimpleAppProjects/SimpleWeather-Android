package com.thewizrd.weather_api.metno

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SunResponse(

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
	val properties: SunProperties? = null
)

@JsonClass(generateAdapter = true)
data class Solarnoon(

	@Json(name = "visible")
	val visible: Boolean? = null,

	@Json(name = "time")
	val time: String? = null,

	@Json(name = "disc_centre_elevation")
	val discCentreElevation: Float? = null
)

@JsonClass(generateAdapter = true)
data class Sunrise(

	@Json(name = "azimuth")
	val azimuth: Float? = null,

	@Json(name = "time")
	val time: String? = null
)

@JsonClass(generateAdapter = true)
data class Sunset(

	@Json(name = "azimuth")
	val azimuth: Float? = null,

	@Json(name = "time")
	val time: String? = null
)

@JsonClass(generateAdapter = true)
data class Solarmidnight(

	@Json(name = "visible")
	val visible: Boolean? = null,

	@Json(name = "time")
	val time: String? = null,

	@Json(name = "disc_centre_elevation")
	val discCentreElevation: Float? = null
)

@JsonClass(generateAdapter = true)
data class SunProperties(

	@Json(name = "solarnoon")
	val solarnoon: Solarnoon? = null,

	@Json(name = "sunrise")
	val sunrise: Sunrise? = null,

	@Json(name = "sunset")
	val sunset: Sunset? = null,

	@Json(name = "body")
	val body: String? = null,

	@Json(name = "solarmidnight")
	val solarmidnight: Solarmidnight? = null
)
