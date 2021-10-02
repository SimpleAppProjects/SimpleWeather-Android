package com.thewizrd.simpleweather.wearable

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.thewizrd.shared_resources.utils.Logger

object WeatherComplicationHelper {
    private const val TAG = "WeatherComplicationHelper"

    @JvmStatic
    fun requestComplicationUpdate(context: Context, complicationId: Int) {
        Logger.writeLine(
            Log.INFO,
            "%s: requesting complication update (complicationId = %s)",
            TAG,
            complicationId
        )

        val updateRequester = ComplicationDataSourceUpdateRequester.create(
            context.applicationContext,
            ComponentName(context.applicationContext, WeatherComplicationService::class.java)
        )
        updateRequester.requestUpdate(complicationId)
    }

    @JvmStatic
    fun requestComplicationUpdateAll(context: Context) {
        Logger.writeLine(Log.INFO, "%s: requesting complication update all", TAG)

        val updateRequester = ComplicationDataSourceUpdateRequester.create(
            context.applicationContext,
            ComponentName(context.applicationContext, WeatherComplicationService::class.java)
        )
        updateRequester.requestUpdateAll()
    }
}