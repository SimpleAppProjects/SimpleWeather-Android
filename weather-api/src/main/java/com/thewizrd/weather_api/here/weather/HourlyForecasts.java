package com.thewizrd.weather_api.here.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class HourlyForecasts {

    @Json(name = "forecastLocation")
    private ForecastLocation1 forecastLocation;

    public void setForecastLocation(ForecastLocation1 forecastLocation) {
        this.forecastLocation = forecastLocation;
    }

    public ForecastLocation1 getForecastLocation() {
        return forecastLocation;
    }
}