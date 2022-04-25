package com.thewizrd.weather_api.openweather.citydb

import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.weather_api.database.City

/* OpenWeatherMap Location */
fun createLocationModel(result: City, weatherAPI: String): LocationQuery {
    return LocationQuery().apply {
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