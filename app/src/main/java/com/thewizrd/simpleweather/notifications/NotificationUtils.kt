package com.thewizrd.simpleweather.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

class NotificationUtils {
    companion object {
        @JvmStatic
        fun getAppSettingsActivityIntent(context: Context): Intent {
            return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @JvmStatic
        fun getAppNotificationSettingsActivityIntent(context: Context): Intent {
            return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @JvmStatic
        fun getAppNotificationChannelSettingsActivityIntent(context: Context, notificationChannelId: String): Intent {
            return Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, notificationChannelId)
            }
        }
    }
}