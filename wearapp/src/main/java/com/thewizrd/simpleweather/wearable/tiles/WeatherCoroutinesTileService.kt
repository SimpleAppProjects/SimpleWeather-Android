package com.thewizrd.simpleweather.wearable.tiles

import android.graphics.Bitmap
import android.os.Build
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ResourceBuilders.ImageResource
import androidx.wear.protolayout.ResourceBuilders.InlineImageResource
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.TimelineBuilders.Timeline
import androidx.wear.protolayout.TimelineBuilders.TimelineEntry
import androidx.wear.tiles.EventBuilders
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import com.thewizrd.common.utils.ImageUtils
import com.thewizrd.common.weatherdata.WeatherDataLoader
import com.thewizrd.common.weatherdata.WeatherRequest
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.icons.WeatherIconProvider
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.ContextUtils.getThemeContextOverride
import com.thewizrd.shared_resources.wearable.WearableDataSync
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.simpleweather.LaunchActivity
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream

internal const val ID_WEATHER_ICON_PREFIX = "weather_icon:"
internal const val ID_FORECAST_ICON_PREFIX = "forecast:"
internal const val ID_HR_FORECAST_ICON_PREFIX = "hrforecast:"

@OptIn(ExperimentalHorologistApi::class)
abstract class WeatherCoroutinesTileService : SuspendingTileService() {
    open val freshnessIntervalMillis: Long = 0L

    protected val resources = mutableSetOf<String>()

    override fun onTileAddEvent(requestParams: EventBuilders.TileAddEvent) {
        super.onTileAddEvent(requestParams)
        Timber.tag(this::class.java.name).d("onTileAddEvent")

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
        Timber.tag(this::class.java.name).d("tileRequest")

        Timber.tag(this::class.java.name).d("getWeather")
        val weather = getWeather()

        Timber.tag(this::class.java.name).d("renderTile")
        val rootLayout = renderTile(weather, requestParams)

        val singleTileTimeline = Timeline.Builder()
            .addTimelineEntry(
                TimelineEntry.Builder()
                    .setLayout(
                        LayoutElementBuilders.Layout.Builder()
                            .setRoot(
                                LayoutElementBuilders.Box.Builder()
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

        val wipKey = (sharedDeps.weatherIconsManager.iconProvider as WeatherIconProvider).key
        Timber.tag(this::class.java.name).d("buildTile: v($wipKey)")

        return Tile.Builder()
            .setResourcesVersion(wipKey)
            .setTileTimeline(singleTileTimeline)
            .setFreshnessIntervalMillis(freshnessIntervalMillis)
            .build()
    }

    abstract fun renderTile(
        weather: Weather?,
        requestParams: TileRequest
    ): LayoutElementBuilders.LayoutElement

    final override suspend fun resourcesRequest(requestParams: ResourcesRequest): Resources {
        Timber.tag(this::class.java.name).d("resourcesRequest: v(${requestParams.version})")

        return Resources.Builder()
            .setVersion(requestParams.version)
            .apply {
                produceRequestedResources(
                    requestParams.version,
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
        wipKey: String,
        deviceParameters: DeviceParameters,
        resourceIds: List<String>
    ) {
        Timber.tag(this::class.java.name).d("produceRequestedResources")
        val resources = resourceIds.takeIf { it.isNotEmpty() } ?: resources

        if (resources.isNotEmpty()) {
            resources.forEach { id ->
                if (id.startsWith(ID_WEATHER_ICON_PREFIX)) {
                    val icon = id.removePrefix(ID_WEATHER_ICON_PREFIX)

                    this.addIdToImageMapping(
                        id,
                        createImageResourceFromWeatherIcon(wipKey, icon)
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

    protected open fun getLaunchAction(): ActionBuilders.Action {
        return ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(this.packageName)
                    .setClassName(LaunchActivity::class.java.name)
                    .build()
            )
            .build()
    }

    private fun createImageResourceFromWeatherIcon(wipKey: String, icon: String): ImageResource {
        val wim = sharedDeps.weatherIconsManager
        return createImageResourceFromWeatherIcon(wim.getIconProvider(wipKey), icon)
    }

    private fun createImageResourceFromWeatherIcon(icon: String): ImageResource {
        val wim = sharedDeps.weatherIconsManager
        return createImageResourceFromWeatherIcon(wim.iconProvider as WeatherIconProvider, icon)
    }

    private fun createImageResourceFromWeatherIcon(
        iconProvider: WeatherIconProvider,
        icon: String
    ): ImageResource {
        val darkIconCtx = applicationContext.getThemeContextOverride(false)

        return ImageUtils.bitmapFromDrawable(
            darkIconCtx,
            iconProvider.getWeatherIconResource(icon)
        ).run {
            val buffer = ByteArrayOutputStream().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, this)
                } else {
                    compress(Bitmap.CompressFormat.PNG, 100, this)
                }
            }.toByteArray()

            ImageResource.Builder()
                .setInlineResource(
                    InlineImageResource.Builder()
                        .setData(buffer)
                        .setWidthPx(width)
                        .setHeightPx(height)
                        .build()
                )
                .build()
        }
    }
}