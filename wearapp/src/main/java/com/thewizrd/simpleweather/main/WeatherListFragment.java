package com.thewizrd.simpleweather.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Observable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.CurvingLayoutCallback;
import androidx.wear.widget.WearableLinearLayoutManager;

import com.thewizrd.shared_resources.BR;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.adapters.WeatherAlertPanelAdapter;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.simpleweather.adapters.ForecastItemAdapter;
import com.thewizrd.simpleweather.databinding.FragmentWeatherListBinding;
import com.thewizrd.simpleweather.fragments.SwipeDismissFragment;

public class WeatherListFragment extends SwipeDismissFragment {
    private WeatherNowViewModel weatherView = null;

    private FragmentWeatherListBinding binding;
    private WearableLinearLayoutManager mLayoutManager;
    private RecyclerView.Adapter mAdapter;

    private WeatherListType weatherType;

    public static WeatherListFragment newInstance(WeatherListType weatherType) {
        WeatherListFragment fragment = new WeatherListFragment();

        Bundle args = new Bundle();
        args.putInt(Constants.ARGS_WEATHERLISTTYPE, weatherType.getValue());
        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        // Use this to return your custom view for this Fragment
        View outerView = super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentWeatherListBinding.inflate(inflater, (ViewGroup) outerView, true);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setEdgeItemsCenteringEnabled(true);

        mLayoutManager = new WearableLinearLayoutManager(mActivity);
        binding.recyclerView.setLayoutManager(mLayoutManager);

        return outerView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.weatherView = new ViewModelProvider(mActivity).get(WeatherNowViewModel.class);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        weatherView = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Don't resume if fragment is hidden
        if (!this.isHidden()) {
            initialize();
            if (weatherView != null) {
                weatherView.addOnPropertyChangedCallback(propertyChangedCallback);
            }
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden && this.isVisible()) {
            initialize();
        }
    }

    @Override
    public void onPause() {
        if (weatherView != null) {
            weatherView.removeOnPropertyChangedCallback(propertyChangedCallback);
        }
        super.onPause();
    }

    private Observable.OnPropertyChangedCallback propertyChangedCallback = new Observable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged(Observable sender, int propertyId) {
            if (propertyId == BR.pendingBackground) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (getView() != null)
                            getView().setBackgroundColor(weatherView.getPendingBackground());
                    }
                });
            } else if (propertyId == BR.alerts || propertyId == BR.forecasts || propertyId == BR.hourlyForecasts) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initialize();
                    }
                });
            }
        }
    };

    public void initialize() {
        if (weatherView != null && mActivity != null) {
            if (getView() != null)
                getView().setBackgroundColor(weatherView.getPendingBackground());

            WeatherListType newWeatherListType = WeatherListType.FORECAST;

            if (getArguments() != null) {
                newWeatherListType = WeatherListType.valueOf(getArguments().getInt(Constants.ARGS_WEATHERLISTTYPE, 0));
            }

            if (mAdapter == null || weatherType != newWeatherListType) {
                weatherType = newWeatherListType;

                switch (weatherType) {
                    default:
                    case FORECAST:
                    case HOURLYFORECAST:
                        mLayoutManager.setLayoutCallback(new CurvingLayoutCallback(mActivity));

                        if (weatherType == WeatherListType.FORECAST)
                            mAdapter = new ForecastItemAdapter<>(weatherView.getForecasts());
                        else
                            mAdapter = new ForecastItemAdapter<>(weatherView.getHourlyForecasts());
                        break;
                    case ALERTS:
                        mLayoutManager.setLayoutCallback(null);
                        mAdapter = new WeatherAlertPanelAdapter(weatherView.getAlerts());
                        break;
                }

                binding.recyclerView.setAdapter(mAdapter);
            } else {
                if (mAdapter instanceof ForecastItemAdapter) {
                    if (weatherType == WeatherListType.FORECAST)
                        ((ForecastItemAdapter) mAdapter).updateItems(weatherView.getForecasts());
                    else
                        ((ForecastItemAdapter) mAdapter).updateItems(weatherView.getHourlyForecasts());
                } else if (mAdapter instanceof WeatherAlertPanelAdapter) {
                    ((WeatherAlertPanelAdapter) mAdapter).updateItems(weatherView.getAlerts());
                }
            }
        }
    }
}
