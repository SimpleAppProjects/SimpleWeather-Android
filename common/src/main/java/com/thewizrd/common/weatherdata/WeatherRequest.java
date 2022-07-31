package com.thewizrd.common.weatherdata;

public final class WeatherRequest {
    private WeatherRequest() {
    }

    private boolean forceRefresh;
    private boolean loadAlerts;
    private boolean loadForecasts;
    private boolean forceLoadSavedData;
    private boolean shouldSaveData = true;

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

    public boolean isShouldSaveData() {
        return shouldSaveData;
    }

    public static final class Builder {
        private final WeatherRequest request;

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
            request.forceRefresh = false;
            request.forceLoadSavedData = true;
            request.shouldSaveData = false;
            return this;
        }

        public Builder forceRefreshWithoutSave() {
            request.forceRefresh = true;
            request.forceLoadSavedData = false;
            request.shouldSaveData = false;
            return this;
        }

        public WeatherRequest build() {
            return request;
        }
    }
}
