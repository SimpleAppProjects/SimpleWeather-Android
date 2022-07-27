package com.thewizrd.weather_api.openweather.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

// Forecast / Daily
@JsonClass(generateAdapter = true)
public class ForecastRootobject {

    @Json(name = "city")
    private City city;

    @Json(name = "list")
    private List<ListItem> list;

    public void setCity(City city) {
        this.city = city;
    }

    public City getCity() {
        return city;
    }

    public void setList(List<ListItem> list) {
        this.list = list;
    }

    public List<ListItem> getList() {
        return list;
    }
}