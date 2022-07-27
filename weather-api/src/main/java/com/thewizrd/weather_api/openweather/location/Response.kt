package com.thewizrd.weather_api.openweather.location

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ResponseItem(

    @field:Json(name = "local_names")
	var localNames: LocalNames? = null,

    @field:Json(name = "country")
	var country: String? = null,

    @field:Json(name = "name")
	var name: String? = null,

    @field:Json(name = "lon")
	var lon: Double? = null,

    @field:Json(name = "state")
	var state: String? = null,

    @field:Json(name = "lat")
	var lat: Double? = null
)

@JsonClass(generateAdapter = true)
data class LocalNames(

    @field:Json(name = "feature_name")
	var featureName: String? = null,

    @field:Json(name = "en")
	var en: String? = null,

    @field:Json(name = "ascii")
	var ascii: String? = null,

    @field:Json(name = "ca")
	var ca: String? = null,

    @field:Json(name = "ar")
	var ar: String? = null,

    @field:Json(name = "fa")
	var fa: String? = null,

    @field:Json(name = "sr")
	var sr: String? = null,

    @field:Json(name = "de")
	var de: String? = null,

    @field:Json(name = "fi")
	var fi: String? = null,

    @field:Json(name = "ru")
	var ru: String? = null,

    @field:Json(name = "pt")
	var pt: String? = null,

    @field:Json(name = "bg")
	var bg: String? = null,

    @field:Json(name = "lt")
	var lt: String? = null,

    @field:Json(name = "fr")
	var fr: String? = null,

    @field:Json(name = "ja")
	var ja: String? = null,

    @field:Json(name = "pl")
	var pl: String? = null,

    @field:Json(name = "he")
	var he: String? = null,

    @field:Json(name = "nl")
	var nl: String? = null,

    @field:Json(name = "hi")
	var hi: String? = null,

    @field:Json(name = "no")
	var no: String? = null,

    @field:Json(name = "hr")
	var hr: String? = null,

    @field:Json(name = "hu")
	var hu: String? = null,

    @field:Json(name = "sk")
	var sk: String? = null,

    @field:Json(name = "sl")
	var sl: String? = null,

    @field:Json(name = "id")
	var id: String? = null,

    @field:Json(name = "mk")
	var mk: String? = null,

    @field:Json(name = "af")
	var af: String? = null,

    @field:Json(name = "gl")
	var gl: String? = null,

    @field:Json(name = "el")
	var el: String? = null,

    @field:Json(name = "it")
	var it: String? = null,

    @field:Json(name = "eu")
	var eu: String? = null,

    @field:Json(name = "vi")
	var vi: String? = null,

    @field:Json(name = "th")
	var th: String? = null,

    @field:Json(name = "la")
	var la: String? = null,

    @field:Json(name = "az")
	var az: String? = null,

    @field:Json(name = "zu")
	var zu: String? = null,

    @field:Json(name = "da")
	var da: String? = null,

    @field:Json(name = "ro")
	var ro: String? = null,

    @field:Json(name = "tr")
	var tr: String? = null
)
