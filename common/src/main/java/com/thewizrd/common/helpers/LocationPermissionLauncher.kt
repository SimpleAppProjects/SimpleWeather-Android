package com.thewizrd.common.helpers

import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi

class LocationPermissionLauncher(
    private val activity: ComponentActivity,
    private val locationCallback: ((Boolean) -> Unit)? = null,
    private val bgLocationCallback: ((Boolean) -> Unit)? = null,
) {
    private val locationPermissionLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            locationCallback?.invoke(it[Manifest.permission.ACCESS_COARSE_LOCATION] == true || it[Manifest.permission.ACCESS_FINE_LOCATION] == true)
        }

    private val bgLocationPermissionLauncher: ActivityResultLauncher<String> =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            bgLocationCallback?.invoke(it)
        }

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