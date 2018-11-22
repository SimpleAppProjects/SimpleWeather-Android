package com.thewizrd.shared_resources.weatherdata;

public interface WeatherLoadedListenerInterface {
    void onWeatherLoaded(LocationData location, Weather weather);
}
