package com.thewizrd.simpleweather.helpers;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

public class WebViewHelper {
    @SuppressLint("ClickableViewAccessibility")
    public static void disableInteractions(@NonNull WebView webView) {
        webView.setEnabled(false);
        webView.setOnClickListener(null);
        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        WebSettings webSettings = webView.getSettings();
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
    }

    public static void restrictWebView(@NonNull WebView webView) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(false);
        webSettings.setAllowContentAccess(false);
        webSettings.setAllowFileAccess(false);
        webSettings.setAllowFileAccessFromFileURLs(false);
        webSettings.setGeolocationEnabled(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webSettings.setSafeBrowsingEnabled(true);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    public static void enableJS(@NonNull WebView webView, boolean enabled) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(enabled);
    }

    public static void forceReload(@NonNull final WebView webView, final String url) {
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.loadData("<html><body style=\"background-color: black;\"></body></html>", "text/html", "UTF-8");
            }
        });
        webView.postDelayed(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(url);
            }
        }, 1000);
    }

    public static void reload(final WebView webView) {
        if (webView != null) {
            webView.post(new Runnable() {
                @Override
                public void run() {
                    webView.reload();
                }
            });
        }
    }

    public static void loadBlank(@NonNull final WebView webView) {
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.loadData("<html><body style=\"background-color: black;\"></body></html>", "text/html", "UTF-8");
            }
        });
    }

    public static void loadUrl(@NonNull final WebView webView, @NonNull final String url) {
        webView.post(new Runnable() {
            @Override
            public void run() {
                String currentUrl = webView.getOriginalUrl();

                if (!ObjectsCompat.equals(currentUrl, url)) {
                    webView.loadUrl(url);
                }
            }
        });
    }
}
