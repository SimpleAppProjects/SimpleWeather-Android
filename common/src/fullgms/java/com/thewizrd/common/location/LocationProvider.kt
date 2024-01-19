package com.thewizrd.common.location

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import com.thewizrd.common.R
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.utils.ErrorMessage
import com.thewizrd.common.wearable.WearableHelper
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.exceptions.WeatherException
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.locationdata.toLocation
import com.thewizrd.shared_resources.locationdata.toLocationData
import com.thewizrd.shared_resources.utils.ContextUtils.isPhone
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

        val isFusedLocationAvailable = canUseFusedLocation().await()

        if (isFusedLocationAvailable) {
            return mFusedLocationProviderClient.lastLocation.await()
        } else {
            val provider = getBestProvider() ?: return null
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

        canUseFusedLocation()
            .continueWith { task ->
                if (!continuation.isActive) return@continueWith

                val isFusedLocationAvailable = task.result

                if (isFusedLocationAvailable) {
                    val cts = CancellationTokenSource()

                    continuation.invokeOnCancellation {
                        cts.cancel()
                    }

                    mFusedLocationProviderClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
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
                    val cancelSignal = CancellationSignal()

                    continuation.invokeOnCancellation {
                        cancelSignal.cancel()
                    }

                    val provider = getBestProvider()

                    if (provider == null) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                        return@continueWith
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
    }

    suspend fun getLatestLocationData(previousLocation: LocationData? = null): LocationResult {
        if (!LocationManagerCompat.isLocationEnabled(mLocationMgr)) {
            return LocationResult.Error(
                errorMessage = ErrorMessage.Resource(R.string.error_enable_location_services)
            )
        }
        if (!mContext.locationPermissionEnabled()) return LocationResult.PermissionDenied()

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
                return LocationResult.NotChanged(lastGPSLocData)
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

    private fun getBestProvider(): String? {
        val enabledProviders = mLocationMgr.getProviders(true)

        if (enabledProviders.isNotEmpty()) {
            return if (mContext.isPhone() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && enabledProviders.contains(
                    LocationManager.FUSED_PROVIDER
                )
            ) {
                LocationManager.FUSED_PROVIDER
            } else if (enabledProviders.contains(LocationManager.GPS_PROVIDER)) {
                LocationManager.GPS_PROVIDER
            } else if (enabledProviders.contains(LocationManager.NETWORK_PROVIDER)) {
                LocationManager.NETWORK_PROVIDER
            } else if (enabledProviders.contains(LocationManager.PASSIVE_PROVIDER)) {
                LocationManager.PASSIVE_PROVIDER
            } else {
                enabledProviders.first()
            }
        }

        return null
    }

    /**
     * On non-wearables: Check if Google Play Services are available. If so, return [com.google.android.gms.location.LocationAvailability.isLocationAvailable]
     *
     * On wearables: Check if Google Play Services are available. If so, check if any paired phone is available and nearby.
     * If so, confirm fused location availability [com.google.android.gms.location.LocationAvailability.isLocationAvailable]
     *
     * @see [FusedLocationProviderClient.getLocationAvailability]
     * @see [com.google.android.gms.wearable.NodeClient.getConnectedNodes]
     * @return true if we can use fused location provider
     */
    private fun canUseFusedLocation(): Task<Boolean> {
        return Tasks.forResult(isGMSAvailable)
            .continueWithTask {
                val isGMSAvailable = it.result

                if (isGMSAvailable) {
                    if (!mContext.isPhone()) {
                        // Wearables: FusedLocationProvider will likely not be of use if phone is not connected nearby
                        // Verify phone status
                        Wearable.getNodeClient(mContext)
                            .connectedNodes
                            .continueWithTask { nodesTask ->
                                if (it.isSuccessful) {
                                    val nodes = nodesTask.result
                                    val isNearbyNodes = nodes.any { n -> n.isNearby }
                                    if (isNearbyNodes) {
                                        isFusedLocationAvailable()
                                    } else {
                                        Tasks.forResult(false)
                                    }
                                } else {
                                    Tasks.forResult(false)
                                }
                            }
                    } else {
                        isFusedLocationAvailable()
                    }
                } else {
                    Tasks.forResult(false)
                }
            }
    }

    private fun isFusedLocationAvailable(): Task<Boolean> {
        return mFusedLocationProviderClient.locationAvailability.continueWith { avail ->
            if (avail.isSuccessful) {
                avail.result.isLocationAvailable
            } else {
                false
            }
        }
    }
}