package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TxtForecast {

    @SerializedName("date")
    private String date;

    @SerializedName("forecastday")
    private List<ForecastdayItem> forecastday;

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public void setForecastday(List<ForecastdayItem> forecastday) {
        this.forecastday = forecastday;
    }

    public List<ForecastdayItem> getForecastday() {
        return forecastday;
    }
}