package com.thewizrd.simpleweather.wearable.complications

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.thewizrd.shared_resources.utils.Logger

object WeatherComplicationHelper {
    private const val TAG = "WeatherComplicationHelper"

    @JvmStatic
    fun requestComplicationUpdate(
        context: Context,
        serviceClass: Class<*>,
        complicationId: Int
    ) {
        Logger.writeLine(
            Log.INFO,
            "%s: requesting complication update (complicationId = %s)",
            TAG,
            complicationId
        )

        ComplicationDataSourceUpdateRequester.create(
            context.applicationContext,
            ComponentName(context.applicationContext, serviceClass)
        ).run {
            requestUpdate(complicationId)
        }
    }

    @JvmStatic
    fun requestComplicationUpdateAll(context: Context) {
        Logger.writeLine(Log.INFO, "%s: requesting complication update all", TAG)

        val complicationServices = setOf(
            WeatherComplicationService::class.java,
            WeatherHiLoComplicationService::class.java,
            CurrentLocationWeatherComplicationService::class.java,
            PrecipitationComplicationService::class.java,
            UVComplicationService::class.java,
            AQIComplicationService::class.java,
            BeaufortComplicationService::class.java,
            HumidityComplicationService::class.java,
            WindComplicationService::class.java,
            FeelsLikeComplicationService::class.java,
            DewPointComplicationService::class.java
        )

        complicationServices.forEach {
            ComplicationDataSourceUpdateRequester.create(
                context.applicationContext,
                ComponentName(context.applicationContext, it)
            ).run {
                requestUpdateAll()
            }
        }
    }
}