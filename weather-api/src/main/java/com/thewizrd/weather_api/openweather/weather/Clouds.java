package com.thewizrd.weather_api.openweather.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class Clouds {

    @Json(name = "all")
    private int all;

    public void setAll(int all) {
        this.all = all;
    }

    public int getAll() {
        return all;
    }
}