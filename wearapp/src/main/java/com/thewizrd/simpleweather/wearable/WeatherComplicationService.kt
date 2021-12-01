package com.thewizrd.simpleweather.wearable

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.thewizrd.shared_resources.helpers.toImmutableCompatFlag
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.icons.WeatherIconsManager
import com.thewizrd.shared_resources.icons.WeatherIconsProvider
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.ImageUtils
import com.thewizrd.shared_resources.utils.LocaleUtils
import com.thewizrd.shared_resources.utils.Units
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader
import com.thewizrd.shared_resources.weatherdata.WeatherManager
import com.thewizrd.shared_resources.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.App
import com.thewizrd.simpleweather.LaunchActivity
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker
import kotlinx.coroutines.*
import timber.log.Timber

class WeatherComplicationService : SuspendingComplicationDataSourceService() {
    companion object {
        private const val TAG = "WeatherComplicationService"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val settingsMgr = App.instance.settingsManager

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onComplicationActivated(complicationInstanceId: Int, type: ComplicationType) {
        super.onComplicationActivated(complicationInstanceId, type)

        // Enqueue work if not already
        WidgetUpdaterWorker.enqueueAction(this, WidgetUpdaterWorker.ACTION_ENQUEUEWORK)
        WeatherUpdaterWorker.enqueueAction(this, WeatherUpdaterWorker.ACTION_ENQUEUEWORK)

        // Request complication update
        WeatherComplicationHelper.requestComplicationUpdate(this, complicationInstanceId)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        if (request.complicationType != ComplicationType.SHORT_TEXT && request.complicationType != ComplicationType.LONG_TEXT) {
            Timber.tag(TAG).d("Complication %d no update required", request.complicationInstanceId)
            return NoDataComplicationData()
        }

        return scope.async {
            var complicationData: ComplicationData? = null

            if (settingsMgr.isWeatherLoaded()) {
                val weather = withContext(Dispatchers.IO) {
                    try {
                        val locData = settingsMgr.getHomeData() ?: return@withContext null
                        WeatherDataLoader(locData)
                            .loadWeatherData(
                                WeatherRequest.Builder()
                                    .forceLoadSavedData()
                                    .build()
                            )
                    } catch (e: Exception) {
                        null
                    }
                }

                complicationData = buildUpdate(request.complicationType, weather)
            }

            if (complicationData != null) {
                Timber.tag(TAG).d("Complication %d updated", request.complicationInstanceId)
                return@async complicationData
            } else {
                // If no data is sent, we still need to inform the ComplicationManager, so
                // the update job can finish and the wake lock isn't held any longer.
                Timber.tag(TAG)
                    .d("Complication %d no update required", request.complicationInstanceId)
                return@async NoDataComplicationData()
            }
        }.await()
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT && type != ComplicationType.LONG_TEXT) {
            return NoDataComplicationData()
        }

        return when (type) {
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("70°").build(),
                    PlainComplicationText.Builder("70° - Sunny").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            getThemeContextOverride(false),
                            R.drawable.wi_day_sunny
                        )
                    ).build()
                ).build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder("Sunny").build(),
                    PlainComplicationText.Builder("70° - Sunny").build()
                ).setTitle(
                    PlainComplicationText.Builder("70°").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(
                        Icon.createWithResource(
                            getThemeContextOverride(false),
                            R.drawable.wi_day_sunny
                        )
                    ).build()
                ).build()
            }
            else -> {
                null
            }
        }
    }

    private fun getTapIntent(context: Context): PendingIntent {
        val onClickIntent = Intent(context.applicationContext, LaunchActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return PendingIntent.getActivity(context, 0, onClickIntent, 0.toImmutableCompatFlag())
    }

    private fun buildUpdate(dataType: ComplicationType, weather: Weather?): ComplicationData? {
        if (weather == null || !weather.isValid || dataType != ComplicationType.SHORT_TEXT && dataType != ComplicationType.LONG_TEXT) {
            return null
        }

        val isFahrenheit = Units.FAHRENHEIT == settingsMgr.getTemperatureUnit()

        // Temperature
        val currTemp =
            if (weather.condition.tempF != null && weather.condition.tempF != weather.condition.tempC) {
                val temp =
                    if (isFahrenheit) Math.round(weather.condition.tempF) else Math.round(weather.condition.tempC)
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

        val wim = WeatherIconsManager.getInstance()
        val weatherIcon = wim.getWeatherIconResource(weather.condition.icon)
        val icon = Icon.createWithBitmap(
            ImageUtils.bitmapFromDrawable(
                getThemeContextOverride(false),
                weatherIcon
            )
        )

        when (dataType) {
            ComplicationType.SHORT_TEXT -> {
                val builder = ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder(temp).build(),
                    PlainComplicationText.Builder("$temp - $condition").build()
                )

                builder.setMonochromaticImage(
                    MonochromaticImage.Builder(icon).apply {
                        // Weather Icon
                        if (!wim.isFontIcon) {
                            val wip = WeatherIconsManager.getProvider(WeatherIconsProvider.KEY)
                            setAmbientImage(
                                Icon.createWithBitmap(
                                    ImageUtils.tintedBitmapFromDrawable(
                                        this@WeatherComplicationService,
                                        wip.getWeatherIconResource(weather.condition.icon),
                                        Colors.WHITE
                                    )
                                )
                            )
                        }
                    }
                        .build()
                )

                builder.setTapAction(getTapIntent(this))
                return builder.build()
            }
            ComplicationType.LONG_TEXT -> {
                val builder = LongTextComplicationData.Builder(
                    PlainComplicationText.Builder(condition).build(),
                    PlainComplicationText.Builder("$temp - $condition").build()
                ).setTitle(
                    PlainComplicationText.Builder(temp).build()
                )

                // Weather Icon
                if (wim.isFontIcon) {
                    builder.setMonochromaticImage(
                        MonochromaticImage.Builder(icon).build()
                    )
                } else {
                    val wip = WeatherIconsManager.getProvider(WeatherIconsProvider.KEY)
                    builder.setSmallImage(
                        SmallImage.Builder(icon, SmallImageType.ICON)
                            .setAmbientImage(
                                Icon.createWithBitmap(
                                    ImageUtils.tintedBitmapFromDrawable(
                                        this,
                                        wip.getWeatherIconResource(weather.condition.icon),
                                        Colors.WHITE
                                    )
                                )
                            )
                            .build()
                    )
                }

                builder.setTapAction(getTapIntent(this))
                return builder.build()
            }
            else -> {
                return null
            }
        }
    }
}