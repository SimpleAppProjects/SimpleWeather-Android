package com.thewizrd.simpleweather.wearable

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.support.wearable.complications.ComplicationData
import android.support.wearable.complications.ComplicationManager
import android.support.wearable.complications.ComplicationProviderService
import android.support.wearable.complications.ComplicationText
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.helpers.toImmutableCompatFlag
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.icons.WeatherIconsProvider
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ImageUtils
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.LaunchActivity
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker
import kotlinx.coroutines.*
import timber.log.Timber

class WeatherComplicationService : ComplicationProviderService() {
    companion object {
        private const val TAG = "WeatherComplicationService"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val settingsMgr = App.instance.settingsManager

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onComplicationActivated(complicationId: Int, type: Int, manager: ComplicationManager) {
        super.onComplicationActivated(complicationId, type, manager)

        // Enqueue work if not already
        WidgetUpdaterWorker.enqueueAction(this, WidgetUpdaterWorker.ACTION_ENQUEUEWORK)
        WeatherUpdaterWorker.enqueueAction(this, WeatherUpdaterWorker.ACTION_ENQUEUEWORK)

        // Request complication update
        WeatherComplicationHelper.requestComplicationUpdate(this, complicationId)
    }

    override fun onComplicationUpdate(complicationId: Int, type: Int, manager: ComplicationManager) {
        if (type != ComplicationData.TYPE_SHORT_TEXT && type != ComplicationData.TYPE_LONG_TEXT) {
            manager.noUpdateRequired(complicationId)
            Timber.tag(TAG).d("Complication %d no update required", complicationId)
            return
        }

        scope.launch {
            var complicationData: ComplicationData? = null

            if (settingsMgr.isWeatherLoaded()) {
                val weather = withContext(Dispatchers.IO) {
                    try {
                        val locData = settingsMgr.getHomeData() ?: return@withContext null
                        WeatherDataLoader(locData)
                                .loadWeatherData(WeatherRequest.Builder()
                                        .forceLoadSavedData()
                                        .build()
                                )
                    } catch (e: Exception) {
                        null
                    }
                }

                complicationData = buildUpdate(type, weather)
            }

            if (complicationData != null) {
                manager.updateComplicationData(complicationId, complicationData)
                Timber.tag(TAG).d("Complication %d updated", complicationId)
            } else {
                // If no data is sent, we still need to inform the ComplicationManager, so
                // the update job can finish and the wake lock isn't held any longer.
                manager.noUpdateRequired(complicationId)
                Timber.tag(TAG).d("Complication %d no update required", complicationId)
            }
        }
    }

    private fun getTapIntent(context: Context): PendingIntent {
        val onClickIntent = Intent(context.applicationContext, LaunchActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return PendingIntent.getActivity(context, 0, onClickIntent, 0.toImmutableCompatFlag())
    }

    private fun buildUpdate(dataType: Int, weather: Weather?): ComplicationData? {
        if (weather == null || !weather.isValid || dataType != ComplicationData.TYPE_SHORT_TEXT && dataType != ComplicationData.TYPE_LONG_TEXT) {
            return null
        }

        val isFahrenheit = Units.FAHRENHEIT == settingsMgr.getTemperatureUnit()

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
        val provider = WeatherManager.getProvider(weather.source)
        val condition = if (provider.supportsWeatherLocale()) {
            weather.condition.weather
        } else {
            provider.getWeatherCondition(weather.condition.icon)
        }

        val builder = ComplicationData.Builder(dataType)

        val wim = WeatherIconsManager.getInstance()
        val weatherIcon = wim.getWeatherIconResource(weather.condition.icon)
        val icon = Icon.createWithBitmap(
            ImageUtils.bitmapFromDrawable(
                ContextUtils.getThemeContextOverride(this, false),
                weatherIcon
            )
        )
        if (dataType == ComplicationData.TYPE_SHORT_TEXT) {
            builder.setShortText(ComplicationText.plainText(temp))

            // Weather Icon
            builder.setIcon(icon)
            if (!wim.isFontIcon) {
                val wip = WeatherIconsManager.getProvider(WeatherIconsProvider.KEY)
                builder.setBurnInProtectionIcon(Icon.createWithBitmap(
                        ImageUtils.tintedBitmapFromDrawable(this, wip.getWeatherIconResource(weather.condition.icon), Colors.WHITE)
                ))
            }
        } else if (dataType == ComplicationData.TYPE_LONG_TEXT) {
            builder.setLongText(ComplicationText.plainText(condition))
            builder.setLongTitle(ComplicationText.plainText(temp))

            // Weather Icon
            if (wim.isFontIcon) {
                builder.setIcon(icon)
            } else {
                val wip = WeatherIconsManager.getProvider(WeatherIconsProvider.KEY)
                builder.setImageStyle(ComplicationData.IMAGE_STYLE_ICON)
                        .setSmallImage(icon)
                        .setBurnInProtectionSmallImage(Icon.createWithBitmap(
                                ImageUtils.tintedBitmapFromDrawable(this, wip.getWeatherIconResource(weather.condition.icon), Colors.WHITE)
                        ))
            }
        }

        builder.setTapAction(getTapIntent(this))

        return builder.build()
    }
}