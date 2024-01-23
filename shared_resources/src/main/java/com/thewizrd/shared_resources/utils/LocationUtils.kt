@file:Suppress("PropertyName")

package com.thewizrd.shared_resources.utils

import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.LocationQuery

object LocationUtils {
    // Source: https://gist.github.com/graydon/11198540
    private val US_BOUNDING_BOX = BoundingBox(24.9493, 49.5904, -125.0011, -66.9326)
    private val USCA_BOUNDING_BOX =
        BoundingBox(24.4825578966, 71.7611572494, -168.9184947286, -52.2436900411)
    private val FR_BOUNDING_BOX = BoundingBox(41.2632185, 51.268318, -5.4534286, 9.8678344)

    fun isUS(countryCode: String?): Boolean {
        return if (countryCode.isNullOrBlank()) {
            false
        } else {
            countryCode.equals("us", ignoreCase = true) || countryCode.equals(
                "usa",
                ignoreCase = true
            ) || countryCode.lowercase().contains("united states")
        }
    }

    fun isUS(location: LocationData): Boolean {
        return if (!location.countryCode.isNullOrBlank()) {
            isUS(location.countryCode)
        } else {
            US_BOUNDING_BOX.intersects(location.latitude, location.longitude)
        }
    }

    fun isUS(location: LocationQuery): Boolean {
        return if (!location.locationCountry.isNullOrBlank()) {
            isUS(location.locationCountry)
        } else {
            US_BOUNDING_BOX.intersects(location.locationLat, location.locationLong)
        }
    }

    fun isUSorCanada(countryCode: String?): Boolean {
        return if (countryCode.isNullOrBlank()) {
            false
        } else {
            isUS(countryCode) || countryCode.equals(
                "CA",
                ignoreCase = true
            ) || countryCode.lowercase().contains("canada")
        }
    }

    fun isUSorCanada(location: LocationData): Boolean {
        return if (!location.countryCode.isNullOrBlank()) {
            isUSorCanada(location.countryCode)
        } else {
            USCA_BOUNDING_BOX.intersects(location.latitude, location.longitude)
        }
    }

    fun isUSorCanada(location: LocationQuery): Boolean {
        return if (!location.locationCountry.isNullOrBlank()) {
            isUSorCanada(location.locationCountry)
        } else {
            USCA_BOUNDING_BOX.intersects(location.locationLat, location.locationLong)
        }
    }

    fun isFrance(countryCode: String?): Boolean {
        return if (countryCode.isNullOrBlank()) {
            false
        } else {
            countryCode.equals("fr", ignoreCase = true) || countryCode.equals(
                "france",
                ignoreCase = true
            )
        }
    }

    fun isFrance(location: LocationData): Boolean {
        return if (!location.countryCode.isNullOrBlank()) {
            isFrance(location.countryCode)
        } else {
            FR_BOUNDING_BOX.intersects(location.latitude, location.longitude)
        }
    }

    fun isFrance(location: LocationQuery): Boolean {
        return if (!location.locationCountry.isNullOrBlank()) {
            isFrance(location.locationCountry)
        } else {
            FR_BOUNDING_BOX.intersects(location.locationLat, location.locationLong)
        }
    }

    private data class BoundingBox(
        val lat_min: Double,
        val lat_max: Double,
        val lon_min: Double,
        val lon_max: Double,
    ) {
        fun intersects(lat: Double, lon: Double): Boolean {
            return (lat in lat_min..lat_max) && (lon in lon_min..lon_max)
        }
    }
}
