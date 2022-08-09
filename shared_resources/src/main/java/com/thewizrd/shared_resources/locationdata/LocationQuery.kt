package com.thewizrd.shared_resources.locationdata

import android.location.Location
import androidx.annotation.RestrictTo
import com.thewizrd.shared_resources.R
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherAPI.WeatherProviders
import com.thewizrd.shared_resources.weatherdata.model.LocationType
import java.text.DecimalFormat
import java.util.*

class LocationQuery {
    companion object {
        fun clone(model: LocationQuery): LocationQuery {
            return LocationQuery().apply {
                locationQuery = model.locationQuery
                locationName = model.locationName
                locationLat = model.locationLat
                locationLong = model.locationLong
                locationTZLong = model.locationTZLong
                weatherSource = model.weatherSource
                locationSource = model.locationSource
                locationCountry = model.locationCountry
            }
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun buildEmptyModel(weatherSource: String?): LocationQuery {
            return LocationQuery().apply {
                locationName = "" // Reset name
                updateWeatherSource(weatherSource)
            }
        }
    }

    var locationName: String?
    var locationRegion: String? = null
    var locationCountry: String?
    var locationQuery: String?

    var locationLat: Double = 0.0
    var locationLong: Double = 0.0
    var locationTZLong: String? = null
    var locationSource: String? = null
    var weatherSource: String? = null

    constructor() {
        locationName = sharedDeps.context.getString(R.string.error_noresults)
        locationCountry = ""
        locationQuery = ""
    }

    constructor(data: LocationData) {
        locationQuery = data.query
        locationName = data.name
        locationLat = data.latitude
        locationLong = data.longitude
        locationTZLong = data.tzLong
        weatherSource = data.weatherSource
        locationSource = data.locationSource
        locationCountry = data.countryCode
    }

    private fun updateLocationQuery() {
        val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
        df.applyPattern("0.####")

        locationQuery = when (weatherSource) {
            WeatherAPI.HERE -> {
                String.format(
                    Locale.ROOT,
                    "latitude=%s&longitude=%s",
                    df.format(locationLat),
                    df.format(locationLong)
                )
            }
            WeatherAPI.WEATHERUNLOCKED,
            WeatherAPI.WEATHERAPI,
            WeatherAPI.TOMORROWIO,
            WeatherAPI.ACCUWEATHER,
            WeatherAPI.METEOMATICS -> {
                String.format(
                    Locale.ROOT,
                    "%s,%s",
                    df.format(locationLat),
                    df.format(locationLong)
                )
            }
            else -> {
                String.format(
                    Locale.ROOT,
                    "lat=%s&lon=%s",
                    df.format(locationLat),
                    df.format(locationLong)
                )
            }
        }
    }

    fun updateWeatherSource(@WeatherProviders API: String?) {
        weatherSource = API
        updateLocationQuery()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LocationQuery

        if (locationName != other.locationName) return false
        if (locationRegion != other.locationRegion) return false
        if (locationCountry != other.locationCountry) return false
        if (locationQuery != other.locationQuery) return false
        if (locationLat != other.locationLat) return false
        if (locationLong != other.locationLong) return false
        if (locationTZLong != other.locationTZLong) return false
        if (locationSource != other.locationSource) return false
        if (weatherSource != other.weatherSource) return false

        return true
    }

    override fun hashCode(): Int {
        var result = locationName?.hashCode() ?: 0
        result = 31 * result + (locationRegion?.hashCode() ?: 0)
        result = 31 * result + (locationCountry?.hashCode() ?: 0)
        result = 31 * result + (locationQuery?.hashCode() ?: 0)
        result = 31 * result + locationLat.hashCode()
        result = 31 * result + locationLong.hashCode()
        result = 31 * result + (locationTZLong?.hashCode() ?: 0)
        result = 31 * result + (locationSource?.hashCode() ?: 0)
        result = 31 * result + (weatherSource?.hashCode() ?: 0)
        return result
    }
}

fun LocationQuery.toLocationData(type: LocationType = LocationType.SEARCH): LocationData {
    val locQuery = this

    return LocationData().apply {
        query = locQuery.locationQuery
        name = locQuery.locationName
        latitude = locQuery.locationLat
        longitude = locQuery.locationLong
        tzLong = locQuery.locationTZLong
        weatherSource = locQuery.weatherSource
        locationSource = locQuery.locationSource
        locationType = type
    }
}

fun LocationQuery.toLocationData(location: Location): LocationData {
    val locQuery = this

    return LocationData().apply {
        query = locQuery.locationQuery
        name = locQuery.locationName
        latitude = location.latitude
        longitude = location.longitude
        tzLong = locQuery.locationTZLong
        locationType = LocationType.GPS
        weatherSource = locQuery.weatherSource
        locationSource = locQuery.locationSource
    }
}