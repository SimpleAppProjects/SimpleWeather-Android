package com.thewizrd.simpleweather.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.*
import com.thewizrd.shared_resources.controls.WeatherNowViewModel
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.Settings
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader
import com.thewizrd.shared_resources.weatherdata.WeatherRequest
import com.thewizrd.simpleweather.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.ExecutionException

class WeatherNotificationWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    private val mContext = context.applicationContext

    companion object {
        private const val TAG = "WeatherNotificationWorker"

        // Actions
        private const val KEY_ACTION = "action"
        const val ACTION_REFRESHNOTIFICATION = "SimpleWeather.Droid.action.REFRESH_NOTIFICATION"
        const val ACTION_REMOVENOTIFICATION = "SimpleWeather.Droid.action.REMOVE_NOTIFICATION"

        // Extras
        const val EXTRA_FORCEREFRESH = "SimpleWeather.Droid.extra.FORCE_REFRESH"

        // Sets an ID for the notification
        private const val NOT_CHANNEL_ID = "SimpleWeather.ongoingweather"
        private const val JOB_ID = 1003
        private const val PERSISTENT_NOT_ID = JOB_ID

        @JvmStatic
        fun enqueueAction(context: Context, intent: Intent) {
            val context = context.applicationContext

            if (intent.action != null) {
                when (intent.action) {
                    ACTION_REFRESHNOTIFICATION,
                    ACTION_REMOVENOTIFICATION -> {
                        startWork(context, intent)
                    }
                }
            }
        }

        private fun startWork(context: Context, intent: Intent) {
            val context = context.applicationContext

            Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG)

            val updateRequest = OneTimeWorkRequest.Builder(WeatherNotificationWorker::class.java)
                    .setConstraints(Constraints.NONE)
                    .setInputData(
                            Data.Builder()
                                    .putString(KEY_ACTION, intent.action)
                                    .putBoolean(EXTRA_FORCEREFRESH, intent.getBooleanExtra(EXTRA_FORCEREFRESH, false))
                                    .build()
                    )
                    .build()

            WorkManager.getInstance(context)
                    .enqueueUniqueWork(String.format(Locale.ROOT, "%s:%s_oneTime", TAG, intent.action),
                            ExistingWorkPolicy.REPLACE, updateRequest)

            Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG)
        }
    }

    override suspend fun doWork(): Result {
        Logger.writeLine(Log.INFO, "%s: Work started", TAG)

        val intentAction = inputData.getString(KEY_ACTION)
        val forceRefresh = inputData.getBoolean(EXTRA_FORCEREFRESH, false)

        Logger.writeLine(Log.INFO, "%s: Action: %s", TAG, intentAction)

        if (ACTION_REFRESHNOTIFICATION == intentAction) {
            if (Settings.isWeatherLoaded()) {
                val weather = withContext(Dispatchers.IO) {
                    val locData = Settings.getHomeData()
                    val wLoader = WeatherDataLoader(locData)
                    val request = WeatherRequest.Builder()
                    if (forceRefresh)
                        request.forceRefresh(false)
                    else
                        request.forceLoadSavedData()

                    try {
                        wLoader.loadWeatherData(request.build()).await()
                    } catch (e: ExecutionException) {
                        null
                    } catch (e: InterruptedException) {
                        null
                    }
                }

                if (Settings.showOngoingNotification() && weather != null) {
                    // Gets an instance of the NotificationManager service
                    val mNotifyMgr = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    initChannel(mNotifyMgr)

                    // Update notification
                    val mNotification = WeatherNotificationBuilder.updateNotification(NOT_CHANNEL_ID, WeatherNowViewModel(weather))
                    mNotifyMgr.notify(PERSISTENT_NOT_ID, mNotification)
                } else if (!Settings.showOngoingNotification()) {
                    removeNotification()
                }
            }
        } else if (ACTION_REMOVENOTIFICATION == intentAction) {
            removeNotification()
        }

        return Result.success()
    }

    private fun initChannel(mNotifyMgr: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var mChannel = mNotifyMgr.getNotificationChannel(NOT_CHANNEL_ID)

            val notchannel_name = mContext.resources.getString(R.string.not_channel_name_weather)
            val notchannel_desc = mContext.resources.getString(R.string.not_channel_desc_weather)

            if (mChannel == null) {
                mChannel = NotificationChannel(NOT_CHANNEL_ID, notchannel_name, NotificationManager.IMPORTANCE_LOW)
            }
            mChannel.name = notchannel_name
            mChannel.description = notchannel_desc
            // Configure the notification channel.
            mChannel.setShowBadge(true)
            mChannel.enableLights(false)
            mChannel.enableVibration(false)
            mNotifyMgr.createNotificationChannel(mChannel)
        }
    }

    private fun removeNotification() {
        val mNotifyMgr = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotifyMgr.cancel(PERSISTENT_NOT_ID)
    }
}