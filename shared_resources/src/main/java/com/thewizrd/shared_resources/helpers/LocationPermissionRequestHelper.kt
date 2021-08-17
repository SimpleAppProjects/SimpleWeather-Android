package com.thewizrd.shared_resources.helpers

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.thewizrd.shared_resources.R

fun Context.locationPermissionEnabled(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}

fun Context.backgroundLocationPermissionEnabled(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        return true
    }
}

fun Activity.requestLocationPermission(locationRequestCode: Int) {
    val permList = if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
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

    ActivityCompat.requestPermissions(this, permList, locationRequestCode)
}

fun Fragment.requestLocationPermission(locationRequestCode: Int) {
    val permList = if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
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

    this.requestPermissions(permList, locationRequestCode)
}

@RequiresApi(Build.VERSION_CODES.M)
fun android.app.Fragment.requestLocationPermission(locationRequestCode: Int) {
    val permList = if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
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

    this.requestPermissions(permList, locationRequestCode)
}

@RequiresApi(Build.VERSION_CODES.Q)
fun Activity.requestBackgroundLocationPermission(locationRequestCode: Int) {
    ActivityCompat.requestPermissions(
        this,
        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
        locationRequestCode
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
fun Fragment.requestBackgroundLocationPermission(locationRequestCode: Int) {
    this.requestPermissions(
        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
        locationRequestCode
    )
}

@RequiresApi(Build.VERSION_CODES.Q)
fun android.app.Fragment.requestBackgroundLocationPermission(locationRequestCode: Int) {
    this.requestPermissions(
        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
        locationRequestCode
    )
}

@SuppressLint("QueryPermissionsNeeded")
fun Context.openAppSettingsActivity() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${packageName}")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    if (intent.resolveActivity(packageManager) != null) {
        this.startActivity(intent)
    }
}

fun Context.getBackgroundLocationRationale(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        this.getString(
            R.string.bg_location_permission_rationale_settings,
            this.packageManager.backgroundPermissionOptionLabel
        )
    } else {
        this.getString(R.string.bg_location_permission_rationale)
    }
}