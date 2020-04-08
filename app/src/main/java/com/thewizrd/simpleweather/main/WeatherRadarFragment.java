package com.thewizrd.simpleweather.main;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentWeatherRadarBinding;
import com.thewizrd.simpleweather.fragments.ToolbarFragment;
import com.thewizrd.simpleweather.helpers.RadarWebClient;
import com.thewizrd.simpleweather.helpers.WebViewHelper;

public class WeatherRadarFragment extends ToolbarFragment {
    private WeatherNowViewModel weatherView = null;
    private FragmentWeatherRadarBinding binding;

    private static final String DEFAULT_URL = "https://earth.nullschool.net/#current/wind/surface/level/overlay=precip_3hr";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(null);
        setExitTransition(null);
        setReenterTransition(null);
        setReturnTransition(null);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        // Use this to return your custom view for this Fragment
        binding = FragmentWeatherRadarBinding.inflate(inflater, root, true);

        // Setup Actionbar
        getToolbar().setNavigationIcon(
                ActivityUtils.getResourceId(getAppCompatActivity(), R.attr.homeAsUpIndicator));
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getAppCompatActivity() != null) getAppCompatActivity().onBackPressed();
            }
        });

        return root;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (binding != null) {
            WebView webView = getRadarWebView();
            if (webView != null) {
                if (!StringUtils.isNullOrWhitespace(weatherView.getRadarURL())) {
                    WebViewHelper.forceReload(webView, weatherView.getRadarURL());
                } else {
                    WebViewHelper.forceReload(webView, DEFAULT_URL);
                }
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        weatherView = new ViewModelProvider(getAppCompatActivity()).get(WeatherNowViewModel.class);
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            WebView webView = getRadarWebView();
            if (webView != null) {
                WebViewHelper.loadBlank(webView);
                binding.radarWebviewContainer.removeAllViews();
                webView.destroy();
            }
        }
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!isHidden()) {
            if (binding != null) {
                WebView webView = getRadarWebView();
                if (webView != null) {
                    webView.resumeTimers();
                }
            }

            initialize();
        }
    }

    @Override
    public void onPause() {
        if (binding != null) {
            WebView webView = getRadarWebView();
            if (webView != null) {
                webView.pauseTimers();
            }
        }

        super.onPause();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (hidden) {
            if (binding != null) {
                WebView webView = getRadarWebView();
                if (webView != null) {
                    webView.pauseTimers();
                }
            }
        }

        if (!hidden && isVisible()) {
            if (binding != null) {
                WebView webView = getRadarWebView();
                if (webView != null) {
                    webView.resumeTimers();
                }
            }

            initialize();
        }
    }

    @Override
    protected int getTitle() {
        return R.string.label_radar;
    }

    // Initialize views here
    @CallSuper
    protected void initialize() {
        updateWindowColors();
        navigateToRadarURL();
    }

    private void navigateToRadarURL() {
        if (weatherView == null || binding == null)
            return;

        WebView webView = getRadarWebView();

        if (webView == null) {
            binding.radarWebviewContainer.addView(webView = createWebView());
        }

        String url = null;
        if (weatherView.isValid()) {
            url = weatherView.getRadarURL();
        }

        if (!StringUtils.isNullOrWhitespace(url)) {
            WebViewHelper.loadUrl(webView, url);
        } else {
            WebViewHelper.loadUrl(webView, DEFAULT_URL);
        }
    }

    @NonNull
    private WebView createWebView() {
        WebView webView = new WebView(this.getContext());

        // WebView
        WebViewHelper.restrictWebView(webView);
        WebViewHelper.enableJS(webView, true);
        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true);
        }

        ViewCompat.setOnApplyWindowInsetsListener(webView, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                layoutParams.setMargins(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0);
                return insets;
            }
        });

        webView.setWebViewClient(new RadarWebClient(false) {
            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                if (binding != null) {
                    WebView wv = getRadarWebView();

                    if (wv == view) {
                        binding.radarWebviewContainer.removeAllViews();
                        wv = null;
                        view.loadUrl("about:blank");
                        view.pauseTimers();
                        view.destroy();
                        navigateToRadarURL();
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

    private WebView getRadarWebView() {
        if (binding != null) {
            return (WebView) binding.radarWebviewContainer.getChildAt(0);
        }

        return null;
    }
}