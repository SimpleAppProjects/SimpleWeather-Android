package com.thewizrd.simpleweather.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.thewizrd.shared_resources.helpers.toImmutableCompatFlag
import com.thewizrd.shared_resources.preferences.FeatureSettings
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.LaunchActivity
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.updates.InAppUpdateManager
import java.util.concurrent.TimeUnit

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class AppUpdaterWorker(context: Context, workerParams: WorkerParameters) :
        CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "AppUpdaterWorker"

        // Sets an ID for the notification
        private const val NOT_CHANNEL_ID = "SimpleWeather.appupdates"

        @JvmStatic
        fun registerWorker(context: Context) {
            enqueueWork(context.applicationContext)
        }

        private fun enqueueWork(context: Context) {
            Logger.writeLine(Log.INFO, "%s: Requesting work; workExists: %s", TAG, isWorkScheduled(context.applicationContext).toString())

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED)
                    .setRequiresCharging(false)
                    .build()

            val updateRequest = PeriodicWorkRequest.Builder(AppUpdaterWorker::class.java, 1, TimeUnit.DAYS)
                    .setConstraints(constraints)
                    .addTag(TAG)
                    .build()

            WorkManager.getInstance(context.applicationContext)
                    .enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.KEEP, updateRequest)

            Logger.writeLine(Log.INFO, "%s: Work enqueued", TAG)
        }

        private fun isWorkScheduled(context: Context): Boolean {
            val workMgr = WorkManager.getInstance(context)
            val statuses = workMgr.getWorkInfosForUniqueWorkLiveData(TAG).value
            if (statuses.isNullOrEmpty()) return false
            var running = false
            for (workStatus in statuses) {
                running = (workStatus.state == WorkInfo.State.RUNNING
                        || workStatus.state == WorkInfo.State.ENQUEUED)
            }
            return running
        }

        private fun getLaunchUpdatesIntent(context: Context): PendingIntent {
            val i = Intent(context.applicationContext, LaunchActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            return PendingIntent.getActivity(
                context.applicationContext,
                0,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT.toImmutableCompatFlag()
            )
        }
    }

    override suspend fun doWork(): Result {
        Logger.writeLine(Log.INFO, "%s: Work started", TAG)

        // Creates instance of the manager.
        val appUpdateManager = InAppUpdateManager.create(applicationContext)

        if (appUpdateManager.checkIfUpdateAvailable()) {
            if (appUpdateManager.updatePriority > 3 && !FeatureSettings.isUpdateAvailable()) {
                // Notify user of update availability
                val mNotifyMgr = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                initChannel(mNotifyMgr)

                val mNotif = NotificationCompat.Builder(applicationContext, NOT_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_error_white)
                        .setContentTitle(applicationContext.getString(R.string.prompt_update_title))
                        .setContentText(applicationContext.getString(R.string.prompt_update_available))
                        .setContentIntent(getLaunchUpdatesIntent(applicationContext))
                        .setColor(Colors.SIMPLEBLUE)
                        .setAutoCancel(true)

                mNotifyMgr.notify((SystemClock.uptimeMillis() + appUpdateManager.updatePriority).toInt(), mNotif.build())
            }
        }

        return Result.success()
    }

    private fun initChannel(mNotifyMgr: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var mChannel = mNotifyMgr.getNotificationChannel(NOT_CHANNEL_ID)

            // "App Updates"
            val notchannel_name = applicationContext.resources.getString(R.string.not_channel_update_title)
            if (mChannel == null) {
                mChannel = NotificationChannel(NOT_CHANNEL_ID, notchannel_name, NotificationManager.IMPORTANCE_LOW)
            }

            mChannel.name = notchannel_name

            // Configure the notification channel.
            mChannel.setShowBadge(true)
            mChannel.enableLights(false)
            mChannel.enableVibration(false)
            mNotifyMgr.createNotificationChannel(mChannel)
        }
    }
}