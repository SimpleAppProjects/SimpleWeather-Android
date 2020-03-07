package com.thewizrd.shared_resources.weatherdata;

public final class WeatherRequest {
    private WeatherRequest() {
    }

    private boolean forceRefresh;
    private boolean loadAlerts;
    private boolean loadForecasts;
    private boolean forceLoadSavedData;

    public boolean isForceRefresh() {
        return forceRefresh;
    }

    public boolean isLoadAlerts() {
        return loadAlerts;
    }

    public boolean isLoadForecasts() {
        return loadForecasts;
    }

    public boolean isForceLoadSavedData() {
        return forceLoadSavedData;
    }

    public static final class Builder {
        private WeatherRequest request;

        public Builder() {
            request = new WeatherRequest();
        }

        public Builder forceRefresh(boolean value) {
            request.forceRefresh = value;
            return this;
        }

        public Builder loadAlerts() {
            request.loadAlerts = true;
            return this;
        }

        public Builder loadForecasts() {
            request.loadForecasts = true;
            return this;
        }

        public Builder forceLoadSavedData() {
            request.forceLoadSavedData = true;
            request.forceRefresh = false;
            return this;
        }

        public WeatherRequest build() {
            return request;
        }
    }
}
