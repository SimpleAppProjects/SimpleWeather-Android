package com.thewizrd.simpleweather.wearable.complications

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.MonochromaticImageComplicationData
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.SmallImage
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.data.SmallImageType
import com.thewizrd.common.controls.BeaufortViewModel
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.utils.getBeaufortScale
import com.thewizrd.shared_resources.weatherdata.model.Beaufort
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.R as sharedRes

class BeaufortComplicationService : WeatherHourlyForecastComplicationService() {
    companion object {
        private const val TAG = "BeaufortComplicationService"
    }

    override val supportedComplicationTypes: Set<ComplicationType> =
        setOf(
            ComplicationType.RANGED_VALUE,
            ComplicationType.SHORT_TEXT,
            ComplicationType.LONG_TEXT,
            ComplicationType.MONOCHROMATIC_IMAGE,
            ComplicationType.SMALL_IMAGE
        )

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (!supportedComplicationTypes.contains(type)) {
            return NoDataComplicationData()
        }

        val wim = sharedDeps.weatherIconsManager
        val complicationIcon = WeatherIcons.WIND_BEAUFORT_3

        val monochromaticIcon =
            Icon.createWithResource(this, wim.getWeatherIconResource(complicationIcon))
                .setTint(Colors.WHITESMOKE)
        val icon = Icon.createWithBitmap(
            ImageUtils.bitmapFromDrawable(
                getThemeContextOverride(false),
                wim.getWeatherIconResource(complicationIcon)
            )
        )

