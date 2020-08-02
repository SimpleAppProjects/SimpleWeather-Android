package com.thewizrd.simpleweather.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.CurvingLayoutCallback;
import androidx.wear.widget.WearableLinearLayoutManager;

import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.adapters.WeatherAlertPanelAdapter;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.ForecastsViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.controls.WeatherAlertViewModel;
import com.thewizrd.shared_resources.controls.WeatherAlertsViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.adapters.ForecastItemAdapter;
import com.thewizrd.simpleweather.databinding.FragmentWeatherListBinding;
import com.thewizrd.simpleweather.fragments.SwipeDismissFragment;

import java.util.List;

public class WeatherListFragment extends SwipeDismissFragment {
    private WeatherNowViewModel weatherView;
    private ForecastsViewModel forecastsView;
    private WeatherAlertsViewModel alertsView;
    private LocationData locationData;

    private FragmentWeatherListBinding binding;
    private WearableLinearLayoutManager mLayoutManager;

    private WeatherListType weatherType;
    private WeatherListFragmentArgs args;

    public WeatherListFragment() {
        setArguments(new Bundle());
    }

    @Override
    public boolean isAlive() {
        return binding != null && super.isAlive();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsLogger.logEvent("WeatherList: onCreate");

        args = WeatherListFragmentArgs.fromBundle(requireArguments());

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.ARGS_WEATHERLISTTYPE)) {
                weatherType = WeatherListType.valueOf(savedInstanceState.getInt(Constants.ARGS_WEATHERLISTTYPE));
            }
            if (savedInstanceState.containsKey(Constants.KEY_DATA)) {
                locationData = JSONParser.deserializer(savedInstanceState.getString(Constants.KEY_DATA), LocationData.class);
            }
        }

        weatherType = args.getWeatherListType();
        if (args.getData() != null) {
            locationData = JSONParser.deserializer(args.getData(), LocationData.class);
        }

        if (locationData == null) {
            locationData = Settings.getHomeData();
        }

        getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            private void load() {
                initialize();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        // Use this to return your custom view for this Fragment
        View outerView = super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentWeatherListBinding.inflate(inflater, (ViewGroup) outerView, true);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setEdgeItemsCenteringEnabled(true);

        mLayoutManager = new WearableLinearLayoutManager(getFragmentActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);

        binding.recyclerView.requestFocus();

        return outerView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ViewModelProvider vmProvider = new ViewModelProvider(getFragmentActivity());
        weatherView = vmProvider.get(WeatherNowViewModel.class);
        alertsView = vmProvider.get(WeatherAlertsViewModel.class);
        forecastsView = vmProvider.get(ForecastsViewModel.class);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsLogger.logEvent("WeatherList: onResume");
    }

    @Override
    public void onPause() {
        AnalyticsLogger.logEvent("WeatherList: onPause");
        super.onPause();
    }

    public void initialize() {
        if (isAlive() && getView() != null) {
            binding.recyclerView.requestFocus();

            // specify an adapter (see also next example)
            final RecyclerView.Adapter adapter = binding.recyclerView.getAdapter();
            switch (weatherType) {
                case FORECAST:
                case HOURLYFORECAST:
                    if (!(mLayoutManager.getLayoutCallback() instanceof CurvingLayoutCallback)) {
                        mLayoutManager.setLayoutCallback(new CurvingLayoutCallback(getFragmentActivity()));
                    }

                    final ForecastItemAdapter detailsAdapter;
                    if (!(adapter instanceof ForecastItemAdapter)) {
                        detailsAdapter = new ForecastItemAdapter();
                        binding.recyclerView.setAdapter(detailsAdapter);
                    } else {
                        detailsAdapter = (ForecastItemAdapter) adapter;
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
                    forecastsView.updateForecasts(locationData);
                    break;
                case ALERTS:
                    mLayoutManager.setLayoutCallback(null);

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
                    alertsView.updateAlerts(locationData);
                    break;
                default:
                    binding.recyclerView.setAdapter(null);
            }
        }
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Save data
        outState.putInt(Constants.ARGS_WEATHERLISTTYPE, weatherType.getValue());
        outState.putString(Constants.KEY_DATA, JSONParser.serializer(locationData, LocationData.class));

        super.onSaveInstanceState(outState);
    }
}