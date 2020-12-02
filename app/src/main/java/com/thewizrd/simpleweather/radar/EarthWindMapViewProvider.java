package com.thewizrd.simpleweather.radar;

import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.simpleweather.helpers.RadarWebClient;
import com.thewizrd.simpleweather.helpers.WebViewHelper;

import java.util.Locale;

@RequiresApi(value = Build.VERSION_CODES.LOLLIPOP)
public class EarthWindMapViewProvider extends RadarViewProvider {
    private String radarURL;

    public EarthWindMapViewProvider(@NonNull Fragment fragment, @NonNull ViewGroup rootView) {
        super(fragment, rootView);
    }

    @Override
    public void updateCoordinates(@NonNull WeatherUtils.Coordinate coordinates, boolean updateView) {
        radarURL = String.format(Locale.ROOT, RadarProvider.EARTHWINDMAP_URL_FORMAT, coordinates.getLongitude(), coordinates.getLatitude());
        if (updateView) updateRadarView();
    }

    @Override
    public void updateRadarView() {
        WebView webView = getRadarWebView();

        if (webView == null) {
            getViewContainer().addView(webView = createWebView());
        }

        if (interactionsEnabled()) {
            WebViewHelper.enableInteractions(webView);
        } else {
            WebViewHelper.disableInteractions(webView);
        }

        if (!StringUtils.isNullOrWhitespace(radarURL)) {
            WebViewHelper.loadUrl(webView, radarURL);
        } else {
            WebViewHelper.loadUrl(webView, RadarProvider.EARTHWINDMAP_DEFAULT_URL);
        }
    }

    @Override
    public void onDestroyView() {
        if (getViewContainer() != null) {
            WebView webView = getRadarWebView();
            if (webView != null) {
                WebViewHelper.loadBlank(webView);
                getViewContainer().removeAllViews();
                webView.destroy();
            }
        }
    }

    @Override
    public void onResume() {
        if (getViewContainer() != null) {
            WebView webView = getRadarWebView();
            if (webView != null) {
                webView.onResume();
            }
        }
    }

    @Override
    public void onPause() {
        if (getViewContainer() != null) {
            WebView webView = getRadarWebView();
            if (webView != null) {
                webView.onPause();
            }
        }
    }

    @Override
    public void onConfigurationChanged() {
        if (getViewContainer() != null) {
            WebView webView = getRadarWebView();
            if (webView != null) {
                if (radarURL != null) {
                    WebViewHelper.forceReload(webView, radarURL);
                } else {
                    WebViewHelper.forceReload(webView, RadarProvider.EARTHWINDMAP_DEFAULT_URL);
                }
            }
        }
    }

    private WebView getRadarWebView() {
        if (getViewContainer() != null) {
            return (WebView) getViewContainer().getChildAt(0);
        }

        return null;
    }

    @NonNull
    private WebView createWebView() {
        WebView webView = new WebView(getParentFragment().getContext());

        // WebView
        WebViewHelper.restrictWebView(webView);
        WebViewHelper.enableJS(webView, true);
        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true);
        }

        webView.setWebViewClient(new RadarWebClient(false) {
            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Bundle args = new Bundle();
                    args.putBoolean("didCrash", detail.didCrash());
                    args.putInt("renderPriorityAtExit", detail.rendererPriorityAtExit());
                    AnalyticsLogger.logEvent("WeatherRadarFragment: render gone", args);
                } else {
                    AnalyticsLogger.logEvent("WeatherRadarFragment: render gone");
                }

                if (getViewContainer() != null) {
                    WebView wv = getRadarWebView();

                    if (wv == view) {
                        getViewContainer().removeAllViews();
                        wv = null;
                        view.loadUrl("about:blank");
                        view.onPause();
                        view.destroy();
                        updateRadarView();
                        return true;
                    }
                }

                return super.onRenderProcessGone(view, detail);
            }
        });
        webView.setBackgroundColor(Colors.BLACK);
        webView.resumeTimers();

        return webView;
    }
}
