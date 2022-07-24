package com.thewizrd.common.helpers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
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
            getBackgroundLocationRationalResId(),
            this.packageManager.backgroundPermissionOptionLabel
        )
    } else {
        this.getString(getBackgroundLocationRationalResId())
    }
}

@StringRes
private fun Context.getBackgroundLocationRationalResId(): Int {
    val isWatch =
        resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK == Configuration.UI_MODE_TYPE_WATCH

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (isWatch) {
            R.string.wear_bg_location_permission_rationale_settings
        } else {
            R.string.bg_location_permission_rationale_settings
        }
    } else {
        if (isWatch) {
            R.string.wear_bg_location_permission_rationale
        } else {
            R.string.bg_location_permission_rationale
        }
    }
}