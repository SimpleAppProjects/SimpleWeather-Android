package com.thewizrd.simpleweather;

import com.thewizrd.shared_resources.adapters.WeatherAlertPanelAdapter;
import com.thewizrd.shared_resources.controls.WeatherAlertViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.Weather;

import java.util.List;

public class WeatherAlertsFragment extends WeatherListFragment {

    public static WeatherAlertsFragment newInstance(LocationData location) {
        WeatherAlertsFragment fragment = new WeatherAlertsFragment();
        if (location != null) {
            fragment.location = location;
        }
        return fragment;
    }

    public static WeatherAlertsFragment newInstance(LocationData location, WeatherNowViewModel weatherViewModel) {
        WeatherAlertsFragment fragment = new WeatherAlertsFragment();
        if (location != null && weatherViewModel != null) {
            fragment.location = location;
            fragment.weatherView = weatherViewModel;
        }
        return fragment;
    }

    @Override
    protected void initialize() {
        super.initialize();

        if (weatherView == null) {
            if (location == null)
                location = Settings.getHomeData();

            Weather weather = Settings.getWeatherData(location.getQuery());
            if (weather != null && weather.isValid()) {
                weather.setWeatherAlerts(Settings.getWeatherAlertData(location.getQuery()));
                weatherView = new WeatherNowViewModel(weather);
            }
        }

        if (weatherView != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    locationName.setText(weatherView.getLocation());

                    // specify an adapter (see also next example)
                    List<WeatherAlertViewModel> alerts = null;
                    if (weatherView.getExtras() != null && weatherView.getExtras().getAlerts() != null)
                        alerts = weatherView.getExtras().getAlerts();
                    recyclerView.setAdapter(new WeatherAlertPanelAdapter(alerts));
                }
            });
        }
    }
}
