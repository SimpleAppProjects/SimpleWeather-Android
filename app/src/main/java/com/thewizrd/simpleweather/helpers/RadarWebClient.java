package com.thewizrd.simpleweather.helpers;

import android.os.Build;
import android.view.KeyEvent;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;

public class RadarWebClient extends WebViewClient {
    private static final String DEFAULT_URL = "https://earth.nullschool.net/#current/wind/surface/level/overlay=precip_3hr";
    private boolean disableInteraction;

    public RadarWebClient(boolean disableInteraction) {
        super();
        this.disableInteraction = disableInteraction;
    }

    @Override
    public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
        return true;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return true;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        if (disableInteraction) {
            view.evaluateJavascript("var style = document.createElement('style'); style.innerHTML = '* { pointer-events: none !important; overscroll-behavior: none !important; overflow: hidden !important; } body { pointer-events: all !important; overscroll-behavior: none !important; overflow: hidden !important; }'; document.head.appendChild(style);", null);
        }
    }
}
