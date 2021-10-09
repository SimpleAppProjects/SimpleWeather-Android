package com.thewizrd.simpleweather.wearable

import android.content.ComponentName
import android.content.Context
import android.util.Log
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
        TileProviderUpdateRequester(
            context.applicationContext,
            ComponentName(context.applicationContext, CurrentWeatherTileProviderService::class.java)
        )
            .requestUpdateAll()
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