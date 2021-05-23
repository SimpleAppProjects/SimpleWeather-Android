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
import com.thewizrd.simpleweather.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit

class DailyWeatherNotificationWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    companion object {
        private const val NOT_CHANNEL_ID = "SimpleWeather.dailynotification"
        private const val TAG = "DailyWeatherNotificationWorker"

        suspend fun executeWork(context: Context) {
            NotificationHelper.executeWork(context.applicationContext)
        }

        fun scheduleNotification(context: Context) {
            Logger.writeLine(Log.INFO, "%s: Requesting to start work", TAG)

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresCharging(false)
                    .build()
            val updateRequest = OneTimeWorkRequestBuilder<DailyWeatherNotificationWorker>()
                    .setConstraints(constraints)
                    .setInitialDelay(getWorkerDelayInMinutes(context), TimeUnit.MINUTES)
                    .build()

            WorkManager.getInstance(context.applicationContext)
                    .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, updateRequest)

            Logger.writeLine(Log.INFO, "%s: One-time work enqueued", TAG)
        }

        fun cancelWork(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(TAG)
            Logger.writeLine(Log.INFO, "%s: Canceled work", TAG)
        }

        private fun getWorkerDelayInMinutes(context: Context): Long {
            /*
            // Format: HH:mm (24-hr)
            val time = SettingsManager(context.applicationContext).getDailyNotificationTime()
            val workerTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("[H:mm][HH:mm][H:m][HH:m]"))

            val now = LocalDateTime.now(Clock.systemDefaultZone())
            var workerDateTime = now.toLocalDate().atTime(workerTime)

            if (now.isAfter(workerDateTime)) {
                // Worker will execute tomorrow
                workerDateTime = workerDateTime.plusDays(1)
            }

            val delay = Duration.between(now, workerDateTime).toMinutes()
             */
            val now = System.currentTimeMillis()

            // Format: HH:mm (24-hr)
            val time = SettingsManager(context.applicationContext).getDailyNotificationTime()
            val timeSplit = time.split(":")
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, timeSplit[0].toInt())
            cal.set(Calendar.MINUTE, timeSplit[1].toInt())
            var workerTime = cal.timeInMillis

            if (now > workerTime) {
                // Worker will execute tomorrow
                workerTime += TimeUnit.DAYS.toMillis(1)
            }

            val delay = TimeUnit.MILLISECONDS.toMinutes(workerTime - now)
            Logger.writeLine(Log.DEBUG, "%s: delay = $delay min", TAG)
            return delay
        }
    }

    override suspend fun doWork(): Result {
        NotificationHelper.executeWork(applicationContext)
        return Result.success()
    }

    private object NotificationHelper {
        suspend fun executeWork(context: Context) {
            val settingsMgr = SettingsManager(context)

            if (!settingsMgr.isDailyNotificationEnabled()) return

            val now = ZonedDateTime.now(Clock.systemDefaultZone())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                initChannel(context)
            }

            // TODO: NOTE: May preload weather if its needed;
            //  but it should be handled by the other workers we have

            val location = withContext(Dispatchers.IO) { settingsMgr.getHomeData() } ?: return
            val forecasts = withContext(Dispatchers.IO) {
                settingsMgr.getWeatherForecastData(location.query)
            }

            if (forecasts != null) {
                // Get the forecast for today
                val todaysForecast = forecasts.forecast.first {
                    it.date.toLocalDate().isEqual(now.toLocalDate())
                } ?: forecasts.forecast.first()

                val notifMgr = context.getSystemService<NotificationManager>()!!
                val notif = DailyWeatherNotificationBuilder.createNotification(
                        context, NOT_CHANNEL_ID, location, todaysForecast)
                notifMgr.notify(System.currentTimeMillis().toInt(), notif)
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun initChannel(context: Context) {
            // Gets an instance of the NotificationManager service
            val mNotifyMgr = context.getSystemService(NotificationManager::class.java)
            var mChannel = mNotifyMgr.getNotificationChannel(NOT_CHANNEL_ID)
            val notChannelName = context.getString(R.string.not_channel_name_dailynotification)
            if (mChannel == null) {
                mChannel = NotificationChannel(NOT_CHANNEL_ID, notChannelName, NotificationManager.IMPORTANCE_DEFAULT)
            }

            // Configure the notification channel.
            mChannel.name = notChannelName
            mNotifyMgr.createNotificationChannel(mChannel)
        }
    }
}