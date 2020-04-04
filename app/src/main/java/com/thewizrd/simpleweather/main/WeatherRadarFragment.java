package com.thewizrd.simpleweather.main;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

        // WebView
        WebViewHelper.restrictWebView(binding.radarWebview);
        WebViewHelper.enableJS(binding.radarWebview, true);
        binding.radarWebview.setWebViewClient(new RadarWebClient(false));
        binding.radarWebview.setBackgroundColor(Colors.BLACK);

        ViewCompat.setOnApplyWindowInsetsListener(binding.radarWebview, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                layoutParams.setMargins(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0);
                return insets;
            }
        });

        return root;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (!StringUtils.isNullOrWhitespace(weatherView.getRadarURL())) {
            WebViewHelper.forceReload(binding.radarWebview, weatherView.getRadarURL());
        } else {
            WebViewHelper.forceReload(binding.radarWebview, DEFAULT_URL);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        weatherView = new ViewModelProvider(getAppCompatActivity()).get(WeatherNowViewModel.class);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!isHidden()) {
            initialize();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden && isVisible()) {
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

        binding.radarWebview.post(new Runnable() {
            @Override
            public void run() {
                String url = null;
                if (weatherView.isValid()) {
                    url = weatherView.getRadarURL();
                }

                if (!StringUtils.isNullOrWhitespace(url)) {
                    binding.radarWebview.loadUrl(url);
                } else {
                    binding.radarWebview.loadUrl(DEFAULT_URL);
                }
            }
        });
    }
}