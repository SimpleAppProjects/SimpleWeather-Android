package com.thewizrd.shared_resources.remoteconfig;

import com.google.gson.annotations.SerializedName;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;

public final class WeatherProviderConfig {

    @SerializedName("locSource")
    @WeatherAPI.LocationProviders
    private String locSource;

    @SerializedName("newWeatherSource")
    @WeatherAPI.WeatherProviders
    private String newWeatherSource;

    @SerializedName("enabled")
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