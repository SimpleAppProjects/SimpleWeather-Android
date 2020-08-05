package com.thewizrd.simpleweather.main;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
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
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.transition.MaterialContainerTransform;
import com.google.android.material.transition.MaterialFadeThrough;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentWeatherRadarBinding;
import com.thewizrd.simpleweather.fragments.ToolbarFragment;
import com.thewizrd.simpleweather.helpers.RadarWebClient;
import com.thewizrd.simpleweather.helpers.WebViewHelper;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;

public class WeatherRadarFragment extends ToolbarFragment {
    private WeatherNowViewModel weatherView = null;
    private FragmentWeatherRadarBinding binding;

    private static final String DEFAULT_URL = "https://earth.nullschool.net/#current/wind/surface/level/overlay=precip_3hr";

    @NonNull
    @Override
    public SnackbarManager createSnackManager() {
        SnackbarManager mSnackMgr = new SnackbarManager(binding.getRoot());
        mSnackMgr.setSwipeDismissEnabled(true);
        mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);
        return mSnackMgr;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsLogger.logEvent("WeatherRadarFragment: onCreate");
        setExitTransition(new MaterialFadeThrough());
        setEnterTransition(new MaterialFadeThrough());
        setSharedElementEnterTransition(new MaterialContainerTransform());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        // Use this to return your custom view for this Fragment
        binding = FragmentWeatherRadarBinding.inflate(inflater, root, true);

        ViewCompat.setTransitionName(binding.radarWebviewContainer, "radar");

        // Setup Actionbar
        Context context = binding.getRoot().getContext();
        Drawable navIcon = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_arrow_back_white_24dp));
        DrawableCompat.setTint(navIcon, ContextCompat.getColor(context, R.color.invButtonColorText));
        getToolbar().setNavigationIcon(navIcon);
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigateUp();
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
            AnalyticsLogger.logEvent("WeatherRadarFragment: onResume");
            if (binding != null) {
                WebView webView = getRadarWebView();
                if (webView != null) {
                    webView.onResume();
                }
            }

            initialize();
        }
    }

    @Override
    public void onPause() {
        AnalyticsLogger.logEvent("WeatherRadarFragment: onPause");
        if (binding != null) {
            WebView webView = getRadarWebView();
            if (webView != null) {
                webView.onPause();
            }
        }

        super.onPause();
    }

    @Override
    protected int getTitle() {
        return R.string.label_radar;
    }

    // Initialize views here
    @CallSuper
    protected void initialize() {
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

                if (binding != null) {
                    WebView wv = getRadarWebView();

                    if (wv == view) {
                        binding.radarWebviewContainer.removeAllViews();
                        wv = null;
                        view.loadUrl("about:blank");
                        view.onPause();
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