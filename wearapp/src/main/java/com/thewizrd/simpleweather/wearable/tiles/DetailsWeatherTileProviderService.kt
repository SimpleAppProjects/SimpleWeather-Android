package com.thewizrd.simpleweather.wearable.tiles

import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.tiles.RequestBuilders
import com.thewizrd.common.controls.DetailItemViewModel
import com.thewizrd.common.controls.WeatherDetailsType
import com.thewizrd.common.controls.toUiModel
import com.thewizrd.shared_resources.icons.WeatherIcons
import com.thewizrd.shared_resources.utils.NumberUtils.tryParseInt
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.wearable.tiles.layouts.ID_DETAIL_ICON_PREFIX
import com.thewizrd.simpleweather.wearable.tiles.layouts.ID_ROTATION_PREFIX
import com.thewizrd.simpleweather.wearable.tiles.layouts.detailsWeatherTileLayout
import com.thewizrd.simpleweather.wearable.tiles.layouts.supportsTransformation

class DetailsWeatherTileProviderService : WeatherCoroutinesTileService() {
    override fun renderTile(
        weather: Weather?,
        requestParams: RequestBuilders.TileRequest
    ): LayoutElementBuilders.LayoutElement {
        val viewModel = weather?.toUiModel()
        val details = viewModel?.weatherDetailsMap

        if (details != null) {
            WeatherDetailsType.entries.forEach {
                details.putIfAbsent(it, DetailItemViewModel(it, WeatherIcons.EM_DASH))
            }
        }

        resources.clear()

        details?.forEach { (_, model) ->
            if (!requestParams.deviceConfiguration.supportsTransformation() && model.iconRotation != 0) {
                resources.add("${ID_ROTATION_PREFIX}${model.iconRotation}:${ID_WEATHER_ICON_PREFIX}${model.icon}")
            } else {
                resources.add("${ID_WEATHER_ICON_PREFIX}${model.icon}")
            }
        }

        return detailsWeatherTileLayout(weather, details, this, requestParams.deviceConfiguration)
    }

    override fun ResourceBuilders.Resources.Builder.produceRequestedResource(
        deviceParameters: DeviceParametersBuilders.DeviceParameters,
        id: String
    ) {
        var resId = id
        var rotation = 0

        if (resId.startsWith(ID_ROTATION_PREFIX)) {
            resId = resId.removePrefix(ID_ROTATION_PREFIX)
            rotation = resId.split(':').firstOrNull().tryParseInt(0)
            resId = resId.substring(resId.indexOfFirst { it == ':' } + 1)
        }

        if (resId.startsWith(ID_DETAIL_ICON_PREFIX)) {
            val detailsTypeName = resId.removePrefix(ID_DETAIL_ICON_PREFIX)

            when (detailsTypeName) {
                WeatherDetailsType.SUNRISE.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.SUNRISE, rotation)
                    )
                }

                WeatherDetailsType.SUNSET.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.SUNSET, rotation)
                    )
                }

                WeatherDetailsType.FEELSLIKE.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.THERMOMETER, rotation)
                    )
                }

                WeatherDetailsType.WINDSPEED.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.WIND_DIRECTION, rotation)
                    )
                }

                WeatherDetailsType.WINDGUST.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.CLOUDY_GUSTS, rotation)
                    )
                }

                WeatherDetailsType.HUMIDITY.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.HUMIDITY, rotation)
                    )
                }

                WeatherDetailsType.PRESSURE.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.BAROMETER, rotation)
                    )
                }

                WeatherDetailsType.VISIBILITY.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.VISIBILITY, rotation)
                    )
                }

                WeatherDetailsType.POPCLOUDINESS.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.CLOUDY, rotation)
                    )
                }

                WeatherDetailsType.POPCHANCE.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.UMBRELLA, rotation)
                    )
                }

                WeatherDetailsType.POPRAIN.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.RAINDROPS, rotation)
                    )
                }

                WeatherDetailsType.POPSNOW.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.SNOWFLAKE_COLD, rotation)
                    )
                }

                WeatherDetailsType.DEWPOINT.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.THERMOMETER, rotation)
                    )
                }

                WeatherDetailsType.MOONRISE.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.MOONRISE, rotation)
                    )
                }

                WeatherDetailsType.MOONSET.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.MOONSET, rotation)
                    )
                }

                WeatherDetailsType.MOONPHASE.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.MOON_NEW, rotation)
                    )
                }

                WeatherDetailsType.BEAUFORT.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.WIND_BEAUFORT_0, rotation)
                    )
                }

                WeatherDetailsType.UV.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.UV_INDEX, rotation)
                    )
                }

                WeatherDetailsType.AIRQUALITY.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.AIR_QUALITY, rotation)
                    )
                }

                WeatherDetailsType.TREEPOLLEN.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.TREE_POLLEN, rotation)
                    )
                }

                WeatherDetailsType.GRASSPOLLEN.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.GRASS_POLLEN, rotation)
                    )
                }

                WeatherDetailsType.RAGWEEDPOLLEN.name -> {
                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(WeatherIcons.RAGWEED_POLLEN, rotation)
                    )
                }
            }
        }
    }
}