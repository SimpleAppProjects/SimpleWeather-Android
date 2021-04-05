package com.thewizrd.simpleweather.shortcuts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.*
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.ImageUtils
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.shared_resources.weatherdata.LocationType
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.N_MR1)
class ShortcutCreatorWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    private val mContext = context.applicationContext

    companion object {
        private const val TAG = "ShortcutCreatorWorker"

        @RequiresApi(Build.VERSION_CODES.N_MR1)
        suspend fun updateShortcuts(context: Context) {
            ShortcutCreatorHelper.executeWork(context.applicationContext)
        }

        @JvmStatic
        fun requestUpdateShortcuts(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                Logger.writeLine(Log.INFO, "%s: Requesting work", TAG)

                // Set a delay of 1 minute to allow the loaders to refresh the weather before this starts
                val workRequest = OneTimeWorkRequest.Builder(ShortcutCreatorWorker::class.java)
                        .setInitialDelay(1, TimeUnit.MINUTES)
                        .build()

                WorkManager.getInstance(context.applicationContext)
                        .enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, workRequest)
            }
        }

        fun removeShortcuts() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                val context = App.instance.appContext

                val shortcutMan = context.getSystemService(ShortcutManager::class.java)
                shortcutMan.removeAllDynamicShortcuts()

                Logger.writeLine(Log.INFO, "%s: Shortcuts removed", TAG)
            }
        }
    }

    override suspend fun doWork(): Result {
        ShortcutCreatorHelper.executeWork(mContext)
        return Result.success()
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private object ShortcutCreatorHelper {
        suspend fun executeWork(context: Context) {
            val wim = WeatherIconsManager.getInstance()
            val settingsManager = SettingsManager(context.applicationContext)

            val locations = ArrayList(settingsManager.getLocationData() ?: Collections.emptyList())
            if (settingsManager.useFollowGPS()) {
                locations.add(0, settingsManager.getHomeData())
            }

            var MAX_SHORTCUTS = 4
            if (locations.size < MAX_SHORTCUTS) {
                MAX_SHORTCUTS = locations.size
            }

            val shortcutMan = context.getSystemService(ShortcutManager::class.java)
            val shortcuts: MutableList<ShortcutInfo> = ArrayList(MAX_SHORTCUTS)

            shortcutMan.removeAllDynamicShortcuts()

            var i = 0
            while (i < MAX_SHORTCUTS) {
                val location = locations[i]
                val weather = settingsManager.getWeatherData(location.query)

                if (weather == null || !weather.isValid) {
                    locations.removeAt(i)
                    i--

                    if (locations.size < MAX_SHORTCUTS) {
                        MAX_SHORTCUTS = locations.size
                    }

                    i++
                    continue
                }

                // Start WeatherNow Activity with weather data
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_MAIN
                    putExtra(Constants.KEY_DATA, JSONParser.serializer(location, LocationData::class.java))
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY
                }

                val bmp = withContext(Dispatchers.IO) {
                    ImageUtils.adaptiveBitmapFromDrawable(context, wim.getWeatherIconResource(weather.condition.icon))
                }

                val shortCutIco = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Icon.createWithAdaptiveBitmap(bmp)
                } else {
                    Icon.createWithBitmap(bmp)
                }

                val shortcut = ShortcutInfo.Builder(context, if (location.locationType == LocationType.GPS) Constants.KEY_GPS else "${location.query}_$i").apply {
                    setShortLabel(weather.location.name)
                    setIcon(shortCutIco)
                    setIntent(intent)
                }.build()

                shortcuts.add(shortcut)

                i++
            }

            shortcutMan.dynamicShortcuts = shortcuts

            Logger.writeLine(Log.INFO, "%s: Shortcuts updated", TAG)
        }
    }
}