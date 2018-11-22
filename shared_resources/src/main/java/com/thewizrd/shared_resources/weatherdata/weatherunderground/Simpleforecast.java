package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Simpleforecast {

    @SerializedName("forecastday")
    private List<Forecastday1> forecastday;

    public void setForecastday(List<Forecastday1> forecastday) {
        this.forecastday = forecastday;
    }

    public List<Forecastday1> getForecastday() {
        return forecastday;
    }
}