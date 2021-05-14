package com.thewizrd.shared_resources.locationdata.locationiq

import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import java.util.*

/* LocationIQ AutoComplete */
fun createLocationModel(result: AutoCompleteQuery, weatherAPI: String): LocationQueryViewModel {
    return LocationQueryViewModel().apply {
        val town: String
        val region: String

        // Try to get district name or fallback to city name
        town = if (!result.address.neighbourhood.isNullOrBlank())
            result.address.neighbourhood
        else if (!result.address.hamlet.isNullOrBlank())
            result.address.hamlet
        else if (!result.address.suburb.isNullOrBlank())
            result.address.suburb
        else if (!result.address.village.isNullOrBlank())
            result.address.village
        else if (!result.address.town.isNullOrBlank())
            result.address.town
        else if (!result.address.city.isNullOrBlank())
            result.address.city
        else
            result.address.name

        // Try to get district name or fallback to city name
        region = if (!result.address.region.isNullOrBlank())
            result.address.region
        else if (!result.address.county.isNullOrBlank())
            result.address.county
        else if (!result.address.stateDistrict.isNullOrBlank())
            result.address.stateDistrict
        else if (!result.address.state.isNullOrBlank())
            result.address.state
        else
            result.address.country

        locationName = if (!result.address.name.isNullOrBlank() && result.address.name != town)
            String.format("%s, %s, %s", result.address.name, town, region)
        else
            String.format("%s, %s", town, region)

        locationCountry = if (!result.address.countryCode.isNullOrBlank())
            result.address.countryCode.toUpperCase(Locale.ROOT)
        else
            result.address.country

        locationLat = result.lat.toDouble()
        locationLong = result.lon.toDouble()

        locationTZLong = null

        locationSource = WeatherAPI.LOCATIONIQ

        updateWeatherSource(weatherAPI)
    }
}

/* LocationIQ Geocoder */
fun createLocationModel(result: GeoLocation, weatherAPI: String): LocationQueryViewModel {
    return LocationQueryViewModel().apply {
        val town: String
        val region: String

        // Try to get district name or fallback to city name
        town = if (!result.address.neighbourhood.isNullOrBlank())
            result.address.neighbourhood
        else if (!result.address.hamlet.isNullOrBlank())
            result.address.hamlet
        else if (!result.address.suburb.isNullOrBlank())
            result.address.suburb
        else if (!result.address.village.isNullOrBlank())
            result.address.village
        else if (!result.address.town.isNullOrBlank())
            result.address.town
        else if (!result.address.city.isNullOrBlank())
            result.address.city
        else
            result.address.name

        // Try to get district name or fallback to city name
        region = if (!result.address.region.isNullOrBlank())
            result.address.region
        else if (!result.address.county.isNullOrBlank())
            result.address.county
        else if (!result.address.stateDistrict.isNullOrBlank())
            result.address.stateDistrict
        else if (!result.address.state.isNullOrBlank())
            result.address.state
        else
            result.address.country

        locationName = if (!result.address.name.isNullOrBlank() && result.address.name != town)
            String.format("%s, %s, %s", result.address.name, town, region)
        else
            String.format("%s, %s", town, region)

        locationCountry = if (!result.address.countryCode.isNullOrBlank())
            result.address.countryCode.toUpperCase(Locale.ROOT)
        else
            result.address.country

        locationLat = result.lat.toDouble()
        locationLong = result.lon.toDouble()

        locationTZLong = null

        locationSource = WeatherAPI.LOCATIONIQ

        updateWeatherSource(weatherAPI)
    }
}