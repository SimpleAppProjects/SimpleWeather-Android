package com.thewizrd.weather_api.openweather.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class City {

    @Json(name = "country")
    private String country;

    @Json(name = "coord")
    private Coord coord;

    @Json(name = "name")
    private String name;

    @Json(name = "id")
    private int id;

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    public void setCoord(Coord coord) {
        this.coord = coord;
    }

    public Coord getCoord() {
        return coord;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}