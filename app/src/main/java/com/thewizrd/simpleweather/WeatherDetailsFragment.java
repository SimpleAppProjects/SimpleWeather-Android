package com.thewizrd.simpleweather;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;

import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.simpleweather.adapters.WeatherDetailsAdapter;

public class WeatherDetailsFragment extends WeatherListFragment {
    private boolean isHourly = false;

    public static WeatherDetailsFragment newInstance(LocationData location, WeatherNowViewModel weatherViewModel, boolean isHourly) {
        WeatherDetailsFragment fragment = new WeatherDetailsFragment();
        if (location != null && weatherViewModel != null) {
            fragment.location = location;
            fragment.weatherView = weatherViewModel;
            fragment.isHourly = isHourly;
        }
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            isHourly = savedInstanceState.getBoolean("isHourly", false);
        }
    }

    @Override
    protected void initialize() {
        super.initialize();

        toolbar.setTitle(R.string.label_forecast);

        if (weatherView == null) {
            if (location == null)
                location = Settings.getHomeData();

            Weather weather = Settings.getWeatherData(location.getQuery());
            if (weather != null && weather.isValid()) {
                weatherView = new WeatherNowViewModel(weather);
            }
        }

        if (recyclerView.getItemDecorationCount() == 0)
            recyclerView.addItemDecoration(new DividerItemDecoration(mActivity, DividerItemDecoration.VERTICAL));

        if (weatherView != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    locationName.setText(weatherView.getLocation());
                    // specify an adapter (see also next example)
                    if (isHourly && weatherView.getExtras().getHourlyForecast().size() > 0)
                        recyclerView.setAdapter(new WeatherDetailsAdapter(weatherView.getExtras().getHourlyForecast()));
                    else if (!isHourly) {
                        recyclerView.setAdapter(new WeatherDetailsAdapter(weatherView.getForecasts()));
                    }

                    if (getArguments() != null) {
                        int scrollToPosition = getArguments().getInt("position", 0);
                        layoutManager.scrollToPosition(scrollToPosition);
                    }
                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Save data
        outState.putBoolean("isHourly", isHourly);

        super.onSaveInstanceState(outState);
    }
}
