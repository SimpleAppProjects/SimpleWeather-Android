@file:JvmMultifileClass
@file:JvmName("GeocoderExt")

package com.thewizrd.weather_api.google.location

import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Throws(Exception::class)
suspend fun Geocoder.getFromLocationNameAsync(
    locationName: String,
    maxResults: Int
): List<Address> = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                //
            }

            getFromLocationName(locationName, maxResults, object : Geocoder.GeocodeListener {
                override fun onGeocode(p0: List<Address>) {
                    if (continuation.isActive) {
                        continuation.resume(p0)
                    }
                }

                override fun onError(errorMessage: String?) {
                    super.onError(errorMessage)
                    if (continuation.isActive) {
                        continuation.resumeWithException(Exception(errorMessage))
                    }
                }
            })
        }
    } else {
        getFromLocationName(locationName, maxResults) ?: emptyList()
    }
}

@Throws(Exception::class)
suspend fun Geocoder.getFromLocationAsync(
    latitude: Double,
    longitude: Double,
    maxResults: Int
): List<Address> = withContext(Dispatchers.IO) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                //
            }

            getFromLocation(latitude, longitude, maxResults, object : Geocoder.GeocodeListener {
                override fun onGeocode(p0: List<Address>) {
                    if (continuation.isActive) {
                        continuation.resume(p0)
                    }
                }

                override fun onError(errorMessage: String?) {
                    super.onError(errorMessage)
                    if (continuation.isActive) {
                        continuation.resumeWithException(Exception(errorMessage))
                    }
                }
            })
        }
    } else {
        getFromLocation(latitude, longitude, maxResults) ?: emptyList()
    }
}