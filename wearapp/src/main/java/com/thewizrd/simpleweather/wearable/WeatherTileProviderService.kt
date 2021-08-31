package com.thewizrd.simpleweather.wearable

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews
import com.google.android.clockwork.tiles.TileData
import com.google.android.clockwork.tiles.TileProviderService
import com.thewizrd.shared_resources.controls.*
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.helpers.toImmutableCompatFlag
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.utils.ImageUtils
import com.thewizrd.shared_resources.utils.StringUtils
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.LaunchActivity
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker
import kotlinx.coroutines.*
import timber.log.Timber
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

class WeatherTileProviderService : TileProviderService() {
    companion object {
        private const val TAG = "WeatherTileProviderService"
        private const val FORECAST_LENGTH = 4
    }

    private var id = -1

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val settingsMgr = App.instance.settingsManager
    private val wim = WeatherIconsManager.getInstance()

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
                            .loadWeatherData(WeatherRequest.Builder()
                                    .forceLoadSavedData()
                                    .build())
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

    private suspend fun buildUpdate(weather: Weather?): RemoteViews? {
        if (weather == null || !weather.isValid) {
            return null
        }

        val wim = WeatherIconsManager.getInstance()
        val updateViews = RemoteViews(packageName, R.layout.tile_layout_weather)
        val viewModel = WeatherNowViewModel(weather)
        val mDarkIconCtx = ContextUtils.getThemeContextOverride(applicationContext, false)

        updateViews.setOnClickPendingIntent(R.id.tile, getTapIntent(applicationContext))

        updateViews.setTextViewText(R.id.condition_temp, viewModel.curTemp)
        updateViews.setImageViewBitmap(R.id.weather_icon, ImageUtils.bitmapFromDrawable(mDarkIconCtx,
                wim.getWeatherIconResource(viewModel.weatherIcon)))
        updateViews.setTextViewText(R.id.weather_condition, viewModel.curCondition)

        // Details
        var chanceModel: DetailItemViewModel? = null
        var windModel: DetailItemViewModel? = null

        for (input in viewModel.getWeatherDetails()) {
            if (input.detailsType == WeatherDetailsType.POPCHANCE || input.detailsType == WeatherDetailsType.POPCLOUDINESS) {
                chanceModel = input
            } else if (input.detailsType == WeatherDetailsType.WINDSPEED) {
                windModel = input
            }

            if (chanceModel != null && windModel != null) {
                break
            }
        }

        if (chanceModel != null) {
            updateViews.setImageViewResource(
                R.id.weather_popicon,
                wim.getWeatherIconResource(chanceModel.icon)
            )
            updateViews.setTextViewText(R.id.weather_pop, chanceModel.value)
            updateViews.setViewVisibility(R.id.weather_pop_layout, View.VISIBLE)
        } else {
            updateViews.setViewVisibility(R.id.weather_pop_layout, View.GONE)
        }

        if (windModel != null) {
            if (windModel.iconRotation != 0) {
                updateViews.setImageViewBitmap(R.id.weather_windicon,
                    ImageUtils.rotateBitmap(
                        ImageUtils.bitmapFromDrawable(
                            this,
                            wim.getWeatherIconResource(windModel.icon)
                        ), windModel.iconRotation.toFloat()
                    )
                )
            } else {
                updateViews.setImageViewResource(
                    R.id.weather_windicon,
                    wim.getWeatherIconResource(windModel.icon)
                )
            }
            var speed = if (TextUtils.isEmpty(windModel.value)) "" else windModel.value.toString()
            speed = speed.split(",").toTypedArray()[0]
            updateViews.setTextViewText(R.id.weather_windspeed, speed)
            updateViews.setViewVisibility(R.id.weather_wind_layout, View.VISIBLE)
        } else {
            updateViews.setViewVisibility(R.id.weather_wind_layout, View.GONE)
        }

        updateViews.setViewVisibility(R.id.extra_layout, if (chanceModel != null || windModel != null) View.VISIBLE else View.GONE)

        // Build forecast
        updateViews.removeAllViews(R.id.forecast_layout)

        val forecastPanel = RemoteViews(packageName, R.layout.tile_forecast_layout_container)
        var hrForecastPanel: RemoteViews? = null

        val forecasts = getForecasts()
        val hrforecasts = getHourlyForecasts()

        if (hrforecasts.isNotEmpty()) {
            hrForecastPanel = RemoteViews(packageName, R.layout.tile_forecast_layout_container)
        }

        for (i in 0 until Math.min(FORECAST_LENGTH, forecasts.size)) {
            val forecast = forecasts[i]
            addForecastItem(mDarkIconCtx, forecastPanel, forecast)
            if (hrForecastPanel != null && i < hrforecasts.size) {
                addForecastItem(mDarkIconCtx, hrForecastPanel, hrforecasts[i])
            }
        }

        updateViews.setViewVisibility(R.id.forecast_layout, if (Math.min(forecasts.size, hrforecasts.size) <= 0) View.GONE else View.VISIBLE)

        updateViews.addView(R.id.forecast_layout, forecastPanel)
        if (hrForecastPanel != null) {
            updateViews.addView(R.id.forecast_layout, hrForecastPanel)
        }

        return updateViews
    }

