package com.thewizrd.weather_api.meteofrance.weather

import com.google.gson.annotations.SerializedName
import com.vimeo.stag.UseStag

@UseStag(UseStag.FieldOption.ALL)
data class CurrentsResponse(

    @field:SerializedName("updated_on")
    var updatedOn: Long? = null,

    @field:SerializedName("observation")
    var observation: Observation? = null,

    @field:SerializedName("position")
    var position: CurrentsPosition? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class CurrentsPosition(

    @field:SerializedName("timezone")
    var timezone: String? = null,

    @field:SerializedName("lon")
    var lon: Float? = null,

    @field:SerializedName("lat")
    var lat: Float? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class Observation(

    @field:SerializedName("T")
    var T: Float? = null,

    @field:SerializedName("weather")
    var weather: Weather? = null,

    @field:SerializedName("wind")
    var wind: Wind? = null
)
/*
@UseStag(UseStag.FieldOption.ALL)
data class Weather(

	@field:SerializedName("icon")
	var icon: String? = null,

	@field:SerializedName("desc")
	var desc: String? = null
)
@UseStag(UseStag.FieldOption.ALL)
data class Wind(

	@field:SerializedName("icon")
	var icon: String? = null,

	@field:SerializedName("speed")
	var speed: Int? = null,

	@field:SerializedName("direction")
	var direction: Int? = null
)
*/