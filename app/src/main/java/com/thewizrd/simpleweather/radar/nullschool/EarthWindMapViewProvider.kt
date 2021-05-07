package com.thewizrd.simpleweather.radar.nullschool

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.annotation.RequiresApi
import com.google.android.material.snackbar.Snackbar
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import com.thewizrd.shared_resources.utils.Colors
import com.thewizrd.shared_resources.utils.Coordinate
import com.thewizrd.shared_resources.utils.StringUtils
import com.thewizrd.simpleweather.helpers.RadarWebClient
import com.thewizrd.simpleweather.helpers.WebViewHelper.disableInteractions
import com.thewizrd.simpleweather.helpers.WebViewHelper.enableInteractions
import com.thewizrd.simpleweather.helpers.WebViewHelper.enableJS
import com.thewizrd.simpleweather.helpers.WebViewHelper.forceReload
import com.thewizrd.simpleweather.helpers.WebViewHelper.loadBlank
import com.thewizrd.simpleweather.helpers.WebViewHelper.postLoadUrl
import com.thewizrd.simpleweather.helpers.WebViewHelper.restrictWebView
import com.thewizrd.simpleweather.radar.RadarViewProvider
import java.util.*

@RequiresApi(value = Build.VERSION_CODES.LOLLIPOP)
class EarthWindMapViewProvider(context: Context, rootView: ViewGroup) : RadarViewProvider(context, rootView) {
    companion object {
        private const val EARTHWINDMAP_DEFAULT_URL = "https://earth.nullschool.net/#current/wind/surface/level/overlay=precip_3hr"
        private const val EARTHWINDMAP_URL_FORMAT = "$EARTHWINDMAP_DEFAULT_URL/orthographic=%s,%s,3000"
    }

    private var radarURL: String? = null
    override fun updateCoordinates(coordinates: Coordinate, updateView: Boolean) {
        radarURL = String.format(Locale.ROOT, EARTHWINDMAP_URL_FORMAT, coordinates.longitude, coordinates.latitude)
        if (updateView) updateRadarView()
    }

    override fun onCreateView(savedInstanceState: Bundle?) {
        super.onCreateView(savedInstanceState)
        createWebView()?.let {
            viewContainer.addView(it)
        }
    }

    override fun updateRadarView() {
        var webView = radarWebView
        if (webView == null) {
            createWebView()?.let {
                viewContainer.addView(it)
                webView = it
            }
        }

        if (interactionsEnabled()) {
            webView?.enableInteractions()
        } else {
            webView?.disableInteractions()
        }

        if (!StringUtils.isNullOrWhitespace(radarURL)) {
            webView?.postLoadUrl(radarURL!!)
        } else {
            webView?.postLoadUrl(EARTHWINDMAP_DEFAULT_URL)
        }
    }

    override fun onDestroyView() {
        if (viewContainer != null) {
            radarWebView?.let {
                it.loadBlank()
                viewContainer.removeAllViews()
                it.destroy()
            }
        }
    }

    override fun onResume() {
        if (viewContainer != null) {
            radarWebView?.onResume()
        }
    }

    override fun onPause() {
        if (viewContainer != null) {
            radarWebView?.onPause()
        }
    }

    override fun onConfigurationChanged() {
        if (viewContainer != null) {
            val webView = radarWebView
            if (webView != null) {
                if (radarURL != null) {
                    webView.forceReload(radarURL)
                } else {
                    webView.forceReload(EARTHWINDMAP_DEFAULT_URL)
                }
            }
        }
    }

    private val radarWebView: WebView?
        get() {
            return viewContainer?.getChildAt(0) as WebView?
        }

    private fun createWebView(): WebView? {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_WEBVIEW)) {
            AnalyticsLogger.logEvent("EarthWindMap: no webview installed")
            Snackbar.make(viewContainer, "WebView not installed", Snackbar.LENGTH_SHORT).show()
            return null
        }

        return WebView(context).apply {
            // WebView
            restrictWebView()
            enableJS(true)

            settings.setRenderPriority(WebSettings.RenderPriority.HIGH)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true)
            }

            webViewClient = object : RadarWebClient(!interactionsEnabled()) {
                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val args = Bundle().apply {
                            putBoolean("didCrash", detail.didCrash())
                            putInt("renderPriorityAtExit", detail.rendererPriorityAtExit())
                        }
                        AnalyticsLogger.logEvent("WeatherRadarFragment: render gone", args)
                    } else {
                        AnalyticsLogger.logEvent("WeatherRadarFragment: render gone")
                    }

                    if (viewContainer != null) {
                        var wv = radarWebView

                        if (wv === view) {
                            viewContainer.removeAllViews()
                            wv = null
                            view.loadUrl("about:blank")
                            view.onPause()
                            view.destroy()
                            updateRadarView()
                            return true
                        }
                    }

                    return super.onRenderProcessGone(view, detail)
                }
            }

            setBackgroundColor(Colors.BLACK)
            resumeTimers()
        }
    }
}