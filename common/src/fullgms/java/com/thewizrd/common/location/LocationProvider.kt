package com.thewizrd.common.location

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import androidx.core.os.ExecutorCompat
import androidx.core.os.postDelayed
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.wearable.WearableHelper
import com.thewizrd.shared_resources.utils.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.util.concurrent.Executors
import kotlin.coroutines.resume

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
}