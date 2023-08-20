package com.thewizrd.simpleweather.wearable.tiles

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import com.google.android.horologist.compose.tools.LayoutRootPreview
import com.google.android.horologist.compose.tools.buildDeviceParameters
import com.google.android.horologist.tiles.images.drawableResToImageResource
import com.google.android.horologist.tiles.images.toImageResource
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.getColorFromTempF
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.tools.WearPreviewDevices
import com.thewizrd.simpleweather.wearable.tiles.layouts.ID_WEATHER_HI_ICON
import com.thewizrd.simpleweather.wearable.tiles.layouts.ID_WEATHER_LO_ICON
import com.thewizrd.simpleweather.wearable.tiles.layouts.currentWeatherGoogleTileLayout
import com.thewizrd.simpleweather.wearable.tiles.layouts.currentWeatherTileLayout

@WearPreviewDevices
@Composable
fun CurrentWeatherTilePreview() {
    val context = LocalContext.current

    LayoutRootPreview(
        root = Box.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setHeight(expand())
            .setWidth(expand())
            .addContent(
                currentWeatherTileLayout(
                    context,
                    context.deviceParams(),
                    location = "New York, New York",
                    weatherIconId = "$ID_WEATHER_ICON_PREFIX${WeatherIcons.DAY_SUNNY}",
                    currentTemperature = "70°",
                    currentTemperatureColor = getColorFromTempF(70f),
                    lowTemperature = "60°",
                    highTemperature = "75°",
                    weatherCondition = "Sunny"
                )
            )
            .build()
    ) {
        addIdToImageMapping(
            "$ID_WEATHER_ICON_PREFIX${WeatherIcons.DAY_SUNNY}",
            ImageUtils.tintedBitmapFromDrawable(
                context,
                R.drawable.wi_day_sunny,
                Colors.WHITE
            ).toImageResource()
        )
        addIdToImageMapping(
            "$ID_WEATHER_ICON_PREFIX${WeatherIcons.NA}",
            ImageUtils.tintedBitmapFromDrawable(
                context,
                R.drawable.wi_na,
                Colors.WHITE
            ).toImageResource()
        )
    }
}

@WearPreviewDevices
@Composable
fun CurrentWeatherGoogleTilePreview() {
    val context = LocalContext.current

    LayoutRootPreview(
        root = Box.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setHeight(expand())
            .setWidth(expand())
            .addContent(
                currentWeatherGoogleTileLayout(
                    context,
                    context.deviceParams(),
                    location = "New York, New York",
                    weatherIconId = "$ID_WEATHER_ICON_PREFIX${WeatherIcons.DAY_SUNNY}",
                    currentTemperature = "70°",
                    currentTemperatureColor = getColorFromTempF(70f),
                    lowTemperature = "60°",
                    highTemperature = "75°",
                    weatherCondition = "Sunny"
                )
            )
            .build()
    ) {
        addIdToImageMapping(
            "$ID_WEATHER_ICON_PREFIX${WeatherIcons.DAY_SUNNY}",
            ImageUtils.tintedBitmapFromDrawable(
                context,
                R.drawable.wi_day_sunny,
                Colors.WHITE
            ).toImageResource()
        )
        addIdToImageMapping(
            "$ID_WEATHER_ICON_PREFIX${WeatherIcons.NA}",
            ImageUtils.tintedBitmapFromDrawable(
                context,
                R.drawable.wi_na,
                Colors.WHITE
            ).toImageResource()
        )
        addIdToImageMapping(
            ID_WEATHER_HI_ICON,
            drawableResToImageResource(R.drawable.ic_arrow_upward_24dp)
        )
        addIdToImageMapping(
            ID_WEATHER_LO_ICON,
            drawableResToImageResource(R.drawable.ic_arrow_downward_24dp)
        )
    }
}

private fun Context.deviceParams() = buildDeviceParameters(resources)