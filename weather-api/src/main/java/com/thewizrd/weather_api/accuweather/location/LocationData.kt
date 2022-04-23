package com.thewizrd.weather_api.accuweather.location

import com.thewizrd.shared_resources.locationdata.LocationQuery
import com.thewizrd.shared_resources.weatherdata.WeatherAPI

fun createLocationModel(result: GeopositionResponse): LocationQuery {
    return LocationQuery().apply {
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

fun createLocationModel(result: GeopositionResponse, oldModel: LocationQuery):
        LocationQuery {
    val newModel = LocationQuery.clone(oldModel)

    newModel.locationQuery = result.key
    newModel.locationTZLong = oldModel.locationTZLong ?: result.timeZone?.name
    newModel.locationCountry = oldModel.locationCountry ?: result.country?.iD
    newModel.locationSource = WeatherAPI.ACCUWEATHER
    newModel.weatherSource = WeatherAPI.ACCUWEATHER

    return newModel
}