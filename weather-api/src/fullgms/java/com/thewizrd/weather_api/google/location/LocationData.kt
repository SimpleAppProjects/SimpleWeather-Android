package com.thewizrd.weather_api.google.location

import com.google.android.libraries.places.api.model.AddressComponent
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.weatherdata.WeatherAPI

/* Google Maps Places Autocomplete */
fun createLocationModel(
    result: AutocompletePrediction,
    @WeatherAPI.WeatherProviders weatherAPI: String?
): LocationQuery {
    return LocationQuery().apply {
        locationName = result.getPrimaryText(null).toString()
        locationCountry = result.getSecondaryText(null).toString()
        locationQuery = result.placeId

        locationLat = -1.0
        locationLong = -1.0

        locationTZLong = null

        locationSource = WeatherAPI.GOOGLE
        weatherSource = weatherAPI
    }
}

/* Google Places API Place */
fun createLocationModel(
    response: FetchPlaceResponse,
    @WeatherAPI.WeatherProviders weatherAPI: String?
): LocationQuery {
    return LocationQuery().apply {
        var town: String? = null
        var region: String? = null
        var adminArea: String? = null
        var countryName: String? = null

        val addressComponents: List<AddressComponent?>? = response.place.addressComponents?.asList()

        if (!addressComponents.isNullOrEmpty()) {
            for (addrCmp in addressComponents) {
                if (town.isNullOrBlank() && addrCmp?.types?.contains("locality") == true) {
                    town = addrCmp.name
                }
                if (adminArea.isNullOrBlank() && addrCmp?.types?.contains("administrative_area_level_2") == true) {
                    adminArea = addrCmp.shortName
                }
                if (region.isNullOrBlank() && addrCmp?.types?.contains("administrative_area_level_1") == true) {
                    region = addrCmp.shortName
                }
                if (locationCountry.isNullOrBlank() && addrCmp?.types?.contains("country") == true) {
                    countryName = addrCmp.name
                    locationCountry = addrCmp.shortName
                }
                if (town != null && adminArea != null && region != null && locationCountry != null) {
                    break
                }
            }
        }

        if (!town.isNullOrBlank() && !region.isNullOrBlank() && !adminArea.isNullOrBlank() &&
                !(adminArea == region || adminArea == town)) {
            locationName = String.format("%s, %s, %s", town, adminArea, region)
        } else if (!town.isNullOrBlank() && !region.isNullOrBlank()) {
            locationName = if (town == region) {
                String.format("%s, %s", town, countryName)
            } else {
                String.format("%s, %s", town, region)
            }
        } else {
            if (town.isNullOrBlank() || region.isNullOrBlank()) {
                if (!response.place.name.isNullOrBlank()) {
                    val placeName = response.place.name
                    locationName = when {
                        placeName?.contains(", $countryName") == true -> {
                            placeName.replace(", $countryName", "")
                        }
                        placeName?.contains(", $locationCountry") == true -> {
                            placeName.replace(", $locationCountry", "")
                        }
                        else -> {
                            placeName
                        }
                    }
                } else {
                    locationName = if (town.isNullOrBlank()) {
                        String.format("%s, %s", region, countryName)
                    } else {
                        String.format("%s, %s", town, countryName)
                    }
                }
            }
        }

        if (locationName.isNullOrBlank()) {
            locationName = response.place.name
        }
        if (locationCountry.isNullOrBlank()) {
            locationCountry = countryName
        }

        locationLat = response.place.latLng!!.latitude
        locationLong = response.place.latLng!!.longitude

        locationTZLong = null

        locationSource = WeatherAPI.GOOGLE
        updateWeatherSource(weatherAPI)
    }
}