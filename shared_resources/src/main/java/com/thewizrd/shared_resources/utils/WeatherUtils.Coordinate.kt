@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("WeatherUtils")

package com.thewizrd.shared_resources.utils

import android.location.Location
import com.thewizrd.shared_resources.locationdata.LocationData
import java.util.*

class Coordinate {
    var latitude = 0.0
        private set
    var longitude = 0.0
        private set

    constructor(coordinatePair: String) {
        setCoordinate(coordinatePair)
    }

    constructor(latitude: Double, longitude: Double) {
        setCoordinate(latitude, longitude)
    }

    constructor(location: Location) {
        latitude = location.latitude
        longitude = location.longitude
    }

    constructor(location: LocationData) {
        latitude = location.latitude
        longitude = location.longitude
    }

    fun setCoordinate(coordinatePair: String) {
        val coord = coordinatePair.split(",").toTypedArray()
        latitude = coord[0].toDouble()
        longitude = coord[1].toDouble()
    }

    fun setCoordinate(latitude: Double, longitude: Double) {
        this.latitude = latitude
        this.longitude = longitude
    }

    override fun toString(): String {
        return String.format(Locale.ROOT, "%s,%s", latitude.toString(), longitude.toString())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as Coordinate

        return if (that.latitude.compareTo(latitude) != 0) {
            false
        } else {
            that.longitude.compareTo(longitude) == 0
        }
    }

    override fun hashCode(): Int {
        var result: Int
        var temp: Long = java.lang.Double.doubleToLongBits(latitude)
        result = (temp xor (temp ushr 32)).toInt()
        temp = java.lang.Double.doubleToLongBits(longitude)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        return result
    }
}
