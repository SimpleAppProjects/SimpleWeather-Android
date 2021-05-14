package com.thewizrd.shared_resources.locationdata.weatherapi

import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.utils.LocationUtils
import com.thewizrd.shared_resources.weatherdata.WeatherAPI

/* WeatherAPI Search/AutoComplete Query */
fun createLocationModel(result: LocationItem, weatherAPI: String): LocationQueryViewModel {
    return LocationQueryViewModel().apply {
        val isUSorCA = LocationUtils.isUSorCanada(result.country)

        var name = result.name
        if (isUSorCA) {
            name = name.replaceFirst(String.format(", %s", result.country).toRegex(), "")
        } else {
            name = name.replaceFirst(String.format(", %s, %s", result.region, result.country).toRegex(), String.format(", %s", result.country))
            locationRegion = result.region
        }

        locationName = name
        locationCountry = result.country
        locationQuery = result.id.toString()

        locationLat = result.lat
        locationLong = result.lon

        locationTZLong = null

        locationSource = WeatherAPI.WEATHERAPI

        updateWeatherSource(weatherAPI)
    }
}