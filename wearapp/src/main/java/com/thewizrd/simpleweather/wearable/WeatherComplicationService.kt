package com.thewizrd.simpleweather.wearable

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationManager
import android.support.wearable.complications.ComplicationProviderService
import android.support.wearable.complications.ComplicationText
import android.util.Log
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.icons.WeatherIconsProvider
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.weatherdata.Weather
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader
import com.thewizrd.shared_resources.weatherdata.WeatherRequest
import com.thewizrd.simpleweather.LaunchActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class WeatherComplicationService : ComplicationProviderService() {
    companion object {
        private const val TAG = "WeatherComplicationService"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onComplicationActivated(complicationId: Int, type: Int, manager: ComplicationManager) {
        super.onComplicationActivated(complicationId, type, manager)

        // Request complication update
        WeatherComplicationHelper.requestComplicationUpdate(this, complicationId)
    }

    override fun onComplicationUpdate(complicationId: Int, type: Int, manager: ComplicationManager) {
        if (type != ComplicationData.TYPE_SHORT_TEXT && type != ComplicationData.TYPE_LONG_TEXT) {
            manager.noUpdateRequired(complicationId)
            Logger.writeLine(Log.DEBUG, "%s: Complication %d no update required", TAG, complicationId)
            return
        }

        scope.launch {
            var complicationData: ComplicationData? = null

            if (Settings.isWeatherLoaded()) {
                val weather = withContext(Dispatchers.IO) {
                    try {
                        WeatherDataLoader(Settings.getHomeData())
                                .loadWeatherData(WeatherRequest.Builder()
                                        .forceLoadSavedData()
                                        .build()
                                ).await()
                    } catch (e: Exception) {
                        null
                    }
                }

                complicationData = buildUpdate(type, weather)
            }

            if (complicationData != null) {
                manager.updateComplicationData(complicationId, complicationData)
                Logger.writeLine(Log.DEBUG, "%s: Complication %d updated", TAG, complicationId)
            } else {
                // If no data is sent, we still need to inform the ComplicationManager, so
                // the update job can finish and the wake lock isn't held any longer.
                manager.noUpdateRequired(complicationId)
                Logger.writeLine(Log.DEBUG, "%s: Complication %d no update required", TAG, complicationId)
            }
        }
    }

    private fun getTapIntent(context: Context): PendingIntent {
        val onClickIntent = Intent(context.applicationContext, LaunchActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return PendingIntent.getActivity(context, 0, onClickIntent, 0)
    }

    private fun buildUpdate(dataType: Int, weather: Weather?): ComplicationData? {
        if (weather == null || !weather.isValid || dataType != ComplicationData.TYPE_SHORT_TEXT && dataType != ComplicationData.TYPE_LONG_TEXT) {
            return null
        }

        val isFahrenheit = Units.FAHRENHEIT == Settings.getTemperatureUnit()

        // Temperature
        val currTemp = if (weather.condition.tempF != null && weather.condition.tempF != weather.condition.tempC) {
            val temp = if (isFahrenheit) Math.round(weather.condition.tempF) else Math.round(weather.condition.tempC)
            String.format(LocaleUtils.getLocale(), "%d", temp)
        } else {
            WeatherIcons.PLACEHOLDER
        }

        val tempUnit = if (isFahrenheit) Units.FAHRENHEIT else Units.CELSIUS

        val temp = String.format(LocaleUtils.getLocale(), "%s°%s", currTemp, tempUnit)

        // Condition text
        val condition = weather.condition.weather

        val builder = ComplicationData.Builder(dataType)

        if (dataType == ComplicationData.TYPE_SHORT_TEXT) {
            builder.setShortText(ComplicationText.plainText(temp))

            // Weather Icon
            val wip = WeatherIconsManager.getProvider(WeatherIconsProvider.KEY)
            val weatherIcon = wip.getWeatherIconResource(weather.condition.icon)
            builder.setIcon(Icon.createWithResource(applicationContext, weatherIcon))
        } else if (dataType == ComplicationData.TYPE_LONG_TEXT) {
            builder.setLongText(ComplicationText.plainText("$temp - $condition"))

            // Weather Icon
            val weatherIcon = WeatherIconsManager.getInstance().getWeatherIconResource(weather.condition.icon)
            builder.setImageStyle(ComplicationData.IMAGE_STYLE_ICON)
                    .setSmallImage(Icon.createWithBitmap(
                            ImageUtils.bitmapFromDrawable(ContextUtils.getThemeContextOverride(applicationContext, false), weatherIcon))
                    )
        }

        builder.setTapAction(getTapIntent(this))

        return builder.build()
    }
}