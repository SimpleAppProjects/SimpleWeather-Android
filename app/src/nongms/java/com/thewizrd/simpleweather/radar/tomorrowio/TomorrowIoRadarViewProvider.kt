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
import com.google.android.material.slider.Slider
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.di.settingsManager
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.Coordinate
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.simpleweather.databinding.RadarAnimateContainerBinding
import com.thewizrd.simpleweather.extras.isRadarInteractionEnabled
import com.thewizrd.simpleweather.radar.MapTileRadarViewProvider
import com.thewizrd.weather_api.keys.Keys
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.modules.TileWriter
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@RequiresApi(value = Build.VERSION_CODES.LOLLIPOP)
class TomorrowIoRadarViewProvider(context: Context, rootView: ViewGroup) :
    MapTileRadarViewProvider(context, rootView) {
    private val availableRadarFrames: MutableList<RadarFrame>
    private val radarLayers: MutableMap<String, TilesOverlay>

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
        super.updateRadarView()
        radarContainerBinding!!.radarToolbar.visibility =
            if (interactionsEnabled() && isRadarInteractionEnabled()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        availableRadarFrames.clear()
        radarLayers.clear()
        radarContainerBinding = null
    }

    override fun onMapReady() {
        super.onMapReady()

        mapCameraPosition?.let { cameraPosition ->
            if (interactionsEnabled()) {
                if (locationMarker == null) {
                    locationMarker = Marker(mapView)
                    locationMarker.setDefaultIcon()
                    mapView.overlays.add(locationMarker)
                }
                locationMarker.position =
                    GeoPoint(cameraPosition.latitude, cameraPosition.longitude)
            }
        }

        getRadarFrames()
    }

    private fun getRadarFrames() {
        mProcessingFrames = true

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
            val overlay = TilesOverlay(
                MapTileProviderBasic(context, TomorrowIoTileProvider(mapFrame), TileWriter()),
                context,
                false,
                false
            )
            overlay.loadingBackgroundColor = Colors.TRANSPARENT
            overlay.loadingLineColor = Colors.TRANSPARENT
            overlay.isEnabled = false
            mapView.overlays.add(overlay)
            radarLayers[mapFrame.timestamp] = overlay
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
                currentOverlay.isEnabled = false
            }
        }
        val nextOverlay = radarLayers[nextTimeStamp]
        if (nextOverlay != null) {
            nextOverlay.isEnabled = true
        }

        mapView.postInvalidate()

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

    private class TomorrowIoTileProvider(private val mapFrame: RadarFrame?) : XYTileSource(
        "TomorrowIo",
        MIN_ZOOM_LEVEL,
        MAX_ZOOM_LEVEL,
        256,
        "${mapFrame?.timestamp ?: ""}.png",
        arrayOf("https://api.tomorrow.io")
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String? {
            val zoom = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)

            val key = getKey()

            if (mapFrame != null && !key.isNullOrBlank()) {
                /* Define the URL pattern for the tile images */
                return "https://api.tomorrow.io/v4/map/tile/${zoom}/${x}/${y}/precipitationIntensity/${mapFrame.timestamp}.png?apikey=${key}"
            }

            return null
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