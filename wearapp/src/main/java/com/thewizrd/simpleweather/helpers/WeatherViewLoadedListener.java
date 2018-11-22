package com.thewizrd.simpleweather.helpers;

import com.thewizrd.shared_resources.controls.WeatherNowViewModel;

public interface WeatherViewLoadedListener {
    void onWeatherViewUpdated(WeatherNowViewModel weatherNowView);
}
