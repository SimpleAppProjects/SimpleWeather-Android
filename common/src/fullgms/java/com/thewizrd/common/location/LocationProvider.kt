package com.thewizrd.common.location

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.thewizrd.common.R
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.common.wearable.WearableHelper
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.toLocation
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.weather_api.weatherModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.math.abs

@SuppressLint("MissingPermission")
class LocationProvider {
    companion object {
        private const val TAG = "LocationProvider"
    }

    private val mContext: Context
    private val mFusedLocationProviderClient: FusedLocationProviderClient
    private val mLocationMgr: LocationManager
    private val isGMSAvailable: Boolean
        get() = WearableHelper.isGooglePlayServicesInstalled(mContext)

    constructor(context: Context) {
        mContext = context
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        mLocationMgr = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    constructor(activity: Activity) {
        mContext = activity
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
        mLocationMgr = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private fun checkPermissions(): Boolean {
        return LocationManagerCompat.isLocationEnabled(mLocationMgr) && mContext.locationPermissionEnabled()
    }

    suspend fun getLastLocation(): Location? {
        if (!checkPermissions()) return null

        if (isGMSAvailable) {
            return mFusedLocationProviderClient.lastLocation.await()
        } else {
            val locCriteria = Criteria().apply {
                accuracy = Criteria.ACCURACY_COARSE
                isCostAllowed = false
                powerRequirement = Criteria.POWER_LOW
            }

            val provider = mLocationMgr.getBestProvider(locCriteria, true) ?: return null
            return mLocationMgr.getLastKnownLocation(provider)
        }
    }

    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        if (!checkPermissions()) {
            Logger.writeLine(Log.INFO, "$TAG: Location permission denied...")

            if (continuation.isActive) {
                continuation.resume(null)
            }
            return@suspendCancellableCoroutine
        }

        if (isGMSAvailable) {
            val cts = CancellationTokenSource()

            continuation.invokeOnCancellation {
                cts.cancel()
            }

            mFusedLocationProviderClient.getCurrentLocation(
                LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY,
                cts.token
            ).addOnCompleteListener {
                if (it.isSuccessful) {
                    Logger.writeLine(Log.INFO, "$TAG: Location update received...")

                    if (continuation.isActive) {
                        continuation.resume(it.result)
                    }
                } else {
                    it.exception?.let { ex ->
                        Logger.writeLine(Log.INFO, ex, "$TAG: Error retrieving location...")
                    }

                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        } else {
            val locCriteria = Criteria().apply {
                accuracy = Criteria.ACCURACY_COARSE
                isCostAllowed = false
                powerRequirement = Criteria.POWER_LOW
            }

            val cancelSignal = CancellationSignal()

            continuation.invokeOnCancellation {
                cancelSignal.cancel()
            }

            val provider = mLocationMgr.getBestProvider(locCriteria, true)

            if (provider == null) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
                return@suspendCancellableCoroutine
            }

            runCatching {
                LocationManagerCompat.getCurrentLocation(
                    mLocationMgr,
                    provider,
                    cancelSignal,
                    Executors.newSingleThreadExecutor()
                ) {
                    Logger.writeLine(Log.INFO, "$TAG: Location update received...")
                    if (continuation.isActive) {
                        continuation.resume(it)
                    }
                }
            }.onFailure {
                Logger.writeLine(Log.INFO, it, "$TAG: Error retrieving location...")
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }
    }

    suspend fun getLatestLocationData(previousLocation: LocationData? = null): LocationResult {
        if (!checkPermissions()) return LocationResult.PermissionDenied()

        var location = withContext(Dispatchers.IO) {
            val result: Location? = try {
                withTimeoutOrNull(5000) {
                    getLastLocation()
                }
            } catch (e: Exception) {
                null
            }
            result
        }

        /* Get current location from provider */
        if (location == null) {
            location = withTimeoutOrNull(30000) {
                getCurrentLocation()
            }
        }

        if (location != null) {
            var lastGPSLocData = settingsManager.getLastGPSLocData()

            // Check previous location difference
            if (lastGPSLocData?.isValid == true && previousLocation != null && ConversionMethods.calculateGeopositionDistance(
                    previousLocation.toLocation(),
                    location
                ) < 1600
            ) {
                return LocationResult.NotChanged(previousLocation)
            }

            if (lastGPSLocData?.isValid == true &&
                abs(
                    ConversionMethods.calculateHaversine(
                        lastGPSLocData.latitude, lastGPSLocData.longitude,
                        location.latitude, location.longitude
                    )
                ) < 1600
            ) {
                return LocationResult.NotChanged(previousLocation)
            }

            val wm = weatherModule.weatherManager

            val view = try {
                withContext(Dispatchers.IO) {
                    wm.getLocation(location)
                }
            } catch (e: WeatherException) {
                return LocationResult.Error(errorMessage = ErrorMessage.WeatherError(e))
            }

            if (view == null || view.locationQuery.isNullOrBlank()) {
                // Stop since there is no valid query
                return LocationResult.Error(errorMessage = ErrorMessage.Resource(R.string.error_retrieve_location))
            } else if (view.locationTZLong.isNullOrBlank() && view.locationLat != 0.0 && view.locationLong != 0.0) {
                val tzId =
                    weatherModule.tzdbService.getTimeZone(view.locationLat, view.locationLong)
                if ("unknown" != tzId)
                    view.locationTZLong = tzId
            }

            // Save location as last known
            lastGPSLocData = view.toLocationData(location)

            return LocationResult.Changed(lastGPSLocData, true)
        }

        return LocationResult.NotChanged(previousLocation, false)
    }
}