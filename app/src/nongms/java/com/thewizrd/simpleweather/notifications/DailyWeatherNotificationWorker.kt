package com.thewizrd.simpleweather.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DailyWeatherNotificationWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    companion object {
        suspend fun executeWork(context: Context) {
            // no-op
        }

        fun scheduleNotification(context: Context) {
            // no-op
        }

        fun sendDailyNotification(context: Context) {
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