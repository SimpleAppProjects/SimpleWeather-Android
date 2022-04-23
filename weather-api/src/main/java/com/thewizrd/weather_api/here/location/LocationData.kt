package com.thewizrd.weather_api.here.location

import androidx.core.util.ObjectsCompat
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.weatherdata.WeatherAPI

/* HERE Autocomplete */
fun createLocationModel(location: SuggestionsItem, weatherAPI: String): LocationQuery {
    return LocationQuery().apply {
        var town: String?
        var region: String?
        val country: String

        // Try to get district name or fallback to city name
        town = location.address.district

        if (town.isNullOrBlank())
            town = location.address.city

        region = location.address.state

        if (region.isNullOrBlank() && town != location.address.county)
            region = location.address.county

        country = location.address.country

        if (!town.isNullOrBlank() && !region.isNullOrBlank() &&
                !location.address.county.isNullOrEmpty() &&
                !ObjectsCompat.equals(location.address.county, region) &&
                !ObjectsCompat.equals(location.address.county, town)) {
            locationName = String.format("%s, %s, %s", town, location.address.county, region)
        } else if (!town.isNullOrBlank() && !region.isNullOrBlank()) {
            locationName = if (ObjectsCompat.equals(town, region)) {
                String.format("%s, %s", town, country)
            } else {
                String.format("%s, %s", town, region)
            }
        } else {
            if (town.isNullOrBlank() || region.isNullOrBlank()) {
                locationName = if (town.isNullOrBlank()) {
                    String.format("%s, %s", region, country)
                } else {
                    String.format("%s, %s", town, country)
                }
            }
        }

        locationCountry = location.countryCode
        locationQuery = location.locationId

        locationLat = -1.0
        locationLong = -1.0

        locationTZLong = null

        locationSource = WeatherAPI.HERE
        weatherSource = weatherAPI
    }
}

/* HERE Geocoder */
fun createLocationModel(location: ResultItem, weatherAPI: String): LocationQuery {
    return LocationQuery().apply {
        var country: String? = null
        var countryCode: String? = null
        var region: String? = null
        var town: String? = null

        if (location.location.address.additionalData != null) {
            for (item in location.location.address.additionalData) {
                if ("Country2" == item.key)
                    countryCode = item.value
                else if ("CountryName" == item.key)
                    country = item.value

                if (countryCode != null && country != null) break
            }
        }

        // Try to get district name or fallback to city name
        town = location.location.address.district

        if (town.isNullOrBlank())
            town = location.location.address.city

        region = location.location.address.state

        if (region.isNullOrBlank() && !ObjectsCompat.equals(town, location.location.address.county))
            region = location.location.address.county

        if (country.isNullOrBlank())
            country = location.location.address.country

        if (countryCode.isNullOrBlank())
            countryCode = location.location.address.country

        if (!town.isNullOrBlank() && !region.isNullOrBlank() &&
                !location.location.address.county.isNullOrBlank() &&
                !ObjectsCompat.equals(location.location.address.county, region) &&
                !ObjectsCompat.equals(location.location.address.county, town)) {
            locationName = String.format("%s, %s, %s", town, location.location.address.county, region)
        } else if (!town.isNullOrBlank() && !region.isNullOrBlank()) {
            locationName = if (ObjectsCompat.equals(town, region)) {
                String.format("%s, %s", town, country)
            } else {
                String.format("%s, %s", town, region)
            }
        } else {
            if (town.isNullOrBlank() || region.isNullOrBlank()) {
                locationName = if (!location.location.address.label.isNullOrBlank()) {
                    location.location.address.label.let { label ->
                        if (label != null && label.contains(", $country")) {
                            label.replace(", $country", "")
                        } else {
                            label
                        }
                    }
                } else {
                    if (town.isNullOrBlank()) {
                        String.format("%s, %s", region, country)
                    } else {
                        String.format("%s, %s", town, country)
                    }
                }
            }
        }

        locationCountry = countryCode

        locationLat = location.location.displayPosition.latitude
        locationLong = location.location.displayPosition.longitude

        locationTZLong = location.location.adminInfo.timeZone.id

        locationSource = WeatherAPI.HERE

        updateWeatherSource(weatherAPI!!)
    }
}