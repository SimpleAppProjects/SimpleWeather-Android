package com.thewizrd.simpleweather.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ImageDatabaseWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object {
        @JvmStatic
        fun enqueueAction(context: Context, intentAction: String) {
        }
    }

    override suspend fun doWork(): Result {
        return Result.success()
    }
}