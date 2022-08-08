@file:JvmMultifileClass
@file:JvmName("GeocoderExt")

package com.thewizrd.weather_api.google.location

import android.location.Geocoder
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailabilityLight
import com.thewizrd.shared_resources.sharedDeps

fun isGeocoderAvailable(): Boolean {
    return Geocoder.isPresent() && isGooglePlayServicesAvailable()
}

private fun isGooglePlayServicesAvailable(): Boolean {
    val gPlayAvailability = GoogleApiAvailabilityLight.getInstance()

    return when (gPlayAvailability.isGooglePlayServicesAvailable(sharedDeps.context)) {
        ConnectionResult.SUCCESS -> true
        else -> false
    }
}