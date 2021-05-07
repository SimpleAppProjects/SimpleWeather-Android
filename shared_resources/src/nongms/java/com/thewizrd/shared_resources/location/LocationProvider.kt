package com.thewizrd.shared_resources.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

@SuppressLint("MissingPermission")
class LocationProvider(private val context: Context) {
    private val mLocationMgr = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var locationListener: LocationListener? = null

    private fun checkPermissions(): Boolean {
        if (!LocationManagerCompat.isLocationEnabled(mLocationMgr) ||
            !(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
              ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            return false
        }

        return true
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

    fun requestSingleUpdate(callback: Callback, looper: Looper?) {
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
            override fun onLocationChanged(location: Location?) {
                callback.onLocationChanged(location)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
                // no-op
            }

            override fun onProviderEnabled(provider: String?) {
                // no-op
            }

            override fun onProviderDisabled(provider: String?) {
                // no-op
            }

        }

        val provider = mLocationMgr.getBestProvider(locCriteria, true)
                       ?: return callback.onLocationChanged(null)
        mLocationMgr.requestSingleUpdate(provider, locationListener!!, looper)
    }

    fun stopLocationUpdates() {
        locationListener?.let {
            mLocationMgr.removeUpdates(it)
        }
    }

    interface Callback {
        fun onLocationChanged(location: Location?)
    }
}