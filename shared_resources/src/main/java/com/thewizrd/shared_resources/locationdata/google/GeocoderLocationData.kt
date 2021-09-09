package com.thewizrd.shared_resources.locationdata.google

import android.location.Address
import androidx.core.util.ObjectsCompat
import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.weatherdata.WeatherAPI

/* Android Geocoder */
fun createLocationModel(result: Address, weatherAPI: String): LocationQueryViewModel {
    return LocationQueryViewModel().apply {
        if (!result.hasLatitude() || !result.hasLongitude()) return@apply

        val town = if (!result.locality.isNullOrBlank()) {
            result.locality
        } else  /* if (result.subLocality.isNullOrBlank())*/ {
            result.subLocality
        }
        val region = result.adminArea

        locationName = if (region != null && !ObjectsCompat.equals(town, region)) {
            if (town != null) {
                "$town, $region"
            } else {
                region
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