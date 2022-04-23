package com.thewizrd.weather_api.openweather.location

import com.google.gson.annotations.SerializedName
import com.vimeo.stag.UseStag

@UseStag(UseStag.FieldOption.ALL)
data class ResponseItem(

    @field:SerializedName("local_names")
	var localNames: LocalNames? = null,

    @field:SerializedName("country")
	var country: String? = null,

    @field:SerializedName("name")
	var name: String? = null,

    @field:SerializedName("lon")
	var lon: Double? = null,

    @field:SerializedName("state")
	var state: String? = null,

    @field:SerializedName("lat")
	var lat: Double? = null
)

@UseStag(UseStag.FieldOption.ALL)
data class LocalNames(

	@field:SerializedName("feature_name")
	var featureName: String? = null,

	@field:SerializedName("en")
	var en: String? = null,

	@field:SerializedName("ascii")
	var ascii: String? = null,

	@field:SerializedName("ca")
	var ca: String? = null,

	@field:SerializedName("ar")
	var ar: String? = null,

	@field:SerializedName("fa")
	var fa: String? = null,

	@field:SerializedName("sr")
	var sr: String? = null,

	@field:SerializedName("de")
	var de: String? = null,

	@field:SerializedName("fi")
	var fi: String? = null,

	@field:SerializedName("ru")
	var ru: String? = null,

	@field:SerializedName("pt")
	var pt: String? = null,

	@field:SerializedName("bg")
	var bg: String? = null,

	@field:SerializedName("lt")
	var lt: String? = null,

	@field:SerializedName("fr")
	var fr: String? = null,

	@field:SerializedName("ja")
	var ja: String? = null,

	@field:SerializedName("pl")
	var pl: String? = null,

	@field:SerializedName("he")
	var he: String? = null,

	@field:SerializedName("nl")
	var nl: String? = null,

	@field:SerializedName("hi")
	var hi: String? = null,

	@field:SerializedName("no")
	var no: String? = null,

	@field:SerializedName("hr")
	var hr: String? = null,

	@field:SerializedName("hu")
	var hu: String? = null,

	@field:SerializedName("sk")
	var sk: String? = null,

	@field:SerializedName("sl")
	var sl: String? = null,

	@field:SerializedName("id")
	var id: String? = null,

	@field:SerializedName("mk")
	var mk: String? = null,

	@field:SerializedName("af")
	var af: String? = null,

	@field:SerializedName("gl")
	var gl: String? = null,

	@field:SerializedName("el")
	var el: String? = null,

	@field:SerializedName("it")
	var it: String? = null,

	@field:SerializedName("eu")
	var eu: String? = null,

	@field:SerializedName("vi")
	var vi: String? = null,

	@field:SerializedName("th")
	var th: String? = null,

	@field:SerializedName("la")
	var la: String? = null,

	@field:SerializedName("az")
	var az: String? = null,

	@field:SerializedName("zu")
	var zu: String? = null,

	@field:SerializedName("da")
	var da: String? = null,

	@field:SerializedName("ro")
	var ro: String? = null,

	@field:SerializedName("tr")
	var tr: String? = null
)
