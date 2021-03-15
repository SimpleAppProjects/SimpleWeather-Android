package com.thewizrd.simpleweather.helpers

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.WebView
import androidx.core.util.ObjectsCompat

object WebViewHelper {
    @JvmStatic
    @SuppressLint("ClickableViewAccessibility")
    fun WebView.disableInteractions() {
        this.isEnabled = false
        this.setOnClickListener(null)
        this.setOnTouchListener { v, event -> true }
        this.settings.apply {
            setSupportZoom(false)
            displayZoomControls = false
        }
    }

    @JvmStatic
    @SuppressLint("ClickableViewAccessibility")
    fun WebView.enableInteractions() {
        this.isEnabled = true
        this.setOnTouchListener(null)
        this.settings.apply {
            setSupportZoom(true)
            displayZoomControls = true
        }
    }

    @JvmStatic
    fun WebView.restrictWebView() {
        this.settings.apply {
            this.javaScriptEnabled = false
            this.allowContentAccess = false
            this.allowFileAccess = false
            this.allowFileAccessFromFileURLs = false
            this.setGeolocationEnabled(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.safeBrowsingEnabled = true
            }
        }
    }

    @JvmStatic
    @SuppressLint("SetJavaScriptEnabled")
    fun WebView.enableJS(enabled: Boolean) {
        this.settings.javaScriptEnabled = enabled
    }

    @JvmStatic
    fun WebView.forceReload(url: String?) {
        this.post {
            this.loadData("<html><body style=\"background-color: black;\"></body></html>", "text/html", "UTF-8")
        }
        this.postDelayed({ this.loadUrl(url) }, 1000)
    }

    fun WebView?.reload() {
        this?.post { this.reload() }
    }

    @JvmStatic
    fun WebView.loadBlank() {
        this.post {
            try {
                this.loadData("<html><body style=\"background-color: black;\"></body></html>", "text/html", "UTF-8")
            } catch (ignored: Exception) {
            }
        }
    }

    @JvmStatic
    fun WebView.postLoadUrl(url: String) {
        this.post {
            val currentUrl = this.originalUrl
            if (!ObjectsCompat.equals(currentUrl, url)) {
                this.loadUrl(url)
            }
        }
    }
}