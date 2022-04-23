package com.thewizrd.common.weatherdata;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.thewizrd.shared_resources.weatherdata.model.Weather;

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

    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    static WeatherResult create(@Nullable Weather weather, boolean freshFromProvider) {
        WeatherResult result = new WeatherResult();
        result.isSavedData = !freshFromProvider;
        result.weather = weather;
        return result;
    }
}
