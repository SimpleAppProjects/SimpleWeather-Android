@file:JvmMultifileClass
@file:JvmName("GeocoderExt")

package com.thewizrd.weather_api.google.location

import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.weather_api.google.utils.GeocoderException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.DecimalFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Throws(GeocoderException::class, IllegalArgumentException::class, IOException::class)
suspend fun Geocoder.getFromLocationNameAsync(
    locationName: String,
    maxResults: Int
): List<Address> = withContext(Dispatchers.IO) {
    val cacheKey = "name:$locationName|locale:${LocaleUtils.getLocale().toLanguageTag()}"

    try {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
                            continuation.resumeWithException(GeocoderException(errorMessage))
                        }
                    }
                })
            }
        } else {
            getFromLocationName(locationName, maxResults) ?: emptyList()
        }

        if (result.isNotEmpty()) {
            GeocoderCache.put(cacheKey, result)
        }

        result
    } catch (e: Exception) {
        if (e is IOException || e is GeocoderException) {
            GeocoderCache.get(cacheKey)?.let { cachedResults ->
                if (cachedResults.isNotEmpty()) {
                    Logger.warn("Geocoder", e)

                    return@withContext if (cachedResults.size > maxResults) {
                        cachedResults.subList(0, maxResults)
                    } else {
                        cachedResults
                    }
                }
            }
        }
        throw e
    }
}

@Throws(GeocoderException::class, IllegalArgumentException::class, IOException::class)
suspend fun Geocoder.getFromLocationAsync(
    latitude: Double,
    longitude: Double,
    maxResults: Int
): List<Address> = withContext(Dispatchers.IO) {
    val df = DecimalFormat.getInstance(Locale.ROOT) as DecimalFormat
    df.applyPattern("0.####")

    val cacheKey = "loc:${df.format(latitude)},${df.format(longitude)}|locale:${
        LocaleUtils.getLocale().toLanguageTag()
    }"

    try {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
                            continuation.resumeWithException(GeocoderException(errorMessage))
                        }
                    }
                })
            }
        } else {
            getFromLocation(latitude, longitude, maxResults) ?: emptyList()
        }

        if (result.isNotEmpty()) {
            GeocoderCache.put(cacheKey, result)
        }

        result
    } catch (e: Exception) {
        if (e is IOException || e is GeocoderException) {
            GeocoderCache.get(cacheKey)?.let { cachedResults ->
                if (cachedResults.isNotEmpty()) {
                    Logger.warn("Geocoder", e)

                    return@withContext if (cachedResults.size > maxResults) {
                        cachedResults.subList(0, maxResults)
                    } else {
                        cachedResults
                    }
                }
            }
        }
        throw e
    }
}
