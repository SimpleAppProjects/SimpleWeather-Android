@file:JvmMultifileClass
@file:JvmName("GeocoderExt")

package com.thewizrd.weather_api.google.location

import android.location.Geocoder
import com.thewizrd.shared_resources.sharedDeps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun isGeocoderAvailable(): Boolean {
    return false
}