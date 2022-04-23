package com.thewizrd.weather_api.here.weather;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
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