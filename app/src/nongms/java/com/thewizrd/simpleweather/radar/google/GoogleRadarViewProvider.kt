package com.thewizrd.simpleweather.radar.google

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.view.isEmpty
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.simpleweather.radar.BoundingBox
import com.thewizrd.simpleweather.radar.MapTileRadarViewProvider
import com.thewizrd.weather_api.google.utils.getGoogleAuthHeaders
import com.thewizrd.weather_api.keys.Keys
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.modules.TileWriter
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class GoogleRadarViewProvider(context: Context, rootView: ViewGroup) :
    MapTileRadarViewProvider(context, rootView) {
    private var tilesOverlay: TilesOverlay? = null

    override fun onCreateView(savedInstanceState: Bundle?) {
        super.onCreateView(savedInstanceState)
        if (viewContainer.isEmpty()) {
            viewContainer.addView(mapView)
        }
    }

    public override fun onMapReady() {
        super.onMapReady()

        val cameraPosition = getMapCameraPosition()
        if (cameraPosition != null) {
            if (interactionsEnabled()) {
                if (locationMarker == null) {
                    locationMarker = Marker(mapView)
                    locationMarker.setDefaultIcon()
                    mapView.overlays.add(locationMarker)
                }

                locationMarker.setPosition(
                    GeoPoint(
                        cameraPosition.latitude,
                        cameraPosition.longitude
                    )
                )
            }
        }

        if (tilesOverlay == null) {
            val tileProvider =
                MapTileProviderBasic(context, GoogleTileProvider(context), TileWriter())
            tilesOverlay = TilesOverlay(tileProvider, context, false, false).apply {
                loadingLineColor = Colors.TRANSPARENT
                loadingBackgroundColor = Colors.TRANSPARENT
            }
            mapView.overlays.add(tilesOverlay)
        }

        mapView.postInvalidate()
    }

    override fun configureMapView(mapView: MapView) {
        super.configureMapView(mapView)
        Configuration.getInstance().additionalHttpRequestProperties.putAll(context.getGoogleAuthHeaders())
    }

    private class GoogleTileProvider(private val context: Context) : XYTileSource(
        "OWM", 0, 12, 256, ".png",
        arrayOf("https://tile.openweathermap.org/")
    ) {
        companion object {
            // Approx...
            val CONTINENTAL_US = BoundingBox(-128.457600, 24.365871, -65.000568, 51.192260)
            val EUROPE = BoundingBox(-28.6397727617, 34.9305490565, 37.4539772383, 74.9465370831)
        }

        override fun getTileURLString(pMapTileIndex: Long): String? {
            val zoom = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)

            val key = getKey().takeUnless { it.isNullOrBlank() } ?: return null

            val bbox = BoundingBox.fromTile(x, y, zoom)

            /* Define the URL pattern for the tile images */
            return if (bbox.intersects(CONTINENTAL_US)) {
                "https://weather.googleapis.com/v1/mapTypes/US_PRECIPITATION_CURRENT/mapTiles/${zoom}/${x}/${y}?key=${key}"
            } else if (bbox.intersects(EUROPE)) {
                "https://weather.googleapis.com/v1/mapTypes/EU_PRECIPITATION_CURRENT/mapTiles/${zoom}/${x}/${y}?key=${key}"
            } else {
                null
            }
        }

        private fun getKey(): String? {
            val key = settingsManager.getAPIKey(WeatherAPI.GOOGLE)
            return if (key.isNullOrBlank()) Keys.getGWeatherKey() else key
        }
    }
}
