package com.thewizrd.weather_api.openweather.weather.onecall;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true)
public class AirPollutionResponse {

    @Json(name = "coord")
    private Coord coord;

    @Json(name = "list")
    private List<ListItem> list;

    public void setCoord(Coord coord) {
        this.coord = coord;
    }

    public Coord getCoord() {
        return coord;
    }

    public void setList(List<ListItem> list) {
        this.list = list;
    }

    public List<ListItem> getList() {
        return list;
    }
}