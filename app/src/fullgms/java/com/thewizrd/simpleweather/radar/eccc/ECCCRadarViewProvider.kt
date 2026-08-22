package com.thewizrd.simpleweather.radar.eccc

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
import androidx.core.net.toUri
import com.bumptech.glide.load.model.GlideUrl
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.material.slider.Slider
import com.thewizrd.shared_resources.DateTimeConstants
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils.getStream
import com.thewizrd.shared_resources.sharedDeps
import com.thewizrd.shared_resources.utils.Coordinate
import com.thewizrd.shared_resources.utils.DateTimeUtils
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.simpleweather.databinding.RadarAnimateContainerBinding
import com.thewizrd.simpleweather.extras.isRadarInteractionEnabled
import com.thewizrd.simpleweather.radar.BoundingBox
import com.thewizrd.simpleweather.radar.CachingUrlTileProvider
import com.thewizrd.simpleweather.radar.MapTileRadarViewProvider
import com.thewizrd.weather_api.utils.APIRequestUtils.checkForErrors
import com.thewizrd.weather_api.utils.RateLimitedRequest
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.w3c.dom.Node
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory


@RequiresApi(value = Build.VERSION_CODES.LOLLIPOP)
class ECCCRadarViewProvider(context: Context, rootView: ViewGroup) :
    MapTileRadarViewProvider(context, rootView) {
    private val availableRadarFrames: MutableList<RadarFrame>
    private val radarLayers: MutableMap<String, TileOverlay>

    private var googleMap: GoogleMap? = null
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
        val httpClient = sharedDeps.httpClient

        val request = Request.Builder()
            .get()
            .url("https://geo.weather.gc.ca/geomet/?lang=en&service=WMS&version=1.3.0&request=GetCapabilities&LAYERS=Radar_1km_SfcPrecipType".toHttpUrl())
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
                response.checkForErrors(WeatherAPI.ECCC, this)

                val stream = response.getStream()

                if (call.isCanceled()) return

                // Load data
                val docFactory = DocumentBuilderFactory.newInstance()
                val builder = docFactory.newDocumentBuilder()
                val capabilitiesDoc = builder.parse(stream)

                // XPath: /WMS_Capabilities/Capability/Layer/Layer/Layer/Layer/Dimension
                val xPath = XPathFactory.newInstance().newXPath()
                val node = xPath.evaluate(
                    "/WMS_Capabilities/Capability/Layer/Layer/Layer/Layer/Dimension",
                    capabilitiesDoc,
                    XPathConstants.NODE
                ) as? Node
                val dimensions = node?.textContent?.split('/') ?: run {
                    capabilitiesDoc.getElementsByTagName("Dimension")?.item(0)?.textContent?.split(
                        '/'
                    )
                }
                if (dimensions?.size != 3) throw IllegalStateException()

                if (call.isCanceled()) return

                mProcessingFrames = true

                // 3 hour window / interval - 6 minutes
                val start = Instant.parse(dimensions[0])
                val end = Instant.parse(dimensions[1])
                val interval = Duration.parse(dimensions[2])

                // Remove already added tile overlays
                val overlaysToDelete = radarLayers.values.toList()
                radarLayers.clear()
                for (overlay in overlaysToDelete) {
                    mMainHandler.post { overlay.remove() }
                }

                availableRadarFrames.clear()
                animationPosition = 0

                var current = start
                var nowIndex = -1

                while (current <= end) {
                    availableRadarFrames.add(RadarFrame(current.toString()))

                    if (current == end) {
                        nowIndex = availableRadarFrames.size - 1
                    }

                    current = current.plus(interval.multipliedBy(2))
                }

                mProcessingFrames = false

                mMainHandler.post {
                    if (isViewAlive) {
                        val lastPastFramePosition = nowIndex
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

        if (!radarLayers.containsKey(mapFrame.timestamp)) {
            val overlay = googleMap!!.addTileOverlay(
                TileOverlayOptions().tileProvider(ECCCTileProvider(context, mapFrame))
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
            ZonedDateTime.ofInstant(
                Instant.parse(mapFrame.timestamp),
                ZoneOffset.systemDefault()
            )
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

    private class ECCCTileProvider(context: Context, private val mapFrame: RadarFrame?) :
        CachingUrlTileProvider(context, 256, 256) {
        override fun getTileUrl(x: Int, y: Int, zoom: Int): GlideUrl? {
            if (!checkTileExists(x, y, zoom)) {
                return null
            }

            if (mapFrame != null) {
                val bbox = BoundingBox.fromTile(x, y, zoom)

                /* Define the URL pattern for the tile images */
                // https://eccc-msc.github.io/open-data/msc-data/obs_radar/readme_radar_geomet_en/
                // https://eccc-msc.github.io/open-data/msc-geomet/wms_en/#wms-getmap
                val uri =
                    "https://geo.weather.gc.ca/geomet".toUri()
                        .buildUpon()
                        .appendQueryParameter("SERVICE", "WMS")
                        .appendQueryParameter("VERSION", "1.3.0")
                        .appendQueryParameter("REQUEST", "GetMap")
                        .appendQueryParameter("BBOX", bbox.toYXString())
                        .appendQueryParameter("CRS", "EPSG:4326")
                        .appendQueryParameter("WIDTH", "256")
                        .appendQueryParameter("HEIGHT", "256")
                        .appendQueryParameter("LAYERS", "Radar_1km_SfcPrecipType")
                        .appendQueryParameter("FORMAT", "image/png")
                        .appendQueryParameter(
                            "TIME",
                            mapFrame.timestamp
                        ) // ex) 2019-06-21T12:00:00Z
                        .build()

                return GlideUrl(uri.toString())
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
    }

    private data class RadarFrame(
        val timestamp: String
    )
}