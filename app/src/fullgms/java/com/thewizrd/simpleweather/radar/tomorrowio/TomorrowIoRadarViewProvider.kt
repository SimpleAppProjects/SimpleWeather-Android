package com.thewizrd.simpleweather.radar.tomorrowio

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.material.slider.Slider
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.utils.Coordinate
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.simpleweather.databinding.RadarAnimateContainerBinding
import com.thewizrd.simpleweather.extras.isRadarInteractionEnabled
import com.thewizrd.simpleweather.radar.CachingUrlTileProvider
import com.thewizrd.simpleweather.radar.MapTileRadarViewProvider
import com.thewizrd.weather_api.keys.Keys
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@RequiresApi(value = Build.VERSION_CODES.LOLLIPOP)
class TomorrowIoRadarViewProvider(context: Context, rootView: ViewGroup) :
    MapTileRadarViewProvider(context, rootView) {
    private val availableRadarFrames: MutableList<RadarFrame>
    private val radarLayers: MutableMap<String, TileOverlay>

    private var googleMap: GoogleMap? = null
    private var radarContainerBinding: RadarAnimateContainerBinding? = null

    private var animationPosition = 0
    private val mMainHandler: Handler
    private var mProcessingFrames: Boolean = false

    init {
        availableRadarFrames = ArrayList()
        radarLayers = HashMap()
        mMainHandler = Handler(Looper.getMainLooper())
    }

    override fun onCreateView(savedInstanceState: Bundle?) {
        super.onCreateView(savedInstanceState)

        radarContainerBinding = RadarAnimateContainerBinding.inflate(LayoutInflater.from(context))
        viewContainer.addView(radarContainerBinding!!.root)

        radarContainerBinding!!.playButton.addOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                mMainHandler.post(animationRunnable)
            } else {
                mMainHandler.removeCallbacks(animationRunnable)
            }
        }

        radarContainerBinding!!.animationSeekbar.addOnChangeListener(Slider.OnChangeListener { _, value, fromUser ->
            if (fromUser) {
                mMainHandler.removeCallbacks(animationRunnable)
                showFrame(value.toInt())
            }
        })

        radarContainerBinding!!.animationSeekbar.value = 0f
        if (radarContainerBinding!!.radarContainer.childCount == 0) {
            radarContainerBinding!!.radarContainer.addView(mapView)
        }
    }

    override fun onPause() {
        super.onPause()
        // Remove animation callbacks
        radarContainerBinding?.playButton?.isChecked = false
    }

    override fun onViewCreated(coordinates: Coordinate) {
        super.onViewCreated(coordinates)
    }

    override fun updateRadarView() {
        radarContainerBinding!!.radarToolbar.visibility =
            if (interactionsEnabled() && isRadarInteractionEnabled()) View.VISIBLE else View.GONE
        mapView.getMapAsync(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        this.googleMap = null
        availableRadarFrames.clear()
        radarLayers.clear()
        radarContainerBinding = null
    }

    override fun onMapReady(googleMap: GoogleMap) {
        super.onMapReady(googleMap)
        this.googleMap = googleMap

        getRadarFrames()
    }

    private fun getRadarFrames() {
        mProcessingFrames = true

        // Remove already added tile overlays
        val overlaysToDelete = radarLayers.values.toList()
        radarLayers.clear()
        for (overlay in overlaysToDelete) {
            mMainHandler.post { overlay.remove() }
        }

        availableRadarFrames.clear()
        animationPosition = 0

        var now = Instant.now().truncatedTo(ChronoUnit.MINUTES) // 2024-05-05T14:52:00.000Z
        val minute = (now.epochSecond - now.truncatedTo(ChronoUnit.HOURS).epochSecond) / 60L // 52
        // Trim minute
        now = now.minus(minute % 10, ChronoUnit.MINUTES)

        val start = now.minus(2, ChronoUnit.HOURS)
        val end = now.plus(2, ChronoUnit.HOURS)

        var current = start
        var nowIndex = -1

        while (current <= end) {
            availableRadarFrames.add(RadarFrame(DateTimeFormatter.ISO_INSTANT.format(current)))

            if (current == now) {
                nowIndex = availableRadarFrames.size - 1
            }

            current = current.plus(10, ChronoUnit.MINUTES)
        }

        mProcessingFrames = false

        mMainHandler.post {
            if (isViewAlive) {
                val lastPastFramePosition = nowIndex
                showFrame(lastPastFramePosition)
            }
        }
    }

    private fun addLayer(mapFrame: RadarFrame) {
        if (mProcessingFrames) return

        if (!radarLayers.containsKey(mapFrame.timestamp)) {
            val overlay = googleMap!!.addTileOverlay(
                TileOverlayOptions().tileProvider(TomorrowIoTileProvider(context, mapFrame))
                    .transparency(1f)
            )
            if (overlay != null) {
                radarLayers[mapFrame.timestamp] = overlay
            }
        }

        radarContainerBinding!!.animationSeekbar.stepSize = 1f
        radarContainerBinding!!.animationSeekbar.valueFrom = 0f
        radarContainerBinding!!.animationSeekbar.valueTo = (availableRadarFrames.size - 1).toFloat()
    }

    private fun changeRadarPosition(pos: Int, preloadOnly: Boolean = false) {
        if (mProcessingFrames) return

        var position = pos
        while (position >= availableRadarFrames.size) {
            position -= availableRadarFrames.size
        }
        while (position < 0) {
            position += availableRadarFrames.size
        }

        if (availableRadarFrames.isEmpty() || animationPosition >= availableRadarFrames.size || position >= availableRadarFrames.size) {
            return
        }

        val currentFrame = availableRadarFrames[animationPosition] ?: return
        val currentTimeStamp = currentFrame.timestamp

        val nextFrame = availableRadarFrames[position] ?: return
        val nextTimeStamp = nextFrame.timestamp

        addLayer(nextFrame)

        if (preloadOnly) {
            return
        }

        animationPosition = position

        // 0 is opaque; 1 is transparent
        if (radarLayers.containsKey(currentTimeStamp)) {
            val currentOverlay = radarLayers[currentTimeStamp]
            if (currentOverlay != null) {
                currentOverlay.transparency = 1f
            }
        }
        val nextOverlay = radarLayers[nextTimeStamp]
        if (nextOverlay != null) {
            nextOverlay.transparency = 0f
        }

        updateToolbar(position, nextFrame)
    }

    private fun updateToolbar(
        position: Int,
        mapFrame: RadarFrame = availableRadarFrames[position]
    ) {
        radarContainerBinding!!.animationSeekbar.value = position.toFloat()

        val dateTime =
            ZonedDateTime.ofInstant(Instant.parse(mapFrame.timestamp), ZoneOffset.systemDefault())
        val fmt = if (DateFormat.is24HourFormat(context)) {
            DateTimeUtils.ofPatternForUserLocale(
                DateTimeUtils.getBestPatternForSkeleton(
                    DateTimeConstants.SKELETON_DAYOFWEEK_AND_24HR
                )
            )
        } else {
            DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAYOFWEEK_AND_12HR_MIN_AMPM)
        }
        radarContainerBinding!!.timestampText.text = dateTime.format(fmt)
    }

    /**
     * Check availability and show particular frame position from the timestamps list
     */
    private fun showFrame(nextPosition: Int) {
        if (mProcessingFrames) return

        val preloadingDirection = if (nextPosition - animationPosition > 0) 1 else -1

        changeRadarPosition(nextPosition)

        // preload next next frame (typically, +1 frame)
        // if don't do that, the animation will be blinking at the first loop
        changeRadarPosition(nextPosition + preloadingDirection, true)
    }

    private val animationRunnable = object : Runnable {
        override fun run() {
            if (isViewAlive) {
                showFrame(animationPosition + 1)
                mMainHandler.postDelayed(this, 500)
            } else {
                mMainHandler.removeCallbacks(this)
            }
        }
    }

    private class TomorrowIoTileProvider(context: Context, private val mapFrame: RadarFrame?) :
        CachingUrlTileProvider(context, 256, 256) {
        override fun getTileUrl(x: Int, y: Int, zoom: Int): String? {
            if (!checkTileExists(x, y, zoom)) {
                return null
            }

            val key = getKey()

            if (mapFrame != null && !key.isNullOrBlank()) {
                /* Define the URL pattern for the tile images */
                return String.format(
                    Locale.ROOT,
                    "https://api.tomorrow.io/v4/map/tile/%d/%d/%d/precipitationIntensity/%s.png?apikey=%s",
                    zoom,
                    x,
                    y,
                    mapFrame.timestamp,
                    key
                )
            }

            return null
        }

        /*
         * Check that the tile server supports the requested x, y and zoom.
         * Complete this stub according to the tile range you support.
         * If you support a limited range of tiles at different zoom levels, then you
         * need to define the supported x, y range at each zoom level.
         */
        private fun checkTileExists(x: Int, y: Int, zoom: Int): Boolean {
            val minZoom = MIN_ZOOM_LEVEL
            val maxZoom = MAX_ZOOM_LEVEL

            return zoom in minZoom..maxZoom
        }

        private fun getKey(): String? {
            val key = settingsManager.getAPIKey(WeatherAPI.TOMORROWIO)
            return if (key.isNullOrBlank()) Keys.getTomorrowIoKey() else key
        }
    }

    private data class RadarFrame(
        val timestamp: String
    )
}