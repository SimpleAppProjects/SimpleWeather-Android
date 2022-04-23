package com.thewizrd.simpleweather.radar.rainviewer

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources.NotFoundException
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.material.slider.Slider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Coordinate
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.weather_api.utils.RateLimitedRequest
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.databinding.RadarAnimateContainerBinding
import com.thewizrd.simpleweather.extras.isRadarInteractionEnabled
import com.thewizrd.simpleweather.radar.CachingUrlTileProvider
import com.thewizrd.simpleweather.radar.MapTileRadarViewProvider
import com.thewizrd.simpleweather.radar.RadarProvider
import com.thewizrd.simpleweather.stag.generated.Stag
import com.thewizrd.weather_api.utils.APIRequestUtils.checkForErrors
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.io.InputStreamReader
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

@RequiresApi(value = Build.VERSION_CODES.LOLLIPOP)
class RainViewerViewProvider(context: Context, rootView: ViewGroup) : MapTileRadarViewProvider(context, rootView) {
    private val availableRadarFrames: MutableList<RadarFrame>
    private val radarLayers: MutableMap<Long, TileOverlay>

    private var googleMap: GoogleMap? = null
    private var radarContainerBinding: RadarAnimateContainerBinding? = null

    private var animationPosition = 0
    private val mMainHandler: Handler
    private var mProcessingFrames: Boolean = false
    private var mFrameCall: Call? = null

    private val gson: Gson

    init {
        availableRadarFrames = ArrayList()
        radarLayers = HashMap()
        mMainHandler = Handler(Looper.getMainLooper())

        gson = GsonBuilder()
                .registerTypeAdapterFactory(Stag.Factory())
                .create()
    }

    override fun onCreateView(savedInstanceState: Bundle?) {
        super.onCreateView(savedInstanceState)

        radarContainerBinding = RadarAnimateContainerBinding.inflate(LayoutInflater.from(context))
        viewContainer.addView(radarContainerBinding!!.root)

        radarContainerBinding!!.playButton.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                mMainHandler.post(animationRunnable)
            } else {
                mMainHandler.removeCallbacks(animationRunnable)
            }
        }

        radarContainerBinding!!.animationSeekbar.addOnChangeListener(Slider.OnChangeListener { slider, value, fromUser ->
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
        radarContainerBinding!!.radarToolbar.visibility = if (interactionsEnabled() && isRadarInteractionEnabled()) View.VISIBLE else View.GONE
        mapView.getMapAsync(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        radarContainerBinding = null
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val currentConfig = context.resources.configuration
        val systemNightMode = currentConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = systemNightMode == Configuration.UI_MODE_NIGHT_YES

        this.googleMap = googleMap

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            context, if (isNightMode) R.raw.gmap_dark_style else R.raw.gmap_light_style)
            )

            if (!success) {
                Timber.tag("RadarView").e("Style parsing failed.")
            }
        } catch (e: NotFoundException) {
            Timber.tag("RadarView").e(e, "Can't find style.")
        }

        mapCameraPosition?.let { cameraPosition ->
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

            if (interactionsEnabled()) {
                if (locationMarker == null) {
                    locationMarker = googleMap.addMarker(MarkerOptions().position(cameraPosition.target))
                } else {
                    locationMarker.position = cameraPosition.target
                }
            }
        }

        val mapUISettings = googleMap.uiSettings
        mapUISettings.isScrollGesturesEnabled = interactionsEnabled()

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
                val root = gson.fromJson<WeatherMapsResponse>(
                    JsonReader(InputStreamReader(stream)),
                    WeatherMapsResponse::class.java
                )

                if (call.isCanceled()) return

                mProcessingFrames = true

                // Remove already added tile overlays
                val overlaysToDelete = ArrayList(radarLayers.values)
                radarLayers.clear()
                for (overlay in overlaysToDelete) {
                    mMainHandler.post { overlay.remove() }
                }

                if (call.isCanceled()) {
                    mProcessingFrames = false
                    return
                }

                availableRadarFrames.clear()
                animationPosition = 0

                if (root?.radar != null) {
                    if (root.radar?.past?.isNotEmpty() == true) {
                        availableRadarFrames.addAll(root.radar.past.mapNotNull { input: RadarItem? ->
                            input?.let { RadarFrame(input.time.toLong(), root.host, input.path) }
                        })
                    }

                    if (root.radar?.nowcast?.isNotEmpty() == true) {
                        availableRadarFrames.addAll(root.radar.nowcast.mapNotNull { input: RadarItem? ->
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
        if (!radarLayers.containsKey(mapFrame.timeStamp)) {
            val overlay = googleMap!!.addTileOverlay(
                TileOverlayOptions().tileProvider(RainViewTileProvider(context, mapFrame))
                    .transparency(1f)
            )
            if (overlay != null) {
                radarLayers[mapFrame.timeStamp] = overlay
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
                currentOverlay.transparency = 1f
            }
        }
        val nextOverlay = radarLayers[nextTimeStamp]
        if (nextOverlay != null) {
            nextOverlay.transparency = 0f
        }

        updateToolbar(position, nextFrame)
    }

    private fun updateToolbar(position: Int, mapFrame: RadarFrame = availableRadarFrames[position]) {
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

    private class RainViewTileProvider(context: Context, private val mapFrame: RadarFrame?) : CachingUrlTileProvider(context, 256, 256) {
        override fun getTileUrl(x: Int, y: Int, zoom: Int): String? {
            if (!checkTileExists(x, y, zoom)) {
                return null
            }

            if (mapFrame != null) {
                /* Define the URL pattern for the tile images */
                return String.format(Locale.ROOT, "%s%s/256/%d/%d/%d/1/1_1.png", mapFrame.host, mapFrame.path, zoom, x, y)
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
            val minZoom = 6
            val maxZoom = 6

            return zoom >= minZoom && zoom <= maxZoom
        }
    }
}