package com.thewizrd.weather_api.weatherapi.location

import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.utils.LocationUtils
import com.thewizrd.shared_resources.weatherdata.WeatherAPI

/* WeatherAPI Search/AutoComplete Query */
fun createLocationModel(result: LocationItem, weatherAPI: String): LocationQuery {
    return LocationQuery().apply {
        val isUSorCA = LocationUtils.isUSorCanada(result.country)

        var name = result.name
        if (isUSorCA) {
            name = "$name, ${result.region}"
        } else {
            name = "$name, ${result.country}"
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