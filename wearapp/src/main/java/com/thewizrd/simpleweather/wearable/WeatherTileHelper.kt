package com.thewizrd.simpleweather.wearable

import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.google.android.clockwork.tiles.TileProviderUpdateRequester
import com.thewizrd.shared_resources.utils.Logger

object WeatherTileHelper {
    private const val TAG = "WeatherTileHelper"

    @JvmStatic
    fun requestTileUpdate(context: Context, tileId: Int) {
        Logger.writeLine(Log.INFO, "%s: requesting tile update (tileId = %s)", TAG, tileId)

        val updateRequester = TileProviderUpdateRequester(context.applicationContext,
                ComponentName(context.applicationContext, WeatherTileProviderService::class.java))
        updateRequester.requestUpdate(tileId)
    }

    @JvmStatic
    fun requestTileUpdateAll(context: Context) {
        Logger.writeLine(Log.INFO, "%s: requesting tile update all", TAG)

        val updateRequester = TileProviderUpdateRequester(context.applicationContext,
                ComponentName(context.applicationContext, WeatherTileProviderService::class.java))
        updateRequester.requestUpdateAll()
    }
}