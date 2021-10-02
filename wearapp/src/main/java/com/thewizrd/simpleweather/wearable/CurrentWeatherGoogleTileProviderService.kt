package com.thewizrd.simpleweather.wearable

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.google.android.clockwork.tiles.TileData
import com.google.android.clockwork.tiles.TileProviderService
import com.thewizrd.shared_resources.controls.*
import com.thewizrd.shared_resources.helpers.toImmutableCompatFlag
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.ImageUtils
import com.thewizrd.shared_resources.utils.getColorFromTempF
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader
import com.thewizrd.shared_resources.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.LaunchActivity
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*

class CurrentWeatherGoogleTileProviderService : TileProviderService() {
    companion object {
        private const val TAG = "CurrentWeatherGoogleTileProviderService"
    }

    private var id = -1

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val settingsMgr = App.instance.settingsManager

    override fun onDestroy() {
        Timber.tag(TAG).d("destroying service...")
        super.onDestroy()
        scope.cancel()
    }

    override fun onUnbind(intent: Intent): Boolean {
        val result = super.onUnbind(intent)
        Timber.tag(TAG).d("Service unbound")
        return result
    }

    override fun onTileUpdate(tileId: Int) {
        Timber.tag(TAG).d("onTileUpdate called with: tileId = %s", tileId)

        if (!isIdForDummyData(tileId)) {
            id = tileId
            sendRemoteViews()

            // Enqueue work if not already
            WidgetUpdaterWorker.enqueueAction(this, WidgetUpdaterWorker.ACTION_ENQUEUEWORK)
            WeatherUpdaterWorker.enqueueAction(this, WeatherUpdaterWorker.ACTION_ENQUEUEWORK)
        }
    }

    override fun onTileFocus(tileId: Int) {
        super.onTileFocus(tileId)

        Timber.tag(TAG).d("onTileFocus called with: tileId = %s", tileId)

        if (!isIdForDummyData(tileId)) {
            id = tileId
        }
    }

    override fun onTileBlur(tileId: Int) {
        super.onTileBlur(tileId)

        Timber.tag(TAG).d("onTileBlur called with: tileId = %s", tileId)

        if (!isIdForDummyData(tileId)) {
            id = tileId
        }
    }

    private fun sendRemoteViews() {
        Timber.tag(TAG).d("sendRemoteViews")

        scope.launch {
            Timber.tag(TAG).d("building update...")

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

            val updateViews = buildUpdate(weather)

            if (updateViews != null) {
                val tileData = TileData.Builder()
                    .setRemoteViews(updateViews)
                    .build()

                Timber.tag(TAG).d("sending update...")
                sendUpdate(id, tileData)
            } else {
                Timber.tag(TAG).d("unable to update tile...")
            }
        }
    }

    private fun buildUpdate(weather: Weather?): RemoteViews? {
        if (weather == null || !weather.isValid) {
            return null
        }

        val wim = WeatherIconsManager.getInstance()
        val updateViews = RemoteViews(packageName, R.layout.tile_layout_currentweather_google)
        val viewModel = WeatherNowViewModel(weather)
        val mDarkIconCtx = getThemeContextOverride(false)

        updateViews.setOnClickPendingIntent(R.id.tile, getTapIntent(applicationContext))

        updateViews.setImageViewBitmap(
            R.id.weather_icon,
            ImageUtils.bitmapFromDrawable(
                mDarkIconCtx,
                wim.getWeatherIconResource(viewModel.weatherIcon)
            )
        )

        updateViews.setTextViewText(
            R.id.condition_temp,
            viewModel.curTemp?.replace(viewModel.tempUnit ?: "", "") ?: WeatherIcons.PLACEHOLDER
        )
        updateViews.setTextColor(
            R.id.condition_temp,
            getColorFromTempF(weather.condition.tempF, Colors.WHITE)
        )
        updateViews.setTextViewText(R.id.condition_weather, viewModel.curCondition)
        updateViews.setTextViewText(R.id.location_name, viewModel.location)

        updateViews.setTextViewText(R.id.condition_hi, viewModel.hiTemp ?: WeatherIcons.PLACEHOLDER)
        updateViews.setTextViewText(R.id.condition_lo, viewModel.loTemp ?: WeatherIcons.PLACEHOLDER)


        return updateViews
    }

    private fun getTapIntent(context: Context): PendingIntent {
        val onClickIntent = Intent(context.applicationContext, LaunchActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return PendingIntent.getActivity(context, 0, onClickIntent, 0.toImmutableCompatFlag())
    }
}