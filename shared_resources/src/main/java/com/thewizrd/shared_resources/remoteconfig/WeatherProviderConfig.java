package com.thewizrd.shared_resources.remoteconfig;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;

@JsonClass(generateAdapter = true, generator = "java")
public final class WeatherProviderConfig {

    @Json(name = "locSource")
    @WeatherAPI.LocationProviders
    private String locSource;

    @Json(name = "newWeatherSource")
    @WeatherAPI.WeatherProviders
    private String newWeatherSource;

    @Json(name = "enabled")
    private boolean enabled;

    public void setLocSource(@WeatherAPI.LocationProviders String locSource) {
        this.locSource = locSource;
    }

    @WeatherAPI.LocationProviders
    public String getLocSource() {
        return locSource;
    }

    public void setNewWeatherSource(@WeatherAPI.WeatherProviders String newWeatherSource) {
        this.newWeatherSource = newWeatherSource;
    }

    @WeatherAPI.WeatherProviders
    public String getNewWeatherSource() {
        return newWeatherSource;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}