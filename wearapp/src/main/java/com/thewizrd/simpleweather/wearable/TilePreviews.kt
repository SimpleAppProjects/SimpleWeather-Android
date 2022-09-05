package com.thewizrd.simpleweather.wearable

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.wear.tiles.DimensionBuilders.expand
import androidx.wear.tiles.LayoutElementBuilders
import androidx.wear.tiles.LayoutElementBuilders.Box
import com.google.android.horologist.compose.tools.LayoutRootPreview
import com.google.android.horologist.compose.tools.buildDeviceParameters
import com.google.android.horologist.tiles.images.toImageResource
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.model.*
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.ui.tools.WearPreviewDevices
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random

@WearPreviewDevices
@Composable
fun CurrentWeatherTilePreview() {
    val context = LocalContext.current.getThemeContextOverride(false)

    LayoutRootPreview(
        root = Box.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setHeight(expand())
            .setWidth(expand())
            .addContent(
                currentWeatherTileLayout(
                    buildMockWeatherData(context),
                    context,
                    context.deviceParams()
                )
            )
            .build()
    ) {
        val wim = sharedDeps.weatherIconsManager

        addIdToImageMapping(
            "${ID_WEATHER_ICON_PREFIX}${WeatherIcons.DAY_SUNNY}",
            ImageUtils.bitmapFromDrawable(
                context,
                wim.getWeatherIconResource(WeatherIcons.DAY_SUNNY)
            ).toImageResource()
        )
        addIdToImageMapping(
            "${ID_WEATHER_ICON_PREFIX}${WeatherIcons.NA}",
            ImageUtils.bitmapFromDrawable(
                context,
                wim.getWeatherIconResource(WeatherIcons.NA)
            ).toImageResource()
        )
    }
}

private fun Context.deviceParams() = buildDeviceParameters(resources)

private fun buildMockWeatherData(context: Context): Weather {
    return Weather().apply {
        location = Location().apply {
            name = "Location"
            tzLong = "UTC"
        }
        updateTime = ZonedDateTime.now()
        forecast = List(6) { index ->
            Forecast().apply {
                date = LocalDateTime.now().plusDays(index.toLong())
                highF = 70f + index
                highC = 23f + index / 2f
                lowF = 60f - index
                lowC = 17f - index / 2f
                condition = context.getString(R.string.weather_sunny)
                icon = WeatherIcons.DAY_SUNNY
                extras = ForecastExtras().apply {
                    feelslikeF = 80f
                    feelslikeC = 26f
                    humidity = 50
                    dewpointF = 30f
                    dewpointC = -1f
                    uvIndex = 5f
                    pop = 35
                    cloudiness = 25
                    qpfRainIn = 0.05f
                    qpfRainMm = 1.27f
                    qpfSnowIn = 0f
                    qpfSnowCm = 0f
                    pressureIn = 30.05f
                    pressureMb = 1018f
                    windDegrees = 180
                    windMph = 4f
                    windKph = 6.43f
                    windGustKph = 9f
                    windGustKph = 14.5f
                    visibilityMi = 10f
                    visibilityKm = 16.1f
                }
            }
        }
        hrForecast = List(6) { index ->
            HourlyForecast().apply {
                date = ZonedDateTime.now().plusHours(index.toLong())
                highF = 70f + index
                highC = 23f + index / 2f
                condition = context.getString(R.string.weather_sunny)
                icon = WeatherIcons.DAY_SUNNY
                windMph = 5f
                windKph = 8f
                extras = ForecastExtras().apply {
                    feelslikeF = 80f
                    feelslikeC = 26f
                    humidity = 50
                    dewpointF = 30f
                    dewpointC = -1f
                    uvIndex = 5f
                    pop = 35
                    cloudiness = 25
                    qpfRainIn = 0.05f
                    qpfRainMm = 1.27f
                    qpfSnowIn = 0f
                    qpfSnowCm = 0f
                    pressureIn = 30.05f
                    pressureMb = 1018f
                    windDegrees = 180
                    windMph = 4f
                    windKph = 6.43f
                    windGustKph = 9f
                    windGustKph = 14.5f
                    visibilityMi = 10f
                    visibilityKm = 16.1f
                }
            }
        }
        minForecast = List(10) {
            val now = ZonedDateTime.now().truncatedTo(ChronoUnit.MINUTES).let {
                it.withMinute(it.minute - (it.minute % 10))
            }

            MinutelyForecast().apply {
                date = now.plusMinutes(it.toLong() * 10)
                rainMm = Random.nextFloat()
            }
        }
        aqiForecast = List(6) {
            AirQuality().apply {
                date = LocalDate.now().plusDays(it.toLong())
                index = 10 * (it)
            }
        }
        condition = Condition().apply {
            weather = context.getString(R.string.weather_sunny)
            tempF = 70f
            tempC = 21f
            windMph = 5f
            windKph = 8f
            highF = 75f
            highC = 23f
            lowF = 60f
            lowC = 15f
            icon = WeatherIcons.DAY_SUNNY
            airQuality = AirQuality().apply {
                index = 46
            }
        }
        atmosphere = Atmosphere()
        precipitation = Precipitation().apply {
            pop = 15
            cloudiness = 25
            qpfRainIn = 0.05f
            qpfRainMm = 1.27f
            qpfSnowIn = 0f
            qpfSnowCm = 0f
        }
        source = WeatherAPI.NWS
        query = ""
    }
}