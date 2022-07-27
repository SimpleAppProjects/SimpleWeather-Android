package com.thewizrd.weather_api.openweather.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class WeatherItem {

    @Json(name = "icon")
    private String icon;

    @Json(name = "description")
    private String description;

    @Json(name = "main")
    private String main;

    @Json(name = "id")
    private int id;

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setMain(String main) {
        this.main = main;
    }

    public String getMain() {
        return main;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}