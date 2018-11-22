package com.thewizrd.shared_resources.weatherdata.here;

import com.google.gson.annotations.SerializedName;

public class HourlyForecasts {

    @SerializedName("forecastLocation")
    private ForecastLocation1 forecastLocation;

    public void setForecastLocation(ForecastLocation1 forecastLocation) {
        this.forecastLocation = forecastLocation;
    }

    public ForecastLocation1 getForecastLocation() {
        return forecastLocation;
    }
}