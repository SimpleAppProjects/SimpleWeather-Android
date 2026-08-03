package com.thewizrd.weather_api.google.location

import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.maps.model.AddressComponentType
import com.google.maps.model.GeocodingResult
import com.google.maps.model.LocationType
import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.google.android.libraries.places.api.model.AddressComponent as PlacesAddressComponent
import com.google.maps.model.AddressComponent as GeocodingAddressComponent

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
        var town: PlacesAddressComponent? = null
        var region: PlacesAddressComponent? = null
        var adminArea: PlacesAddressComponent? = null
        var country: PlacesAddressComponent? = null

        val addressComponents: List<PlacesAddressComponent?>? =
            response.place.addressComponents?.asList()

        if (!addressComponents.isNullOrEmpty()) {
            for (addrCmp in addressComponents) {
                if (town == null && addrCmp?.types?.contains("neighborhood") == true) {
                    town = addrCmp
                }
                if (town == null && addrCmp?.types?.contains("locality") == true) {
                    town = addrCmp
                }
                if (adminArea == null && addrCmp?.types?.contains("administrative_area_level_2") == true) {
                    adminArea = addrCmp
                }
                if (region == null && addrCmp?.types?.contains("administrative_area_level_1") == true) {
                    region = addrCmp
                }
                if (country == null && addrCmp?.types?.contains("country") == true) {
                    country = addrCmp
                    locationCountry = addrCmp.shortName
                }
                if (town != null && adminArea != null && region != null && country != null) {
                    break
                }
            }
        }

        val isUS = country?.shortName == "US" || country?.name == "United States"

        if (town != null && region != null && adminArea != null && !(adminArea.name == region.name || adminArea.name.contains(
                region.name,
                true
            ) || adminArea.name == town.name || adminArea.name.contains(town.name, true))
        ) {
            locationName = String.format(
                "%s, %s, %s",
                town.name,
                adminArea.name,
                if (isUS) region.shortName else region.name
            )
        } else if (town != null && region != null) {
            locationName = if (town.name == region.name) {
                String.format("%s, %s", town.name, country?.name)
            } else {
                String.format("%s, %s", town.name, if (isUS) region.shortName else region.name)
            }
        } else {
            if (town == null || region == null) {
                if (!response.place.displayName.isNullOrBlank()) {
                    val placeName = response.place.displayName

                    locationName = when {
                        placeName?.contains(", ${country?.name}") == true -> {
                            placeName.replace(", ${country?.name}", "")
                        }
                        placeName?.contains(", $locationCountry") == true -> {
                            placeName.replace(", $locationCountry", "")
                        }
                        else -> {
                            placeName
                        }
                    }
                } else {
                    locationName = if (town == null) {
                        String.format("%s, %s", region?.name, country?.name)
                    } else {
                        String.format("%s, %s", town.name, country?.name)
                    }
                }
            }
        }

        if (locationName.isNullOrBlank()) {
            locationName = response.place.displayName
        }
        if (locationCountry.isNullOrBlank()) {
            locationCountry = country?.shortName ?: country?.name
        }
        if (locationRegion.isNullOrBlank()) {
            locationRegion = region?.name ?: adminArea?.name
        }

        locationLat = response.place.location!!.latitude
        locationLong = response.place.location!!.longitude

        locationTZLong = null

        locationSource = WeatherAPI.GOOGLE
        updateWeatherSource(weatherAPI)
    }
}

/* Google Geocoding API Result */
fun createLocationModel(
    response: GeocodingResult,
    @WeatherAPI.WeatherProviders weatherAPI: String?
): LocationQuery {
    return LocationQuery().apply {
        var town: GeocodingAddressComponent? = null
        var region: GeocodingAddressComponent? = null
        var adminArea: GeocodingAddressComponent? = null
        var country: GeocodingAddressComponent? = null

        val addressComponents: List<GeocodingAddressComponent?>? =
            response.addressComponents?.asList()

        if (!addressComponents.isNullOrEmpty()) {
            for (addrCmp in addressComponents) {
                if (town == null && addrCmp?.types?.contains(AddressComponentType.NEIGHBORHOOD) == true) {
                    town = addrCmp
                }
                if (town == null && addrCmp?.types?.contains(AddressComponentType.LOCALITY) == true) {
                    town = addrCmp
                }
                if (adminArea == null && addrCmp?.types?.contains(AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_2) == true) {
                    adminArea = addrCmp
                }
                if (region == null && addrCmp?.types?.contains(AddressComponentType.ADMINISTRATIVE_AREA_LEVEL_1) == true) {
                    region = addrCmp
                }
                if (country == null && addrCmp?.types?.contains(AddressComponentType.COUNTRY) == true) {
                    country = addrCmp
                    locationCountry = addrCmp.shortName
                }
                if (town != null && adminArea != null && region != null && country != null) {
                    break
                }
            }
        }

        val isUS = country?.shortName == "US" || country?.longName == "United States"

        if (town != null && region != null && adminArea != null && !(adminArea.longName == region.longName || adminArea.longName.contains(
                region.longName,
                true
            ) || adminArea.longName == town.longName || adminArea.longName.contains(
                town.longName,
                true
            ))
        ) {
            locationName = String.format(
                "%s, %s, %s",
                town.longName,
                adminArea.longName,
                if (isUS) region.shortName else region.longName
            )
        } else if (town != null && region != null) {
            locationName = if (town.longName == region.longName) {
                String.format("%s, %s", town.longName, country?.longName)
            } else {
                String.format(
                    "%s, %s",
                    town.longName,
                    if (isUS) region.shortName else region.longName
                )
            }
        } else {
            if (town == null || region == null) {
                locationName = if (town == null) {
                    String.format("%s, %s", region?.longName, country?.longName)
                } else {
                    String.format("%s, %s", town.longName, country?.longName)
                }
            }
        }

        if (locationName.isNullOrBlank()) {
            locationName = response.formattedAddress.let { addr ->
                var updatedAddr = addr

                if (response.geometry.locationType != LocationType.APPROXIMATE) {
                    // Trim un-needed address components
                    val streetNo = response.addressComponents.firstOrNull { adrcmp ->
                        adrcmp.types.contains(AddressComponentType.STREET_NUMBER)
                    }
                    val route = response.addressComponents.firstOrNull { adrcmp ->
                        adrcmp.types.contains(AddressComponentType.ROUTE)
                    }
                    val postalCode = response.addressComponents.firstOrNull { adrcmp ->
                        adrcmp.types.contains(AddressComponentType.POSTAL_CODE)
                    }

                    if (streetNo != null && route != null) {
                        val streetAddress = "${streetNo.longName} ${route.shortName}"
                        updatedAddr =
                            updatedAddr.replaceFirst(streetAddress, "").trimStart().trimStart(',')
                    }
                    if (postalCode != null) {
                        updatedAddr =
                            updatedAddr.replaceFirst(" ${postalCode.longName}", "").trimStart()
                                .trimStart(',')
                    }
                }

                updatedAddr.trim()
            }
        }
        if (locationCountry.isNullOrBlank()) {
            locationCountry = country?.shortName ?: country?.longName
        }
        if (locationRegion.isNullOrBlank()) {
            locationRegion = region?.longName ?: adminArea?.longName
        }

        locationLat = response.geometry.location.lat
        locationLong = response.geometry.location.lng

        locationTZLong = null

        locationSource = WeatherAPI.GOOGLE
        updateWeatherSource(weatherAPI)
    }
}