package com.thewizrd.simpleweather.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.*
import com.thewizrd.common.controls.WeatherUiModel
import com.thewizrd.common.helpers.areNotificationsEnabled
import com.thewizrd.common.weatherdata.WeatherDataLoader
import com.thewizrd.common.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.preferences.SettingsManager
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class WeatherNotificationWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val TAG = "WeatherNotificationWorker"

        // Actions
        private const val KEY_ACTION = "action"
        private const val ACTION_REFRESHNOTIFICATION = "SimpleWeather.Droid.action.REFRESH_NOTIFICATION"
        private const val ACTION_REMOVENOTIFICATION = "SimpleWeather.Droid.action.REMOVE_NOTIFICATION"

        // Extras
        private const val EXTRA_FORCEREFRESH = "SimpleWeather.Droid.extra.FORCE_REFRESH"

        suspend fun refreshNotification(context: Context, forceRefresh: Boolean = false) {
            WeatherNotificationHelper.refreshNotification(context.applicationContext, forceRefresh)
        }

        @JvmStatic
        fun removeNotification(context: Context) {
            WeatherNotificationHelper.removeNotification(context.applicationContext)
        }

        @JvmStatic
        @JvmOverloads
        fun requestRefreshNotification(context: Context, forceRefresh: Boolean = false) {
            startWork(context.applicationContext, Intent(ACTION_REFRESHNOTIFICATION)
                    .putExtra(EXTRA_FORCEREFRESH, forceRefresh))
        }

        private fun startWork(context: Context, intent: Intent) {
            Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG)

            val updateRequest = OneTimeWorkRequest.Builder(WeatherNotificationWorker::class.java)
                .setConstraints(Constraints.NONE)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_ACTION, intent.action)
                        .putBoolean(
                            EXTRA_FORCEREFRESH,
                            intent.getBooleanExtra(EXTRA_FORCEREFRESH, false)
                        )
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(updateRequest)

            Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG)
        }
    }

    override suspend fun doWork(): Result {
        Logger.writeLine(Log.INFO, "%s: Work started", TAG)

        val intentAction = inputData.getString(KEY_ACTION)
        val forceRefresh = inputData.getBoolean(EXTRA_FORCEREFRESH, false)

        Logger.writeLine(Log.INFO, "%s: Action: %s", TAG, intentAction)

        if (ACTION_REFRESHNOTIFICATION == intentAction) {
            WeatherNotificationHelper.refreshNotification(applicationContext, forceRefresh)
        } else if (ACTION_REMOVENOTIFICATION == intentAction) {
            WeatherNotificationHelper.removeNotification(applicationContext)
        }

        return Result.success()
    }

    private object WeatherNotificationHelper {
        // Sets an ID for the notification
        private const val NOT_CHANNEL_ID = "SimpleWeather.ongoingweather"
        private const val JOB_ID = 1003
        private const val PERSISTENT_NOT_ID = JOB_ID

        suspend fun refreshNotification(context: Context, forceRefresh: Boolean) {
            Timber.tag("WeatherNotifWorker")
                .d("Refreshing notification (forceRefresh = $forceRefresh)...")

            val settingsManager = SettingsManager(context.applicationContext)

            if (settingsManager.isWeatherLoaded() && context.areNotificationsEnabled()) {
                val weather = withContext(Dispatchers.IO) {
                    val locData = settingsManager.getHomeData() ?: return@withContext null
                    val wLoader = WeatherDataLoader(locData)
                    val request = WeatherRequest.Builder()
                    if (forceRefresh)
                        request.forceRefresh(false)
                    else
                        request.forceLoadSavedData()

                    try {
                        wLoader.loadWeatherData(request.build())
                    } catch (e: Exception) {
                        null
                    }
                }

                if (settingsManager.showOngoingNotification() && weather != null) {
                    // Gets an instance of the NotificationManager service
                    val mNotifyMgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    initChannel(context, mNotifyMgr)

                    // Update notification
                    val mNotification = WeatherNotificationBuilder.updateNotification(
                        context,
                        NOT_CHANNEL_ID,
                        WeatherUiModel(weather)
                    )
                    mNotifyMgr.notify(PERSISTENT_NOT_ID, mNotification)
                } else if (!settingsManager.showOngoingNotification()) {
                    removeNotification(context)
                }
            }
        }

        fun removeNotification(context: Context) {
            val mNotifyMgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mNotifyMgr.cancel(PERSISTENT_NOT_ID)
        }

        private fun initChannel(context: Context, mNotifyMgr: NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                var mChannel = mNotifyMgr.getNotificationChannel(NOT_CHANNEL_ID)

                val notchannel_name = context.resources.getString(R.string.not_channel_name_weather)
                val notchannel_desc = context.resources.getString(R.string.not_channel_desc_weather)

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
    }
}