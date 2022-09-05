package com.thewizrd.simpleweather.wearable.tiles

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.wear.tiles.TileService
import com.google.android.clockwork.tiles.TileProviderUpdateRequester
import com.thewizrd.shared_resources.utils.Logger

object WeatherTileHelper {
    private const val TAG = "WeatherTileHelper"

    @JvmStatic
    fun requestTileUpdateAll(context: Context) {
        Logger.writeLine(Log.INFO, "%s: requesting tile update all", TAG)

        TileProviderUpdateRequester(
            context.applicationContext,
            ComponentName(
                context.applicationContext,
                ForecastWeatherTileProviderService::class.java
            )
        )
            .requestUpdateAll()

        TileService.getUpdater(
            context.applicationContext
        )
            .requestUpdate(CurrentWeatherTileProviderService::class.java)

        TileProviderUpdateRequester(
            context.applicationContext,
            ComponentName(
                context.applicationContext,
                CurrentWeatherGoogleTileProviderService::class.java
            )
        )
            .requestUpdateAll()
    }
}