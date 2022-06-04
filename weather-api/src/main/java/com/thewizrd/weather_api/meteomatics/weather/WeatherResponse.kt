package com.thewizrd.weather_api.meteomatics.weather

import com.google.gson.annotations.SerializedName
import com.vimeo.stag.UseStag

@UseStag(UseStag.FieldOption.ALL)
data class WeatherResponse(

	@field:SerializedName("dateGenerated")
	var dateGenerated: String? = null,

	@field:SerializedName("data")
	var data: List<DataItem?>? = null,

	@field:SerializedName("version")
	var version: String? = null,

	@field:SerializedName("user")
	var user: String? = null,

	@field:SerializedName("status")
	var status: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class DatesItem(

	@field:SerializedName("date")
	var date: String? = null,

	@field:SerializedName("value")
	var value: String? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class DataItem(

	@field:SerializedName("parameter")
	var parameter: String? = null,

	@field:SerializedName("coordinates")
	var coordinates: List<CoordinatesItem?>? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class CoordinatesItem(

	@field:SerializedName("lon")
	var lon: Double? = null,

	@field:SerializedName("dates")
	var dates: List<DatesItem?>? = null,

	@field:SerializedName("lat")
	var lat: Double? = null
)
