package com.thewizrd.common.helpers

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment

class LocationPermissionLauncher {
    constructor(
        activity: ComponentActivity,
        locationCallback: ((Boolean) -> Unit)? = null,
        bgLocationCallback: ((Boolean) -> Unit)? = null
    ) {
        this.locationPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                locationCallback?.invoke(it[Manifest.permission.ACCESS_COARSE_LOCATION] == true || it[Manifest.permission.ACCESS_FINE_LOCATION] == true)
            }
        this.bgLocationPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                bgLocationCallback?.invoke(it)
            }
    }

    constructor(
        fragment: Fragment,
        locationCallback: ((Boolean) -> Unit)? = null,
        bgLocationCallback: ((Boolean) -> Unit)? = null
    ) {
        this.locationPermissionLauncher =
            fragment.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                locationCallback?.invoke(it[Manifest.permission.ACCESS_COARSE_LOCATION] == true || it[Manifest.permission.ACCESS_FINE_LOCATION] == true)
            }
        this.bgLocationPermissionLauncher =
            fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                bgLocationCallback?.invoke(it)
            }
    }

    private val locationPermissionLauncher: ActivityResultLauncher<Array<String>>

    private val bgLocationPermissionLauncher: ActivityResultLauncher<String>

    fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else {
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun requestBackgroundLocationPermission() {
        bgLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }
}

fun ComponentActivity.createLocationPermissionLauncher(
    locationCallback: ((Boolean) -> Unit)? = null,
    bgLocationCallback: ((Boolean) -> Unit)? = null
): LocationPermissionLauncher {
    return LocationPermissionLauncher(this, locationCallback, bgLocationCallback)
}

fun Fragment.createLocationPermissionLauncher(
    locationCallback: ((Boolean) -> Unit)? = null,
    bgLocationCallback: ((Boolean) -> Unit)? = null
): LocationPermissionLauncher {
    return LocationPermissionLauncher(this, locationCallback, bgLocationCallback)
}