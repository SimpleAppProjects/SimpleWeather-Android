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
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.adapters.WeatherAlertPanelAdapter;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.ForecastsListViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.controls.WeatherAlertViewModel;
import com.thewizrd.shared_resources.controls.WeatherAlertsViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.SimpleRecyclerViewAdapterObserver;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.simpleweather.adapters.ForecastItemAdapter;
import com.thewizrd.simpleweather.databinding.FragmentWeatherListBinding;
import com.thewizrd.simpleweather.fragments.SwipeDismissFragment;

import java.util.List;

public class WeatherListFragment extends SwipeDismissFragment {
    private WeatherNowViewModel weatherView;
    private ForecastsListViewModel forecastsView;
    private WeatherAlertsViewModel alertsView;
    private LocationData locationData;

    private FragmentWeatherListBinding binding;
    private DividerItemDecoration itemDecoration;

    private WeatherListType weatherType;
    private WeatherListFragmentArgs args;

    public WeatherListFragment() {
        setArguments(new Bundle());
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
            locationData = getSettingsManager().getHomeData();
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
        ViewGroup outerView = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentWeatherListBinding.inflate(inflater, outerView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setEdgeItemsCenteringEnabled(true);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getFragmentActivity()));
        binding.recyclerView.requestFocus();
        itemDecoration = new DividerItemDecoration(getFragmentActivity(), DividerItemDecoration.VERTICAL);

        return outerView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ViewModelProvider vmProvider = new ViewModelProvider(getFragmentActivity());
        weatherView = vmProvider.get(WeatherNowViewModel.class);
        alertsView = vmProvider.get(WeatherAlertsViewModel.class);
        forecastsView = vmProvider.get(ForecastsListViewModel.class);

        binding.progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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

    private void initialize() {
        binding.recyclerView.requestFocus();

        // specify an adapter (see also next example)
        final RecyclerView.Adapter adapter = binding.recyclerView.getAdapter();
        switch (weatherType) {
            case FORECAST:
            case HOURLYFORECAST:
                if (binding.recyclerView.getItemDecorationCount() == 0) {
                    binding.recyclerView.addItemDecoration(itemDecoration);
                }
                final ForecastItemAdapter detailsAdapter;
                if (!(adapter instanceof ForecastItemAdapter)) {
                    detailsAdapter = new ForecastItemAdapter();
                    binding.recyclerView.setAdapter(detailsAdapter);
                } else {
                    detailsAdapter = (ForecastItemAdapter) adapter;
                }

                detailsAdapter.registerAdapterDataObserver(new SimpleRecyclerViewAdapterObserver() {
                    @Override
                    public void onChanged() {
                        super.onChanged();
                        detailsAdapter.unregisterAdapterDataObserver(this);
                        if (binding != null) {
                            binding.progressBar.setVisibility(View.GONE);
                        }
                    }
                });

                if (weatherType == WeatherListType.FORECAST) {
                    forecastsView.getForecasts().removeObservers(WeatherListFragment.this);
                    forecastsView.getForecasts().observe(getViewLifecycleOwner(), new Observer<PagedList<ForecastItemViewModel>>() {
                        @Override
                        public void onChanged(PagedList<ForecastItemViewModel> forecasts) {
                            detailsAdapter.submitList(forecasts);
                        }
                    });
                } else {
                    forecastsView.getHourlyForecasts().removeObservers(getViewLifecycleOwner());
                    forecastsView.getHourlyForecasts().observe(getViewLifecycleOwner(), new Observer<PagedList<HourlyForecastItemViewModel>>() {
                        @Override
                        public void onChanged(PagedList<HourlyForecastItemViewModel> hrforecasts) {
                            detailsAdapter.submitList(hrforecasts);
                        }
                    });
                }
                forecastsView.updateForecasts(locationData);
                break;
            case ALERTS:
                binding.recyclerView.removeItemDecoration(itemDecoration);
                final WeatherAlertPanelAdapter alertAdapter;
                if (!(adapter instanceof WeatherAlertPanelAdapter)) {
                    alertAdapter = new WeatherAlertPanelAdapter();
                    binding.recyclerView.setAdapter(alertAdapter);
                } else {
                    alertAdapter = (WeatherAlertPanelAdapter) adapter;
                }

                alertAdapter.registerAdapterDataObserver(new SimpleRecyclerViewAdapterObserver() {
                    @Override
                    public void onChanged() {
                        super.onChanged();
                        alertAdapter.unregisterAdapterDataObserver(this);
                        binding.progressBar.setVisibility(View.GONE);
                    }
                });

                alertsView.getAlerts().removeObservers(getViewLifecycleOwner());
                alertsView.getAlerts().observe(getViewLifecycleOwner(), new Observer<List<WeatherAlertViewModel>>() {
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


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Save data
        outState.putInt(Constants.ARGS_WEATHERLISTTYPE, weatherType.getValue());
        outState.putString(Constants.KEY_DATA, JSONParser.serializer(locationData, LocationData.class));

        super.onSaveInstanceState(outState);
    }
}