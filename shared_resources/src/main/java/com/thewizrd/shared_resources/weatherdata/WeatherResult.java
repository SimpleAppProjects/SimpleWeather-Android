package com.thewizrd.shared_resources.weatherdata;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

public final class WeatherResult {
    private boolean isSavedData = false;
    private Weather weather = null;

    public boolean isSavedData() {
        return isSavedData;
    }

    @Nullable
    public Weather getWeather() {
        return weather;
    }

    private WeatherResult() {
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    static WeatherResult create(@Nullable Weather weather, boolean freshFromProvider) {
        WeatherResult result = new WeatherResult();
        result.isSavedData = !freshFromProvider;
        result.weather = weather;
        return result;
    }
}
