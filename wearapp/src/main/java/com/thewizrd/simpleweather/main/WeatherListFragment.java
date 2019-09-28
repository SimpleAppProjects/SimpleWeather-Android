package com.thewizrd.simpleweather.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.databinding.Observable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.CurvingLayoutCallback;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import com.thewizrd.shared_resources.adapters.WeatherAlertPanelAdapter;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.adapters.ForecastItemAdapter;
import com.thewizrd.simpleweather.adapters.HourlyForecastItemAdapter;
import com.thewizrd.simpleweather.fragments.SwipeDismissFragment;

public class WeatherListFragment extends SwipeDismissFragment {
    private WeatherNowViewModel weatherView = null;

    private WearableRecyclerView recyclerView;
    private WearableLinearLayoutManager mLayoutManager;
    private RecyclerView.Adapter mAdapter;

    private WeatherListType weatherType;

    public static WeatherListFragment newInstance(WeatherListType weatherType, WeatherNowViewModel weatherViewModel) {
        WeatherListFragment fragment = new WeatherListFragment();
        if (weatherViewModel != null) {
            fragment.weatherView = weatherViewModel;
        }

        Bundle args = new Bundle();
        args.putInt("WeatherListType", weatherType.getValue());
        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        // Use this to return your custom view for this Fragment
        View outerView = super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_weather_list, (ViewGroup) outerView, true);

        recyclerView = view.findViewById(R.id.recycler_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);
        recyclerView.setEdgeItemsCenteringEnabled(true);

        mLayoutManager = new WearableLinearLayoutManager(mActivity);
        recyclerView.setLayoutManager(mLayoutManager);

        recyclerView.requestFocus();

        return outerView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Don't resume if fragment is hidden
        if (!this.isHidden()) {
            initialize();
            weatherView.addOnPropertyChangedCallback(propertyChangedCallback);
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
        weatherView.removeOnPropertyChangedCallback(propertyChangedCallback);
        super.onPause();
    }

    private Observable.OnPropertyChangedCallback propertyChangedCallback = new Observable.OnPropertyChangedCallback() {
        @Override
        public void onPropertyChanged(Observable sender, int propertyId) {
            if (mActivity != null) {
                mActivity.runOnUiThread(new Runnable() {
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
                newWeatherListType = WeatherListType.valueOf(getArguments().getInt("WeatherListType", 0));
            }

            if (mAdapter == null || weatherType != newWeatherListType) {
                weatherType = newWeatherListType;

                switch (weatherType) {
                    default:
                    case FORECAST:
                        mLayoutManager.setLayoutCallback(new CurvingLayoutCallback(mActivity));
                        mAdapter = new ForecastItemAdapter(weatherView.getForecasts());
                        break;
                    case HOURLYFORECAST:
                        mLayoutManager.setLayoutCallback(new CurvingLayoutCallback(mActivity));
                        mAdapter = new HourlyForecastItemAdapter(weatherView.getExtras().getHourlyForecast());
                        break;
                    case ALERTS:
                        mLayoutManager.setLayoutCallback(null);
                        mAdapter = new WeatherAlertPanelAdapter(weatherView.getExtras().getAlerts());
                        break;
                }

                recyclerView.setAdapter(mAdapter);
            } else {
                if (mAdapter instanceof ForecastItemAdapter) {
                    ((ForecastItemAdapter) mAdapter).updateItems(weatherView.getForecasts());
                } else if (mAdapter instanceof HourlyForecastItemAdapter) {
                    ((HourlyForecastItemAdapter) mAdapter).updateItems(weatherView.getExtras().getHourlyForecast());
                } else if (mAdapter instanceof WeatherAlertPanelAdapter) {
                    ((WeatherAlertPanelAdapter) mAdapter).updateItems(weatherView.getExtras().getAlerts());
                }
            }
        }
    }
}
