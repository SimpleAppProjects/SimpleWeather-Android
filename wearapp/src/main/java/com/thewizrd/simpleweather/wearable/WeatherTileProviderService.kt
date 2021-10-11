package com.thewizrd.simpleweather.wearable

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.google.android.clockwork.tiles.TileData
import com.google.android.clockwork.tiles.TileProviderService
import com.thewizrd.shared_resources.helpers.toImmutableCompatFlag
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader
import com.thewizrd.shared_resources.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.isNullOrInvalid
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.LaunchActivity
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.concurrent.TimeUnit

abstract class WeatherTileProviderService : TileProviderService() {
    protected abstract val LOGTAG: String

    protected var id = -1
        private set

    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    protected val settingsMgr = App.instance.settingsManager

    override fun onDestroy() {
        Timber.tag(LOGTAG).d("destroying service...")
        super.onDestroy()
        scope.cancel()
    }

    override fun onUnbind(intent: Intent): Boolean {
        val result = super.onUnbind(intent)
        Timber.tag(LOGTAG).d("Service unbound")
        return result
    }

    override fun onTileUpdate(tileId: Int) {
        Timber.tag(LOGTAG).d("onTileUpdate called with: tileId = %s", tileId)

        if (!isIdForDummyData(tileId)) {
            id = tileId

            sendUpdate(
                id, TileData.Builder()
                    .setLoading(true)
                    .build()
            )

            scope.launch {
                sendRemoteViews()
            }

            // Enqueue work if not already
            WidgetUpdaterWorker.enqueueAction(this, WidgetUpdaterWorker.ACTION_ENQUEUEWORK)
            WeatherUpdaterWorker.enqueueAction(this, WeatherUpdaterWorker.ACTION_ENQUEUEWORK)
        }
    }

    override fun onTileFocus(tileId: Int) {
        super.onTileFocus(tileId)

        Timber.tag(LOGTAG).d("onTileFocus called with: tileId = %s", tileId)

        if (!isIdForDummyData(tileId)) {
            id = tileId
        }
    }

    override fun onTileBlur(tileId: Int) {
        super.onTileBlur(tileId)

        Timber.tag(LOGTAG).d("onTileBlur called with: tileId = %s", tileId)

        if (!isIdForDummyData(tileId)) {
            id = tileId
        }
    }

    private suspend fun sendRemoteViews() {
        Timber.tag(LOGTAG).d("sendRemoteViews")

        val weather = withContext(Dispatchers.IO) {
            try {
                val locData = settingsMgr.getHomeData() ?: return@withContext null
                WeatherDataLoader(locData)
                    .loadWeatherData(
                        WeatherRequest.Builder()
                            .forceLoadSavedData()
                            .build()
                    )
            } catch (e: Exception) {
                null
            }
        }

        if (!scope.isActive) return

        Timber.tag(LOGTAG).d("building update...")

        val updateViews = if (!weather.isNullOrInvalid()) {
            buildUpdate(weather!!)
        } else {
            null
        }

        if (updateViews != null) {
            val tileData = TileData.Builder()
                .setRemoteViews(updateViews)
                .setOutdatedTimeMs(TimeUnit.HOURS.toMillis(1))
                .build()

            Timber.tag(LOGTAG).d("sending update...")
            sendUpdate(id, tileData)
        } else {
            Timber.tag(LOGTAG).d("unable to update tile...")
        }
    }

    protected abstract suspend fun buildUpdate(weather: Weather): RemoteViews?

    protected fun getTapIntent(context: Context): PendingIntent {
        val onClickIntent = Intent(context.applicationContext, LaunchActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return PendingIntent.getActivity(context, 0, onClickIntent, 0.toImmutableCompatFlag())
    }
}