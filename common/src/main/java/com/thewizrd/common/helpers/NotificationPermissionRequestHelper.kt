package com.thewizrd.common.helpers

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.PermissionChecker

fun Context.notificationPermissionEnabled(): Boolean {
    return if (Build.VERSION.SDK_INT >= 33) {
        PermissionChecker.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PermissionChecker.PERMISSION_GRANTED
    } else {
        return true
    }
}

fun Context.areNotificationsEnabled(): Boolean {
    return NotificationManagerCompat.from(this).areNotificationsEnabled()
}