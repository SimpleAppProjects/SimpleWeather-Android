package com.thewizrd.shared_resources.locationdata.openweather

import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.weatherdata.WeatherAPI

/* OpenWeatherMap Location */
fun createLocationModel(
    result: ResponseItem,
    iso: String,
    weatherAPI: String
): LocationQueryViewModel {
    return LocationQueryViewModel().apply {
        val locationName = when (iso) {
            "af" -> result.localNames?.af
            "ar" -> result.localNames?.ar
            "az" -> result.localNames?.az
            "bg" -> result.localNames?.bg
            "ca" -> result.localNames?.ca
            "da" -> result.localNames?.da
            "de" -> result.localNames?.de
            "el" -> result.localNames?.el
            "en" -> result.localNames?.en
            "eu" -> result.localNames?.eu
            "fa" -> result.localNames?.fa
            "fi" -> result.localNames?.fi
            "fr" -> result.localNames?.fr
            "gl" -> result.localNames?.gl
            "he" -> result.localNames?.he
            "hi" -> result.localNames?.hi
            "hr" -> result.localNames?.hr
            "hu" -> result.localNames?.hu
            "id" -> result.localNames?.id
            "it" -> result.localNames?.it
            "ja" -> result.localNames?.ja
            "la" -> result.localNames?.la
            "lt" -> result.localNames?.lt
            "mk" -> result.localNames?.mk
            "nl" -> result.localNames?.nl
            "no" -> result.localNames?.no
            "pl" -> result.localNames?.pl
            "pt" -> result.localNames?.pt
            "ro" -> result.localNames?.ro
            "ru" -> result.localNames?.ru
            "sk" -> result.localNames?.sk
            "sl" -> result.localNames?.sl
            "sr" -> result.localNames?.sr
            "th" -> result.localNames?.th
            "tr" -> result.localNames?.tr
            "vi" -> result.localNames?.vi
            "zu" -> result.localNames?.zu
            else -> result.name
        } ?: result.name

        this.locationName = if (!result.state.isNullOrBlank())
            "$locationName, ${result.state}"
        else if (!result.country.isNullOrBlank())
            "$locationName, ${result.country}"
        else
            locationName

        locationCountry = result.country

        locationLat = result.lat!!
        locationLong = result.lon!!

        locationTZLong = null

        locationSource = WeatherAPI.OPENWEATHERMAP

        updateWeatherSource(weatherAPI)
    }
}