package com.thewizrd.shared_resources.remoteconfig;

import com.google.gson.annotations.SerializedName;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;

public final class WeatherProviderConfig {

    @SerializedName("locSource")
    @WeatherAPI.LocationAPIs
    private String locSource;

    @SerializedName("newWeatherSource")
    @WeatherAPI.WeatherAPIs
    private String newWeatherSource;

    @SerializedName("enabled")
    private boolean enabled;

    public void setLocSource(@WeatherAPI.LocationAPIs String locSource) {
        this.locSource = locSource;
    }

    @WeatherAPI.LocationAPIs
    public String getLocSource() {
        return locSource;
    }

    public void setNewWeatherSource(@WeatherAPI.WeatherAPIs String newWeatherSource) {
        this.newWeatherSource = newWeatherSource;
    }

    @WeatherAPI.WeatherAPIs
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