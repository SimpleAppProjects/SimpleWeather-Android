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
import com.thewizrd.shared_resources.utils.ConversionMethods
import com.thewizrd.shared_resources.utils.getColorFromTempF
import com.thewizrd.shared_resources.weatherdata.model.Forecast
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecast
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.tools.WearPreviewDevices
import com.thewizrd.simpleweather.wearable.tiles.layouts.ForecastTileModel
import com.thewizrd.simpleweather.wearable.tiles.layouts.HourlyForecastTileModel
import com.thewizrd.simpleweather.wearable.tiles.layouts.ID_WEATHER_CHANCE_ICON
import com.thewizrd.simpleweather.wearable.tiles.layouts.ID_WEATHER_CLOUDINESS_ICON
import com.thewizrd.simpleweather.wearable.tiles.layouts.ID_WEATHER_HI_ICON
import com.thewizrd.simpleweather.wearable.tiles.layouts.ID_WEATHER_LO_ICON
import com.thewizrd.simpleweather.wearable.tiles.layouts.ID_WEATHER_WINDSPEED_ICON
import com.thewizrd.simpleweather.wearable.tiles.layouts.currentWeatherGoogleTileLayout
import com.thewizrd.simpleweather.wearable.tiles.layouts.currentWeatherTileLayout
import com.thewizrd.simpleweather.wearable.tiles.layouts.forecastWeatherTileLayout
import com.thewizrd.simpleweather.wearable.tiles.layouts.hourlyForecastWeatherTileLayout
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.Locale

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

@WearPreviewDevices
@Composable
fun ForecastWeatherTilePreview() {
    val context = LocalContext.current
    val forecasts = MutableList(4) {
        ForecastTileModel(context, Locale.getDefault(), Forecast().apply {
            date = LocalDateTime.now().plusDays(it.toLong())
            highF = 70f
            highC = ConversionMethods.FtoC(70f)
            lowF = 65f
            lowC = ConversionMethods.FtoC(65f)
            icon = WeatherIcons.CLOUDY
        })
    }

    LayoutRootPreview(
        root = Box.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setHeight(expand())
            .setWidth(expand())
            .addContent(
                forecastWeatherTileLayout(
                    context,
                    context.deviceParams(),
                    weatherIconId = "$ID_WEATHER_ICON_PREFIX${WeatherIcons.DAY_SUNNY}",
                    currentTemperature = "70°",
                    popChance = "90%",
                    popCloudiness = "95%",
                    windSpeed = "7 mph",
                    forecasts = forecasts
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
            ID_WEATHER_CHANCE_ICON,
            drawableResToImageResource(R.drawable.wi_umbrella_white)
        )
        addIdToImageMapping(
            ID_WEATHER_CLOUDINESS_ICON,
            drawableResToImageResource(R.drawable.wi_cloudy)
        )
        addIdToImageMapping(
            ID_WEATHER_WINDSPEED_ICON,
            drawableResToImageResource(R.drawable.wi_strong_wind)
        )
        forecasts.forEachIndexed { index, item ->
            addIdToImageMapping(
                "${ID_FORECAST_ICON_PREFIX}idx=${index}:${item.icon}",
                ImageUtils.tintedBitmapFromDrawable(
                    context,
                    R.drawable.wi_cloudy,
                    Colors.WHITE
                ).toImageResource()
            )
        }
    }
}

@WearPreviewDevices
@Composable
fun HourlyForecastWeatherTilePreview() {
    val context = LocalContext.current
    val forecasts = MutableList(4) {
        HourlyForecastTileModel(context, Locale.getDefault(), HourlyForecast().apply {
            date = ZonedDateTime.now().plusHours(it.toLong())
            highF = 70f
            highC = ConversionMethods.FtoC(70f)
            icon = WeatherIcons.CLOUDY
        })
    }

    LayoutRootPreview(
        root = Box.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setHeight(expand())
            .setWidth(expand())
            .addContent(
                hourlyForecastWeatherTileLayout(
                    context,
                    context.deviceParams(),
                    weatherIconId = "$ID_WEATHER_ICON_PREFIX${WeatherIcons.DAY_SUNNY}",
                    currentTemperature = "70°",
                    popChance = "90%",
                    popCloudiness = "95%",
                    windSpeed = "7 mph",
                    forecasts = forecasts
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
            ID_WEATHER_CHANCE_ICON,
            drawableResToImageResource(R.drawable.wi_umbrella_white)
        )
        addIdToImageMapping(
            ID_WEATHER_CLOUDINESS_ICON,
            drawableResToImageResource(R.drawable.wi_cloudy)
        )
        addIdToImageMapping(
            ID_WEATHER_WINDSPEED_ICON,
            drawableResToImageResource(R.drawable.wi_strong_wind)
        )
        forecasts.forEachIndexed { index, item ->
            addIdToImageMapping(
                "${ID_FORECAST_ICON_PREFIX}idx=${index}:${item.icon}",
                ImageUtils.tintedBitmapFromDrawable(
                    context,
                    R.drawable.wi_cloudy,
                    Colors.WHITE
                ).toImageResource()
            )
        }
    }
}

private fun Context.deviceParams() = buildDeviceParameters(resources)