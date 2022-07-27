package com.thewizrd.weather_api.here.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class DailyForecasts {

    @Json(name = "forecastLocation")
    private ForecastLocation forecastLocation;

    public void setForecastLocation(ForecastLocation forecastLocation) {
        this.forecastLocation = forecastLocation;
    }

    public ForecastLocation getForecastLocation() {
        return forecastLocation;
    }
}