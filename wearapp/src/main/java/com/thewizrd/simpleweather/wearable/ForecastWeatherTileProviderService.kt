package com.thewizrd.simpleweather.wearable

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.widget.RemoteViews
import com.thewizrd.common.controls.*
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.ContextUtils.isScreenRound
import com.thewizrd.shared_resources.utils.ContextUtils.isSmallestWidth
import com.thewizrd.shared_resources.utils.StringUtils.removeDigitChars
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R
import com.thewizrd.weather_api.weatherModule
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class ForecastWeatherTileProviderService : WeatherTileProviderService() {
    companion object {
        private const val TAG = "ForecastWeatherTileProviderService"
        private const val FORECAST_LENGTH = 4
    }

    override val LOGTAG = TAG

    private val wim: WeatherIconsManager
        get() = sharedDeps.weatherIconsManager

    override suspend fun buildUpdate(weather: Weather): RemoteViews {
        val updateViews = RemoteViews(packageName, R.layout.tile_layout_weather)
        val viewModel = WeatherUiModel(weather)
        val mDarkIconCtx = applicationContext.getThemeContextOverride(false)

        updateViews.setOnClickPendingIntent(R.id.tile, getTapIntent(applicationContext))

        updateViews.setTextViewText(
            R.id.condition_temp,
            viewModel.curTemp?.replace(viewModel.tempUnit ?: "", "")?.trim()
                ?: WeatherIcons.PLACEHOLDER
        )
        updateViews.setImageViewBitmap(
            R.id.weather_icon, ImageUtils.bitmapFromDrawable(
                mDarkIconCtx,
                wim.getWeatherIconResource(viewModel.weatherIcon)
            )
        )

        // Details
        val chanceModel = viewModel.weatherDetailsMap[WeatherDetailsType.POPCHANCE]
            ?: viewModel.weatherDetailsMap[WeatherDetailsType.POPCLOUDINESS]
        val windModel = viewModel.weatherDetailsMap[WeatherDetailsType.WINDSPEED]

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
                updateViews.setImageViewBitmap(
                    R.id.weather_windicon,
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

        val showExtraLayout =
            (isScreenRound() && isSmallestWidth(210)) || (!isScreenRound() && isSmallestWidth(180))
        updateViews.setViewVisibility(
            R.id.extra_layout,
            if (showExtraLayout && (chanceModel != null || windModel != null)) View.VISIBLE else View.GONE
        )

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

        updateViews.setViewVisibility(
            R.id.forecast_layout,
            if (Math.min(forecasts.size, hrforecasts.size) <= 0) View.GONE else View.VISIBLE
        )

        updateViews.addView(R.id.forecast_layout, forecastPanel)
        if (hrForecastPanel != null) {
            updateViews.addView(R.id.forecast_layout, hrForecastPanel)
        }

        return updateViews
    }

    private suspend fun getForecasts(): List<ForecastItemViewModel> {
        val locationData = settingsManager.getHomeData()

        if (locationData?.isValid == true) {
            val forecasts = settingsManager.getWeatherForecastData(locationData.query)

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
        val locationData = settingsManager.getHomeData()

        if (locationData?.isValid == true) {
            val now = ZonedDateTime.now().withZoneSameInstant(locationData.tzOffset)

            val hrInterval = weatherModule.weatherManager.getHourlyForecastInterval()
            val forecasts = settingsManager.getHourlyForecastsByQueryOrderByDateByLimitFilterByDate(
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

    private fun addForecastItem(
        context: Context,
        forecastPanel: RemoteViews,
        forecast: BaseForecastItemViewModel
    ) {
        val forecastItem = RemoteViews(context.packageName, R.layout.tile_forecast_panel)

        if (forecast is ForecastItemViewModel) {
            forecastItem.setTextViewText(
                R.id.forecast_date,
                forecast.getShortDate().removeDigitChars()
            )
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
}