package com.thewizrd.shared_resources.locationdata.accuweather

import com.thewizrd.shared_resources.controls.LocationQueryViewModel
import com.thewizrd.shared_resources.weatherdata.WeatherAPI

fun createLocationModel(result: GeopositionResponse): LocationQueryViewModel {
    return LocationQueryViewModel().apply {
        locationName = "${result.localizedName}, ${result.administrativeArea!!.localizedName}"
        locationCountry = result.country!!.iD

        locationQuery = result.key

        locationLat = result.geoPosition!!.latitude!!
        locationLong = result.geoPosition!!.longitude!!

        locationTZLong = result.timeZone!!.name

        locationSource = WeatherAPI.ACCUWEATHER
        weatherSource = WeatherAPI.ACCUWEATHER
    }
}

fun createLocationModel(result: GeopositionResponse, oldModel: LocationQueryViewModel):
        LocationQueryViewModel {
    val newModel = LocationQueryViewModel.clone(oldModel)

    newModel.locationQuery = result.key
    newModel.locationTZLong = oldModel.locationTZLong ?: result.timeZone?.name
    newModel.locationCountry = oldModel.locationCountry ?: result.country?.iD
    newModel.locationSource = WeatherAPI.ACCUWEATHER
    newModel.weatherSource = WeatherAPI.ACCUWEATHER

    return newModel
}