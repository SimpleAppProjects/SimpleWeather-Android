package com.thewizrd.simpleweather.radar.google

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.gms.maps.model.TileProvider
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.simpleweather.radar.CachingUrlTileProvider
import com.thewizrd.simpleweather.radar.MapTileRadarViewProvider
import com.thewizrd.weather_api.keys.Keys
import java.util.Locale
import androidx.core.view.isEmpty
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers
import com.thewizrd.simpleweather.radar.BoundingBox
import com.thewizrd.weather_api.google.utils.getGoogleAuthHeaders

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class GoogleRadarViewProvider(context: Context, rootView: ViewGroup) :
    MapTileRadarViewProvider(context, rootView) {
    private var tileProvider: TileProvider? = null

    override fun onCreateView(savedInstanceState: Bundle?) {
        super.onCreateView(savedInstanceState)
        if (viewContainer.isEmpty()) {
            viewContainer.addView(mapView)
        }
    }

    override fun updateRadarView() {
        mapView.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        super.onMapReady(googleMap)

        if (tileProvider == null) {
            tileProvider = GoogleTileProvider(context)
            googleMap.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider!!))
        }
    }

    private class GoogleTileProvider(private val context: Context) :
        CachingUrlTileProvider(context, 256, 256) {
        companion object {
            // Approx...
            val CONTINENTAL_US = BoundingBox(-128.457600, 24.365871, -65.000568, 51.192260)
            val EUROPE = BoundingBox(-28.6397727617, 34.9305490565, 37.4539772383, 74.9465370831)
        }

        private val googleAuthHeaders = Headers { context.getGoogleAuthHeaders() }

        override fun getTileUrl(x: Int, y: Int, zoom: Int): GlideUrl? {
            if (!checkTileExists(x, y, zoom)) {
                return null
            }

            val key = getKey().takeUnless { it.isNullOrBlank() } ?: return null

            val bbox = BoundingBox.fromTile(x, y, zoom)

            /* Define the URL pattern for the tile images */
            return if (bbox.intersects(CONTINENTAL_US)) {
                GlideUrl(
                    "https://weather.googleapis.com/v1/mapTypes/US_PRECIPITATION_CURRENT/mapTiles/${zoom}/${x}/${y}?key=${key}",
                    googleAuthHeaders
                )
            } else if (bbox.intersects(EUROPE)) {
                GlideUrl(
                    "https://weather.googleapis.com/v1/mapTypes/EU_PRECIPITATION_CURRENT/mapTiles/${zoom}/${x}/${y}?key=${key}",
                    googleAuthHeaders
                )
            } else {
                null
            }
        }

        /*
         * Check that the tile server supports the requested x, y and zoom.
         * Complete this stub according to the tile range you support.
         * If you support a limited range of tiles at different zoom levels, then you
         * need to define the supported x, y range at each zoom level.
         */
        fun checkTileExists(x: Int, y: Int, zoom: Int): Boolean {
            val minZoom = 0
            val maxZoom = 12

            return zoom in minZoom..maxZoom
        }

        private fun getKey(): String? {
            val key = settingsManager.getAPIKey(WeatherAPI.GOOGLE)
            return if (key.isNullOrBlank()) Keys.getGWeatherKey() else key
        }
    }
}
