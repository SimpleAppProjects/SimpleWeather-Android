package com.thewizrd.weather_api.openweather.weather.onecall;

import com.google.gson.annotations.SerializedName;

public class Main {

    @SerializedName("aqi")
    private int aqi;

    public void setAqi(int aqi) {
        this.aqi = aqi;
    }

    public int getAqi() {
        return aqi;
    }
}