        return when (type) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    3f, 0f, 12f,
                    PlainComplicationText.Builder("Beaufort: 3, Gentle Breeze").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(monochromaticIcon).build()
                ).apply {
                    if (!wim.isFontIcon) {
                        setSmallImage(
                            SmallImage.Builder(icon, SmallImageType.ICON)
                                .setAmbientImage(monochromaticIcon)
                                .build()
                        )
                    }
                }.setText(
                    PlainComplicationText.Builder("3").build()
                ).setValueType(
                    RangedValueComplicationData.TYPE_RATING
                ).build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder("3").build(),
                    PlainComplicationText.Builder("Beaufort: 3, Gentle Breeze").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(monochromaticIcon).build()
                ).apply {
                    if (!wim.isFontIcon) {
                        setSmallImage(
                            SmallImage.Builder(icon, SmallImageType.ICON)
                                .setAmbientImage(monochromaticIcon)
                                .build()
                        )
                    }
                }.build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder("Beaufort").build(),
                    PlainComplicationText.Builder("Beaufort: 3, Gentle Breeze").build()
                ).setTitle(
                    PlainComplicationText.Builder("3, Gentle Breeze").build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(monochromaticIcon).build()
                ).apply {
                    if (!wim.isFontIcon) {
                        setSmallImage(
                            SmallImage.Builder(icon, SmallImageType.ICON)
                                .setAmbientImage(monochromaticIcon)
                                .build()
                        )
                    }
                }.build()
            }
            ComplicationType.MONOCHROMATIC_IMAGE -> {
                MonochromaticImageComplicationData.Builder(
                    MonochromaticImage.Builder(monochromaticIcon).build(),
                    PlainComplicationText.Builder("Beaufort: 3, Gentle Breeze").build()
                ).build()
            }
            ComplicationType.SMALL_IMAGE -> {
                SmallImageComplicationData.Builder(
                    SmallImage.Builder(icon, SmallImageType.ICON)
                        .setAmbientImage(monochromaticIcon)
                        .build(),
                    PlainComplicationText.Builder("Beaufort: 3, Gentle Breeze").build()
                ).build()
            }
            else -> {
                null
            }
        }
    }

    override fun buildUpdate(
        dataType: ComplicationType,
        weather: Weather?,
        hourlyForecast: HourlyForecast?
    ): ComplicationData? {
        if (weather == null || !weather.isValid || !supportedComplicationTypes.contains(dataType)) {
            return null
        }

        val beaufort = weather.condition!!.beaufort ?: hourlyForecast?.extras?.windMph?.let {
            Beaufort(getBeaufortScale(it))
        }
        val beaufortModel = beaufort?.let { BeaufortViewModel(it) }

        val wim = sharedDeps.weatherIconsManager

        val contentDescription = PlainComplicationText.Builder(
            beaufortModel?.let { "${beaufortModel.beaufort.label}: ${beaufortModel.progress}, ${beaufortModel.beaufort.value}" }
                ?: "${getString(sharedRes.string.label_beaufort)}: ${getString(sharedRes.string.weather_notavailable)}"
        ).build()

        val progressShortStr = beaufortModel?.progress?.toString() ?: WeatherIcons.EM_DASH
        val progressStr = beaufortModel?.let { "${it.progress}, ${it.beaufort.value}" }
            ?: WeatherIcons.EM_DASH
        val beaufortIcon = beaufortModel?.beaufort?.icon ?: WeatherIcons.WIND_BEAUFORT_0

        val monochromaticIcon =
            Icon.createWithResource(this, wim.getWeatherIconResource(beaufortIcon))
                .setTint(Colors.WHITESMOKE)
        val icon = Icon.createWithBitmap(
            ImageUtils.bitmapFromDrawable(
                getThemeContextOverride(false),
                wim.getWeatherIconResource(beaufortIcon)
            )
        )

        return when (dataType) {
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    beaufortModel?.progress?.toFloat() ?: 0f,
                    0f,
                    beaufortModel?.progressMax?.toFloat() ?: 1f,
                    contentDescription
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(monochromaticIcon).build()
                ).apply {
                    if (!wim.isFontIcon) {
                        setSmallImage(
                            SmallImage.Builder(icon, SmallImageType.ICON)
                                .setAmbientImage(monochromaticIcon)
                                .build(),
                        )
                    }
                }.setText(
                    PlainComplicationText.Builder(progressShortStr).build()
                ).setTapAction(
                    getTapIntent(this)
                ).setValueType(
                    RangedValueComplicationData.TYPE_RATING
                ).build()
            }
            ComplicationType.SHORT_TEXT -> {
                ShortTextComplicationData.Builder(
                    PlainComplicationText.Builder(progressShortStr).build(),
                    contentDescription
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(monochromaticIcon).build()
                ).apply {
                    if (!wim.isFontIcon) {
                        setSmallImage(
                            SmallImage.Builder(icon, SmallImageType.ICON)
                                .setAmbientImage(monochromaticIcon)
                                .build(),
                        )
                    }
                }.setTapAction(
                    getTapIntent(this)
                ).build()
            }
            ComplicationType.LONG_TEXT -> {
                LongTextComplicationData.Builder(
                    PlainComplicationText.Builder(getString(sharedRes.string.label_beaufort))
                        .build(),
                    contentDescription
                ).setTitle(
                    PlainComplicationText.Builder(
                        progressStr
                    ).build()
                ).setMonochromaticImage(
                    MonochromaticImage.Builder(monochromaticIcon).build()
                ).apply {
                    if (!wim.isFontIcon) {
                        setSmallImage(
                            SmallImage.Builder(icon, SmallImageType.ICON)
                                .setAmbientImage(monochromaticIcon)
                                .build(),
                        )
                    }
                }.setTapAction(
                    getTapIntent(this)
                ).build()
            }
            ComplicationType.MONOCHROMATIC_IMAGE -> {
                MonochromaticImageComplicationData.Builder(
                    MonochromaticImage.Builder(monochromaticIcon).build(),
                    contentDescription
                ).setTapAction(
                    getTapIntent(this)
                ).build()
            }
            ComplicationType.SMALL_IMAGE -> {
                SmallImageComplicationData.Builder(
                    SmallImage.Builder(icon, SmallImageType.ICON)
                        .setAmbientImage(monochromaticIcon)
                        .build(),
                    contentDescription
                ).setTapAction(
                    getTapIntent(this)
                ).build()
            }
            else -> {
                null
            }
        }
    }
}