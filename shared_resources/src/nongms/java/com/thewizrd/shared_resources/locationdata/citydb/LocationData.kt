package com.thewizrd.shared_resources.locationdata.citydb

import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.database.City
import com.thewizrd.shared_resources.weatherdata.WeatherAPI

/* OpenWeatherMap Location */
fun createLocationModel(result: City, weatherAPI: String): LocationQueryViewModel {
    return LocationQueryViewModel().apply {
        locationName = if (!result.state.isNullOrBlank())
            "${result.name}, ${result.state}"
        else if (!result.country.isNullOrBlank())
            "${result.name}, ${result.country}"
        else
            result.name

        locationCountry = result.country

        locationLat = result.lat
        locationLong = result.lon

        locationTZLong = null

        locationSource = WeatherAPI.OPENWEATHERMAP

        updateWeatherSource(weatherAPI)
    }
}