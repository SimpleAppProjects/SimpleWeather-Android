package com.thewizrd.weather_api.openweather.weather.onecall;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
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