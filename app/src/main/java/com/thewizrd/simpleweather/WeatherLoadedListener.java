package com.thewizrd.simpleweather;

public interface WeatherLoadedListener {
    void onWeatherLoaded(int locationIdx, Object weather);
}
