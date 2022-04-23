package com.thewizrd.weather_api.google.location

import android.location.Address
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.weatherdata.WeatherAPI

/* Android Geocoder */
fun createLocationModel(result: Address, weatherAPI: String?): LocationQuery {
    return LocationQuery().apply {
        if (!result.hasLatitude() || !result.hasLongitude()) return@apply

        // SubLocality - Neighborhood, Suburb, Subdivision of locality
        // Locality - Village, Town, City
        // SubAdminArea - County, District
        // AdminArea - State, Province
        // CountryCode
        // CountryName

        val town = if (!result.subLocality.isNullOrBlank()) {
            result.subLocality
        } else if (!result.locality.isNullOrBlank()) {
            result.locality
        } else {
            result.subAdminArea
        }

        val region = if (!result.adminArea.isNullOrBlank()) {
            result.adminArea
        } else {
            result.countryName
        }

        locationName = if (region != null && town != region) {
            if (town != null) {
                "$town, $region"
            } else {
                if (region != result.countryName) {
                    "$region, ${result.countryName}"
                } else {
                    region
                }
            }
        } else {
            if (town != null) {
                "$town, ${result.countryName}"
            } else {
                result.countryName
            }
        }

        locationRegion = result.subAdminArea

        locationLat = result.latitude
        locationLong = result.longitude

        locationCountry = result.countryCode

        locationTZLong = null

        locationSource = WeatherAPI.GOOGLE

        updateWeatherSource(weatherAPI)
    }
}