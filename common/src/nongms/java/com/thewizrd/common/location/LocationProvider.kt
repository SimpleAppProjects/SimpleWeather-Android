package com.thewizrd.common.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.shared_resources.utils.Logger
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import kotlin.coroutines.resume

@SuppressLint("MissingPermission")
class LocationProvider(private val context: Context) {
    companion object {
        private const val TAG = "LocationProvider"
    }

    private val mLocationMgr = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private fun checkPermissions(): Boolean {
        return LocationManagerCompat.isLocationEnabled(mLocationMgr) && context.locationPermissionEnabled()
    }

    suspend fun getLastLocation(): Location? {
        if (!checkPermissions()) return null

        val locCriteria = Criteria().apply {
            accuracy = Criteria.ACCURACY_COARSE
            isCostAllowed = false
            powerRequirement = Criteria.POWER_LOW
        }

        val provider = mLocationMgr.getBestProvider(locCriteria, true) ?: return null
        return mLocationMgr.getLastKnownLocation(provider)
    }

    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        if (!checkPermissions()) {
            Logger.writeLine(Log.INFO, "$TAG: Location permission denied...")

            if (continuation.isActive) {
                continuation.resume(null)
            }
            return@suspendCancellableCoroutine
        }

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