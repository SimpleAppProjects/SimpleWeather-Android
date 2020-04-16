package com.thewizrd.simpleweather.main;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionSet;
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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.adapters.WeatherAlertPanelAdapter;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.ForecastsViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.controls.WeatherAlertViewModel;
import com.thewizrd.shared_resources.controls.WeatherAlertsViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherRequest;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.adapters.WeatherDetailsAdapter;
import com.thewizrd.simpleweather.databinding.FragmentWeatherListBinding;
import com.thewizrd.simpleweather.fragments.ToolbarFragment;
import com.thewizrd.simpleweather.snackbar.Snackbar;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;

import java.util.List;

public class WeatherListFragment extends ToolbarFragment {
    private WeatherNowViewModel weatherView = null;
    private ForecastsViewModel forecastsView = null;
    private WeatherAlertsViewModel alertsView = null;
    private LocationData location = null;

    private FragmentWeatherListBinding binding;
    private LinearLayoutManager layoutManager;
    private SnackbarManager mSnackMgr;

    private WeatherListType weatherType;

    public static WeatherListFragment newInstance(LocationData locData, WeatherListType type) {
        WeatherListFragment fragment = new WeatherListFragment();
        fragment.weatherType = type;
        fragment.location = locData;

        Bundle args = new Bundle();
        args.putInt(Constants.ARGS_WEATHERLISTTYPE, type.getValue());
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && weatherType == WeatherListType.ALERTS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                setEnterTransition(new TransitionSet()
                        .addTransition(new Slide(Gravity.START).setDuration(200))
                        .addTransition(new Fade(Fade.IN).setDuration(200)));
            } else {
                setEnterTransition(new TransitionSet()
                        .addTransition(new Slide(Gravity.LEFT).setDuration(200))
                        .addTransition(new Fade(Fade.IN).setDuration(200)));
            }
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.ARGS_WEATHERLISTTYPE)) {
                weatherType = WeatherListType.valueOf(savedInstanceState.getInt(Constants.ARGS_WEATHERLISTTYPE));
            }
            if (savedInstanceState.containsKey(Constants.KEY_DATA)) {
                location = JSONParser.deserializer(savedInstanceState.getString(Constants.KEY_DATA), LocationData.class);
            }
        }

        if (location == null)
            location = Settings.getHomeData();
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
                if (binding == null) return;
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
        final ViewModelProvider vmProvider = new ViewModelProvider(getAppCompatActivity());
        weatherView = vmProvider.get(WeatherNowViewModel.class);
        alertsView = vmProvider.get(WeatherAlertsViewModel.class);
        forecastsView = new ViewModelProvider(this).get(ForecastsViewModel.class);
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
            initSnackManager();
            initialize();
        } else {
            dismissAllSnackbars();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden && isVisible()) {
            initSnackManager();
            initialize();
        } else {
            dismissAllSnackbars();
            mSnackMgr = null;
        }
    }

    @Override
    public void onPause() {
        dismissAllSnackbars();
        mSnackMgr = null;
        super.onPause();
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

        if (!weatherView.isValid()) {
            new WeatherDataLoader(location)
                    .loadWeatherData(new WeatherRequest.Builder()
                            .forceLoadSavedData()
                            .setErrorListener(new WeatherRequest.WeatherErrorListener() {
                                @Override
                                public void onWeatherError(WeatherException wEx) {
                                    switch (wEx.getErrorStatus()) {
                                        case NETWORKERROR:
                                        case NOWEATHER:
                                            // Show error message and prompt to refresh
                                            showSnackbar(Snackbar.make(wEx.getMessage(), Snackbar.Duration.LONG), null);
                                            break;
                                        case QUERYNOTFOUND:
                                            if (WeatherAPI.NWS.equals(Settings.getAPI())) {
                                                showSnackbar(Snackbar.make(R.string.error_message_weather_us_only, Snackbar.Duration.LONG), null);
                                                break;
                                            }
                                        default:
                                            // Show error message
                                            showSnackbar(Snackbar.make(wEx.getMessage(), Snackbar.Duration.LONG), null);
                                            break;
                                    }
                                }
                            })
                            .build())
                    .addOnSuccessListener(requireActivity(), new OnSuccessListener<Weather>() {
                        @Override
                        public void onSuccess(Weather weather) {
                            weatherView.updateView(weather);
                            forecastsView.updateForecasts(location);
                            alertsView.updateAlerts(location);
                        }
                    });
        }
        forecastsView.updateForecasts(location);
        alertsView.updateAlerts(location);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (binding == null) return;
                binding.locationName.setText(weatherView.getLocation());

                // specify an adapter (see also next example)
                final RecyclerView.Adapter adapter = binding.recyclerView.getAdapter();
                switch (weatherType) {
                    case FORECAST:
                    case HOURLYFORECAST:
                        if (binding.recyclerView.getItemDecorationCount() == 0)
                            binding.recyclerView.addItemDecoration(new DividerItemDecoration(getAppCompatActivity(), DividerItemDecoration.VERTICAL));

                        final WeatherDetailsAdapter detailsAdapter;
                        if (!(adapter instanceof WeatherDetailsAdapter)) {
                            detailsAdapter = new WeatherDetailsAdapter();
                            binding.recyclerView.setAdapter(detailsAdapter);
                        } else {
                            detailsAdapter = (WeatherDetailsAdapter) adapter;
                        }

                        if (weatherType == WeatherListType.FORECAST) {
                            forecastsView.getForecasts().removeObservers(WeatherListFragment.this);
                            forecastsView.getForecasts().observe(WeatherListFragment.this, new Observer<PagedList<ForecastItemViewModel>>() {
                                @Override
                                public void onChanged(PagedList<ForecastItemViewModel> forecasts) {
                                    detailsAdapter.submitList(forecasts);
                                }
                            });
                        } else {
                            forecastsView.getHourlyForecasts().removeObservers(WeatherListFragment.this);
                            forecastsView.getHourlyForecasts().observe(WeatherListFragment.this, new Observer<PagedList<HourlyForecastItemViewModel>>() {
                                @Override
                                public void onChanged(PagedList<HourlyForecastItemViewModel> hrforecasts) {
                                    detailsAdapter.submitList(hrforecasts);
                                }
                            });
                        }

                        if (getArguments() != null) {
                            int scrollToPosition = getArguments().getInt(Constants.KEY_POSITION, 0);
                            layoutManager.scrollToPositionWithOffset(scrollToPosition, 0);
                        }
                        break;
                    case ALERTS:
                        final WeatherAlertPanelAdapter alertAdapter;
                        if (!(adapter instanceof WeatherAlertPanelAdapter)) {
                            alertAdapter = new WeatherAlertPanelAdapter();
                            binding.recyclerView.setAdapter(alertAdapter);
                        } else {
                            alertAdapter = (WeatherAlertPanelAdapter) adapter;
                        }

                        alertsView.getAlerts().removeObservers(WeatherListFragment.this);
                        alertsView.getAlerts().observe(WeatherListFragment.this, new Observer<List<WeatherAlertViewModel>>() {
                            @Override
                            public void onChanged(List<WeatherAlertViewModel> alerts) {
                                alertAdapter.updateItems(alerts);
                            }
                        });
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
        outState.putString(Constants.KEY_DATA, JSONParser.serializer(location, LocationData.class));

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

    private void initSnackManager() {
        if (mSnackMgr == null) {
            mSnackMgr = new SnackbarManager(binding.getRoot());
            mSnackMgr.setSwipeDismissEnabled(true);
            mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);
        }
    }

    private void showSnackbar(final Snackbar snackbar, final com.google.android.material.snackbar.Snackbar.Callback callback) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSnackMgr != null) mSnackMgr.show(snackbar, callback);
            }
        });
    }

    private void dismissAllSnackbars() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSnackMgr != null) mSnackMgr.dismissAll();
            }
        });
    }
}
