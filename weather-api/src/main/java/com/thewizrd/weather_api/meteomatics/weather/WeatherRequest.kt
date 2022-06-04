package com.thewizrd.weather_api.meteomatics.weather

import com.google.gson.annotations.SerializedName

data class WeatherRequest(
	@field:SerializedName("validdates")
	val validdates: String,

	@field:SerializedName("format")
	val format: String,

	@field:SerializedName("location")
	val location: String,

	@field:SerializedName("parameters")
	val parameters: List<String>
)