    private suspend fun getForecasts(): List<ForecastItemViewModel> {
        val locationData = settingsMgr.getHomeData()

        if (locationData?.isValid == true) {
            val forecasts = settingsMgr.getWeatherForecastData(locationData.query)

            if (forecasts?.forecast?.isNotEmpty() == true) {
                val size = Math.min(FORECAST_LENGTH, forecasts.forecast.size)
                val fcasts = ArrayList<ForecastItemViewModel>(size)

                for (i in 0 until size) {
                    fcasts.add(ForecastItemViewModel(forecasts.forecast[i]))
                }

                return fcasts
            }
        }

        return emptyList()
    }

    private suspend fun getHourlyForecasts(): List<HourlyForecastItemViewModel> {
        val locationData = settingsMgr.getHomeData()

        if (locationData?.isValid == true) {
            val now = ZonedDateTime.now().withZoneSameInstant(locationData.tzOffset)

            val hrInterval = WeatherManager.instance.getHourlyForecastInterval()
            val forecasts = settingsMgr.getHourlyForecastsByQueryOrderByDateByLimitFilterByDate(
                locationData.query,
                FORECAST_LENGTH,
                now.minusHours((hrInterval * 0.5).toLong()).truncatedTo(ChronoUnit.HOURS)
            )

            if (!forecasts.isNullOrEmpty()) {
                return ArrayList<HourlyForecastItemViewModel>(FORECAST_LENGTH).apply {
                    var count = 0
                    for (fcast in forecasts) {
                        add(HourlyForecastItemViewModel(fcast))
                        count++

                        if (count >= FORECAST_LENGTH) break
                    }
                }
            }
        }

        return emptyList()
    }

    private fun addForecastItem(context: Context, forecastPanel: RemoteViews, forecast: BaseForecastItemViewModel) {
        val forecastItem = RemoteViews(context.packageName, R.layout.tile_forecast_panel)

        if (forecast is ForecastItemViewModel) {
            forecastItem.setTextViewText(R.id.forecast_date, StringUtils.removeDigitChars(forecast.getShortDate()))
        } else {
            forecastItem.setTextViewText(R.id.forecast_date, forecast.shortDate)
        }
        forecastItem.setTextViewText(R.id.forecast_hi, forecast.hiTemp)
        if (forecast is ForecastItemViewModel) {
            forecastItem.setTextViewText(R.id.forecast_lo, forecast.loTemp)
        }

        forecastItem.setImageViewBitmap(
            R.id.forecast_icon,
            ImageUtils.bitmapFromDrawable(context, wim.getWeatherIconResource(forecast.weatherIcon))
        )

        if (forecast is HourlyForecastItemViewModel) {
            forecastItem.setViewVisibility(R.id.forecast_lo, View.GONE)
        }

        forecastPanel.addView(R.id.forecast_container, forecastItem)
    }

    private fun getTapIntent(context: Context): PendingIntent {
        val onClickIntent = Intent(context.applicationContext, LaunchActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return PendingIntent.getActivity(context, 0, onClickIntent, 0.toImmutableCompatFlag())
    }
}