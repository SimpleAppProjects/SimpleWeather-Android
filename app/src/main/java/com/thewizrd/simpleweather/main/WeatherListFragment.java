package com.thewizrd.simpleweather.main;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Fade;
import androidx.transition.Slide;
import androidx.transition.TransitionSet;

import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.adapters.WeatherAlertPanelAdapter;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.adapters.WeatherDetailsAdapter;
import com.thewizrd.simpleweather.databinding.FragmentWeatherListBinding;
import com.thewizrd.simpleweather.fragments.ToolbarFragment;

public class WeatherListFragment extends ToolbarFragment {
    private WeatherNowViewModel weatherView = null;

    private FragmentWeatherListBinding binding;
    private LinearLayoutManager layoutManager;

    private RecyclerView.Adapter mAdapter;
    private WeatherListType weatherType;

    public static WeatherListFragment newInstance(WeatherListType type) {
        WeatherListFragment fragment = new WeatherListFragment();
        fragment.weatherType = type;

        Bundle args = new Bundle();
        args.putInt(Constants.ARGS_WEATHERLISTTYPE, type.getValue());
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (weatherType == WeatherListType.ALERTS) {
            setEnterTransition(new TransitionSet()
                    .addTransition(new Slide(Gravity.START).setDuration(200))
                    .addTransition(new Fade(Fade.IN).setDuration(200)));
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.ARGS_WEATHERLISTTYPE)) {
                weatherType = WeatherListType.valueOf(savedInstanceState.getInt(Constants.ARGS_WEATHERLISTTYPE));
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        // Use this to return your custom view for this Fragment
        binding = FragmentWeatherListBinding.inflate(inflater, root, true);

        // Setup Actionbar
        getToolbar().setNavigationIcon(
                ActivityUtils.getResourceId(getAppCompatActivity(), R.attr.homeAsUpIndicator));
        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getAppCompatActivity() != null) getAppCompatActivity().onBackPressed();
            }
        });

        binding.locationHeader.post(new Runnable() {
            @Override
            public void run() {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) binding.recyclerView.getLayoutParams();
                layoutParams.topMargin = binding.locationHeader.getHeight();
                binding.recyclerView.setLayoutParams(layoutParams);
            }
        });

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the binding.recyclerView
        binding.recyclerView.setHasFixedSize(true);
        // use a linear layout manager
        binding.recyclerView.setLayoutManager(layoutManager = new LinearLayoutManager(getAppCompatActivity()));

        ViewCompat.setOnApplyWindowInsetsListener(binding.locationHeader, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                binding.locationHeader.setContentPadding(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0);
                return insets;
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewCompat.setPaddingRelative(v, insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0);
                return insets;
            }
        });

        return root;
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

        if (!hidden && isVisible())
            initialize();
    }

    @Override
    protected int getTitle() {
        switch (weatherType) {
            case FORECAST:
            case HOURLYFORECAST:
                return R.string.label_forecast;
            case ALERTS:
                return R.string.title_fragment_alerts;
            default:
                return R.string.label_nav_weathernow;
        }
    }

    // Initialize views here
    @CallSuper
    protected void initialize() {
        updateWindowColors();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.locationName.setText(weatherView.getLocation());

                // specify an adapter (see also next example)
                switch (weatherType) {
                    case FORECAST:
                    case HOURLYFORECAST:
                        if (binding.recyclerView.getItemDecorationCount() == 0)
                            binding.recyclerView.addItemDecoration(new DividerItemDecoration(getAppCompatActivity(), DividerItemDecoration.VERTICAL));

                        if (weatherType == WeatherListType.FORECAST)
                            binding.recyclerView.setAdapter(new WeatherDetailsAdapter<>(weatherView.getForecasts()));
                        else
                            binding.recyclerView.setAdapter(new WeatherDetailsAdapter<>(weatherView.getHourlyForecasts()));

                        if (getArguments() != null) {
                            int scrollToPosition = getArguments().getInt(Constants.KEY_POSITION, 0);
                            layoutManager.scrollToPositionWithOffset(scrollToPosition, 0);
                        }
                        break;
                    case ALERTS:
                        binding.recyclerView.setAdapter(new WeatherAlertPanelAdapter(weatherView.getAlerts()));
                        break;
                    default:
                        binding.recyclerView.setAdapter(null);
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Save data
        outState.putInt(Constants.ARGS_WEATHERLISTTYPE, weatherType.getValue());

        super.onSaveInstanceState(outState);
    }

    @Override
    public void updateWindowColors() {
        super.updateWindowColors();

        int currentNightMode = AppCompatDelegate.getDefaultNightMode();
        int bg_color = ActivityUtils.getColor(getAppCompatActivity(), android.R.attr.colorBackground);
        if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES && Settings.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
            bg_color = Colors.BLACK;
        }
        binding.locationHeader.setCardBackgroundColor(bg_color);
        binding.recyclerView.setBackgroundColor(bg_color);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        binding.recyclerView.setAdapter(binding.recyclerView.getAdapter());
        updateWindowColors();
    }
}
