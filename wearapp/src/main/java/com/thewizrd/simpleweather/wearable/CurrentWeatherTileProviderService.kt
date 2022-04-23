package com.thewizrd.simpleweather.wearable

import android.widget.RemoteViews
import com.thewizrd.common.controls.WeatherNowViewModel
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.getColorFromTempF
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.R

class CurrentWeatherTileProviderService : WeatherTileProviderService() {
    companion object {
        private const val TAG = "CurrentWeatherTileProviderService"
    }

    override val LOGTAG = TAG

    override suspend fun buildUpdate(weather: Weather): RemoteViews {
        val wim = sharedDeps.weatherIconsManager
        val updateViews = RemoteViews(packageName, R.layout.tile_layout_currentweather)
        val viewModel = WeatherNowViewModel(weather)
        val mDarkIconCtx = getThemeContextOverride(false)

        updateViews.setOnClickPendingIntent(R.id.tile, getTapIntent(applicationContext))

        updateViews.setTextViewText(R.id.location_name, viewModel.location)

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
            weather.condition?.tempF?.let { getColorFromTempF(it, Colors.WHITE) } ?: Colors.WHITE
        )
        updateViews.setTextViewText(R.id.condition_hi, viewModel.hiTemp ?: WeatherIcons.PLACEHOLDER)
        updateViews.setTextViewText(R.id.condition_lo, viewModel.loTemp ?: WeatherIcons.PLACEHOLDER)

        updateViews.setTextViewText(R.id.condition_weather, viewModel.curCondition)

        return updateViews
    }
}