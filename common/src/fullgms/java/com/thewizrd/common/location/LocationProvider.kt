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
import androidx.core.location.LocationManagerCompat
import androidx.core.os.postDelayed
import com.google.android.gms.location.*
import com.thewizrd.common.helpers.locationPermissionEnabled
import com.thewizrd.common.wearable.WearableHelper
import kotlinx.coroutines.tasks.await

@SuppressLint("MissingPermission")
class LocationProvider {
    private val mContext: Context
    private val mFusedLocationProviderClient: FusedLocationProviderClient
    private val mLocationMgr: LocationManager
    private val isGMSAvailable: Boolean
        get() = WearableHelper.isGooglePlayServicesInstalled(mContext)

    private var locationCallback: LocationCallback? = null
    private var locationListener: LocationListener? = null

    private val mMainHandler = Handler(Looper.getMainLooper())
    private val handlerToken = Object()

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

    fun requestSingleUpdate(callback: Callback, looper: Looper?, timeoutInMs: Long? = null) {
        if (!checkPermissions()) {
            callback.onLocationChanged(null)
            return
        }

        if (isGMSAvailable) {
            val mLocationRequest = LocationRequest.create().apply {
                numUpdates = 1
                interval = 10000
                fastestInterval = 1000
                priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            }

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(p0: LocationResult) {
                    callback.onLocationChanged(p0.lastLocation)
                }

                override fun onLocationAvailability(p0: LocationAvailability) {
                    if (!p0.isLocationAvailable) {
                        callback.onLocationChanged(null)
                    }
                }
            }

            timeoutInMs?.let {
                mLocationRequest.setExpirationDuration(it)
            }

            mFusedLocationProviderClient.requestLocationUpdates(
                mLocationRequest,
                locationCallback,
                looper
            )
        } else {
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
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            mFusedLocationProviderClient.removeLocationUpdates(it)
        }
        locationListener?.let {
            mLocationMgr.removeUpdates(it)
        }
    }

    interface Callback {
        fun onLocationChanged(location: Location?)
        fun onRequestTimedOut()
    }
}