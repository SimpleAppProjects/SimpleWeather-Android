package com.thewizrd.weather_api.openweather.weather.onecall;

import com.squareup.moshi.Json;

public class Main {

    @Json(name = "aqi")
    private int aqi;

    public void setAqi(int aqi) {
        this.aqi = aqi;
    }

    public int getAqi() {
        return aqi;
    }
}