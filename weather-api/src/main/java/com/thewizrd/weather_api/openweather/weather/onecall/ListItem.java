package com.thewizrd.weather_api.openweather.weather.onecall;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class ListItem {

    @Json(name = "dt")
    private long dt;

    @Json(name = "components")
    private Components components;

    @Json(name = "main")
    private Main main;

    public void setDt(long dt) {
        this.dt = dt;
    }

    public long getDt() {
        return dt;
    }

    public void setComponents(Components components) {
        this.components = components;
    }

    public Components getComponents() {
        return components;
    }

    public void setMain(Main main) {
        this.main = main;
    }

    public Main getMain() {
        return main;
    }
}