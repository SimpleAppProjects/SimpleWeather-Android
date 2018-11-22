package com.thewizrd.shared_resources.weatherdata;

import com.thewizrd.shared_resources.utils.WeatherException;

public interface WeatherErrorListenerInterface {
    void onWeatherError(WeatherException wEx);
}
