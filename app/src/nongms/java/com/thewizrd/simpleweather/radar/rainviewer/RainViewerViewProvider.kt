package com.thewizrd.simpleweather.radar.rainviewer

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.google.android.material.slider.Slider
import com.squareup.moshi.JsonReader
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Coordinate
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.JSONParser
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.simpleweather.databinding.RadarAnimateContainerBinding
import com.thewizrd.simpleweather.extras.isRadarInteractionEnabled
import com.thewizrd.simpleweather.radar.MapTileRadarViewProvider
import com.thewizrd.simpleweather.radar.RadarProvider
import com.thewizrd.weather_api.utils.APIRequestUtils.checkForErrors
import com.thewizrd.weather_api.utils.RateLimitedRequest
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.io.IOException
import java.io.InputStreamReader
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

@RequiresApi(value = Build.VERSION_CODES.LOLLIPOP)
class RainViewerViewProvider(context: Context, rootView: ViewGroup) :
        MapTileRadarViewProvider(context, rootView) {
    private val availableRadarFrames: MutableList<RadarFrame>
    private val radarLayers: MutableMap<Long, TilesOverlay>

    private var radarContainerBinding: RadarAnimateContainerBinding? = null

    private var animationPosition = 0
    private val mMainHandler: Handler
    private var mProcessingFrames: Boolean = false
    private var mFrameCall: Call? = null

    init {
        availableRadarFrames = ArrayList()
        radarLayers = HashMap()
        mMainHandler = Handler(Looper.getMainLooper())
    }

    override fun onCreateView(savedInstanceState: Bundle?) {
        super.onCreateView(savedInstanceState)

        radarContainerBinding = RadarAnimateContainerBinding.inflate(LayoutInflater.from(context))
        viewContainer.addView(radarContainerBinding!!.root)

        radarContainerBinding!!.playButton.setOnCheckedChangeListener { _, isChecked ->
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
        radarContainerBinding!!.radarToolbar.visibility = if (interactionsEnabled() && isRadarInteractionEnabled()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
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
        val httpClient = sharedDeps.httpClient

        val request = Request.Builder()
            .get()
            .url("https://api.rainviewer.com/public/weather-maps.json".toHttpUrl())
            .build()

        // Connect to webstream
        mFrameCall?.cancel()
        mFrameCall = httpClient.newCall(request)
        mFrameCall!!.enqueue(mFrameCallBack)
    }

    private val mFrameCallBack = object : Callback, RateLimitedRequest {
        override fun getRetryTime(): Long {
            return 5000
        }

        override fun onFailure(call: Call, e: IOException) {
            Logger.writeLine(Log.ERROR, e)
        }

        @Synchronized
        override fun onResponse(call: Call, response: Response) {
            try {
                response.checkForErrors(RadarProvider.RAINVIEWER, this)

                val stream = response.getStream()

                if (call.isCanceled()) return

                // Load data
                val root: WeatherMapsResponse? =
                    JSONParser.deserializer(stream, WeatherMapsResponse::class.java)

                if (call.isCanceled()) return

                mProcessingFrames = true

                // Remove already added tile overlays
                val overlaysToDelete = radarLayers.values.toList()
                radarLayers.clear()
                for (overlay in overlaysToDelete) {
                    mMainHandler.post {
                        overlay.onDetach(mapView)
                        mapView.overlays.remove(overlay)
                    }
                }

                if (call.isCanceled()) {
                    mProcessingFrames = false
                    return
                }

                availableRadarFrames.clear()
                animationPosition = 0

                if (root?.radar != null) {
                    root.radar?.past?.takeIf { it.isNotEmpty() }?.let {
                        availableRadarFrames.addAll(it.mapNotNull { input: RadarItem? ->
                            input?.let { RadarFrame(input.time.toLong(), root.host, input.path) }
                        })
                    }

                    root.radar?.nowcast?.takeIf { it.isNotEmpty() }?.let {
                        availableRadarFrames.addAll(it.mapNotNull { input: RadarItem? ->
                            input?.let { RadarFrame(input.time.toLong(), root.host, input.path) }
                        })
                    }
                }

                mProcessingFrames = false

                mMainHandler.post {
                    if (isViewAlive) {
                        val lastPastFramePosition = (root?.radar?.past?.size ?: 0) - 1
                        showFrame(lastPastFramePosition)
                    }
                }

                // End Stream
                stream.close()
            } catch (ex: Exception) {
                Logger.writeLine(Log.ERROR, ex)
            } finally {
                response.close()
                mProcessingFrames = false
            }
        }
    }

    private fun addLayer(mapFrame: RadarFrame) {
        if (mProcessingFrames) return

        if (!radarLayers.containsKey(mapFrame.timeStamp)) {
            val overlay = TilesOverlay(
                MapTileProviderBasic(context, RainViewTileProvider(mapFrame)),
                context,
                false,
                false
            )
            overlay.isEnabled = false
            mapView.overlays.add(overlay)
            radarLayers[mapFrame.timeStamp] = overlay
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
        val currentTimeStamp = currentFrame.timeStamp

        val nextFrame = availableRadarFrames[position] ?: return
        val nextTimeStamp = nextFrame.timeStamp

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

    private fun updateToolbar(position: Int, mapFrame: RadarFrame = availableRadarFrames[position]
    ) {
        radarContainerBinding!!.animationSeekbar.value = position.toFloat()

        val dateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(mapFrame.timeStamp), ZoneOffset.systemDefault())
        val fmt = if (DateFormat.is24HourFormat(context)) {
            DateTimeUtils.ofPatternForUserLocale(DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_DAYOFWEEK_AND_24HR))
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

    private class RainViewTileProvider(private val mapFrame: RadarFrame?) :
        XYTileSource(
            "RainViewer",
            DEFAULT_ZOOM_LEVEL,
            DEFAULT_ZOOM_LEVEL,
            256,
            ".png",
            arrayOf(mapFrame?.host)
        ) {
        override fun getTileURLString(pMapTileIndex: Long): String? {
            val zoom = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)

            if (mapFrame != null) {
                /* Define the URL pattern for the tile images */
                return String.format(
                    Locale.ROOT,
                    "%s%s/256/%d/%d/%d/1/1_1.png",
                    mapFrame.host,
                    mapFrame.path,
                    zoom,
                    x,
                    y
                )
            }

            return null
        }
    }
}