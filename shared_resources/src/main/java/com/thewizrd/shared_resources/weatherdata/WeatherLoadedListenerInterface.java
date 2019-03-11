package com.thewizrd.shared_resources.weatherdata;

import com.thewizrd.shared_resources.locationdata.LocationData;

public interface WeatherLoadedListenerInterface {
    void onWeatherLoaded(LocationData location, Weather weather);
}
