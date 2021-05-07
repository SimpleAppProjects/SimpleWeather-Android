package com.thewizrd.simpleweather.wearable

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WearableWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun enqueueAction(context: Context, intentAction: String, urgent: Boolean = true) {
            // no-op
        }
    }

    override suspend fun doWork(): Result {
        return Result.success()
    }
}