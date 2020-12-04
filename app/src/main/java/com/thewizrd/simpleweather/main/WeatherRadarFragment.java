package com.thewizrd.simpleweather.main;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
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
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentWeatherRadarBinding;
import com.thewizrd.simpleweather.fragments.ToolbarFragment;
import com.thewizrd.simpleweather.radar.RadarProvider;
import com.thewizrd.simpleweather.radar.RadarViewProvider;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;

@RequiresApi(value = Build.VERSION_CODES.LOLLIPOP)
public class WeatherRadarFragment extends ToolbarFragment {
    private WeatherNowViewModel weatherView = null;
    private FragmentWeatherRadarBinding binding;

    private RadarViewProvider radarViewProvider;

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

        radarViewProvider = RadarProvider.getRadarViewProvider(context, binding.radarWebviewContainer);
        radarViewProvider.enableInteractions(true);
        radarViewProvider.onCreateView(savedInstanceState);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        weatherView = new ViewModelProvider(getAppCompatActivity()).get(WeatherNowViewModel.class);
        if (radarViewProvider != null && weatherView.getLocationCoord() != null) {
            radarViewProvider.onViewCreated(weatherView.getLocationCoord());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (radarViewProvider != null) {
            radarViewProvider.onStart();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsLogger.logEvent("WeatherRadarFragment: onResume");

        if (radarViewProvider != null) {
            radarViewProvider.onResume();
        }
        initialize();
    }

    @Override
    public void onPause() {
        AnalyticsLogger.logEvent("WeatherRadarFragment: onPause");

        if (radarViewProvider != null) {
            radarViewProvider.onPause();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        if (radarViewProvider != null) {
            radarViewProvider.onStop();
        }
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (radarViewProvider != null) {
            radarViewProvider.onSaveInstanceState(outState);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        if (radarViewProvider != null) {
            radarViewProvider.onDestroyView();
            radarViewProvider = null;
        }
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (radarViewProvider != null) {
            radarViewProvider.onLowMemory();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (radarViewProvider != null) {
            radarViewProvider.onConfigurationChanged();
        }
    }

    @Override
    protected int getTitle() {
        return R.string.label_radar;
    }

    // Initialize views here
    @CallSuper
    protected void initialize() {
        radarViewProvider.updateRadarView();
    }
}