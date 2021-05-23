package com.thewizrd.simpleweather.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.work.*
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.simpleweather.services.ImageDatabaseWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class DailyWeatherNotificationWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    companion object {
        suspend fun executeWork(context: Context) {
            // no-op
        }

        fun scheduleNotification(context: Context) {
            // no-op
        }

        fun cancelWork(context: Context) {
            // no-op
        }
    }

    override suspend fun doWork(): Result {
        return Result.success()
    }
}