package com.thewizrd.simpleweather.wearable.tiles

import android.content.Context
import android.util.Log
import androidx.wear.tiles.TileService
import com.thewizrd.shared_resources.utils.Logger

object WeatherTileHelper {
    private const val TAG = "WeatherTileHelper"

    @JvmStatic
    fun requestTileUpdateAll(context: Context) {
        Logger.writeLine(Log.INFO, "%s: requesting tile update all", TAG)

        TileService.getUpdater(
            context.applicationContext
        )
            .requestUpdate(ForecastWeatherTileProviderService::class.java)

        TileService.getUpdater(
            context.applicationContext
        )
            .requestUpdate(CurrentWeatherTileProviderService::class.java)

        TileService.getUpdater(
            context.applicationContext
        )
            .requestUpdate(CurrentWeatherGoogleTileProviderService::class.java)
    }
}