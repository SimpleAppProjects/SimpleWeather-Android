package com.thewizrd.simpleweather.wearable.tiles

import androidx.wear.protolayout.ActionBuilders.*
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders.*
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.tiles.EventBuilders
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import com.google.android.horologist.tiles.images.toImageResource
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.common.weatherdata.WeatherDataLoader
import com.thewizrd.common.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.LaunchActivity
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val ID_WEATHER_ICON_PREFIX = "weather_icon:"

@OptIn(ExperimentalHorologistApi::class)
abstract class WeatherCoroutinesTileService : SuspendingTileService() {
    companion object {
        /**
         * A constant for non updating resources where each id will always contain the same content.
         */
        const val PERMANENT_RESOURCES_VERSION: String = "0"
    }

    open val freshnessIntervalMillis: Long = 0L

    protected val resources = mutableListOf<String>()

    override fun onTileAddEvent(requestParams: EventBuilders.TileAddEvent) {
        super.onTileAddEvent(requestParams)

        // Enqueue work if not already
        WidgetUpdaterWorker.enqueueAction(this, WidgetUpdaterWorker.ACTION_ENQUEUEWORK)
        WeatherUpdaterWorker.enqueueAction(this, WeatherUpdaterWorker.ACTION_ENQUEUEWORK)
    }

    protected open suspend fun getWeather(): Weather? = withContext(Dispatchers.IO) {
        try {
            val locData = settingsManager.getHomeData() ?: return@withContext null
            // If saved data DNE (for current location), refresh weather
            val wLoader = WeatherDataLoader(locData)

            var weather = wLoader.loadWeatherData(
                WeatherRequest.Builder()
                    .forceLoadSavedData()
                    .build()
            )

            if (weather == null && settingsManager.getDataSync() == WearableDataSync.OFF) {
                weather = wLoader.loadWeatherData(
                    WeatherRequest.Builder()
                        .forceRefresh(false)
                        .loadAlerts()
                        .loadForecasts()
                        .build()
                )
            }

            weather
        } catch (e: Exception) {
            null
        }
    }

    final override suspend fun tileRequest(requestParams: TileRequest): Tile {
        val weather = getWeather()

        val rootLayout = renderTile(weather, requestParams.deviceConfiguration)

        val singleTileTimeline = Timeline.Builder()
            .addTimelineEntry(
                TimelineEntry.Builder()
                    .setLayout(
                        Layout.Builder()
                            .setRoot(
                                Box.Builder()
                                    .addContent(rootLayout)
                                    .setHeight(DimensionBuilders.expand())
                                    .setWidth(DimensionBuilders.expand())
                                    .setModifiers(
                                        ModifiersBuilders.Modifiers.Builder()
                                            .setClickable(
                                                Clickable.Builder()
                                                    .setOnClick(getLaunchAction())
                                                    .build()
                                            )
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        return Tile.Builder()
            .setResourcesVersion(System.currentTimeMillis().toString())
            .setTileTimeline(singleTileTimeline)
            .setFreshnessIntervalMillis(freshnessIntervalMillis)
            .build()
    }

    abstract fun renderTile(
        weather: Weather?,
        deviceParameters: DeviceParameters
    ): LayoutElement

    final override suspend fun resourcesRequest(requestParams: ResourcesRequest): Resources {
        return Resources.Builder()
            .setVersion(requestParams.version)
            .apply {
                produceRequestedResources(
                    requestParams.deviceConfiguration,
                    requestParams.resourceIds
                )
            }
            .build()
    }

    /**
     * Add resources directly to the builder.
     */
    private fun Resources.Builder.produceRequestedResources(
        deviceParameters: DeviceParameters,
        resourceIds: List<String>
    ) {
        val wim = sharedDeps.weatherIconsManager
        val darkIconCtx = applicationContext.getThemeContextOverride(false)
        val resources = resourceIds.takeIf { it.isNotEmpty() } ?: resources

        if (resources.isNotEmpty()) {
            resources.forEach { id ->
                if (id.startsWith(ID_WEATHER_ICON_PREFIX)) {
                    val icon = id.removePrefix(ID_WEATHER_ICON_PREFIX)

                    this.addIdToImageMapping(
                        id,
                        ImageUtils.bitmapFromDrawable(
                            darkIconCtx,
                            wim.getWeatherIconResource(icon)
                        ).toImageResource()
                    )
                } else {
                    produceRequestedResource(deviceParameters, id)
                }
            }
        }
    }

    protected open fun Resources.Builder.produceRequestedResource(
        deviceParameters: DeviceParameters,
        id: String
    ) {
    }

    protected open fun getLaunchAction(): Action {
        return LaunchAction.Builder()
            .setAndroidActivity(
                AndroidActivity.Builder()
                    .setPackageName(this.packageName)
                    .setClassName(LaunchActivity::class.java.name)
                    .build()
            )
            .build()
    }
}