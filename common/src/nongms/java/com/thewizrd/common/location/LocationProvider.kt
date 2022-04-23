package com.thewizrd.common.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.location.LocationManagerCompat
import androidx.core.os.postDelayed
import com.thewizrd.common.helpers.locationPermissionEnabled

@SuppressLint("MissingPermission")
class LocationProvider(private val context: Context) {
    private val mLocationMgr = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var locationListener: LocationListener? = null

    private val mMainHandler = Handler(Looper.getMainLooper())
    private val handlerToken = Object()

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

    fun requestSingleUpdate(callback: Callback, looper: Looper?, timeoutInMs: Long? = null) {
        if (!checkPermissions()) {
            callback.onLocationChanged(null)
            return
        }

        val locCriteria = Criteria().apply {
            accuracy = Criteria.ACCURACY_COARSE
            isCostAllowed = false
            powerRequirement = Criteria.POWER_LOW
        }

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                mMainHandler.removeCallbacksAndMessages(handlerToken)
                callback.onLocationChanged(location)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                // no-op
            }

            override fun onProviderEnabled(provider: String) {
                // no-op
            }

            override fun onProviderDisabled(provider: String) {
                // no-op
            }

        }

        val provider = mLocationMgr.getBestProvider(locCriteria, true)
            ?: return callback.onLocationChanged(null)

        timeoutInMs?.let {
            mMainHandler.postDelayed(it, handlerToken) {
                stopLocationUpdates()
                callback.onRequestTimedOut()
            }
        }

        mLocationMgr.requestSingleUpdate(provider, locationListener!!, looper)
    }

    fun stopLocationUpdates() {
        locationListener?.let {
            mLocationMgr.removeUpdates(it)
        }
    }

    interface Callback {
        fun onLocationChanged(location: Location?)
        fun onRequestTimedOut()
    }
}