package com.thewizrd.simpleweather.main;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;

import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.adapters.WeatherDetailsAdapter;

public class WeatherDetailsFragment extends WeatherListFragment {
    private static final String ARGS_ISHOURLY = "isHourly";
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
    protected int getTitle() {
        return R.string.label_forecast;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            isHourly = savedInstanceState.getBoolean(ARGS_ISHOURLY, false);
        }
    }

    @Override
    protected void initialize() {
        super.initialize();

        if (weatherView == null) {
            if (location == null)
                location = Settings.getHomeData();

            Weather weather = Settings.getWeatherData(location.getQuery());
            if (weather != null && weather.isValid()) {
                weatherView = new WeatherNowViewModel(weather);
            }
        }

        if (binding.recyclerView.getItemDecorationCount() == 0)
            binding.recyclerView.addItemDecoration(new DividerItemDecoration(getAppCompatActivity(), DividerItemDecoration.VERTICAL));

        if (weatherView != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    binding.locationName.setText(weatherView.getLocation());
                    // specify an adapter (see also next example)
                    if (isHourly && weatherView.getExtras().getHourlyForecast().size() > 0)
                        binding.recyclerView.setAdapter(new WeatherDetailsAdapter<>(weatherView.getExtras().getHourlyForecast()));
                    else if (!isHourly) {
                        binding.recyclerView.setAdapter(new WeatherDetailsAdapter<>(weatherView.getForecasts()));
                    }

                    if (getArguments() != null) {
                        int scrollToPosition = getArguments().getInt(Constants.KEY_POSITION, 0);
                        layoutManager.scrollToPositionWithOffset(scrollToPosition, 0);
                    }
                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Save data
        outState.putBoolean(ARGS_ISHOURLY, isHourly);

        super.onSaveInstanceState(outState);
    }
}
