package com.thewizrd.weather_api.weatherunlocked;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true, generator = "java")
public class ForecastResponse {

    @Json(name = "Days")
    private List<DaysItem> days;

    public void setDays(List<DaysItem> days) {
        this.days = days;
    }

    public List<DaysItem> getDays() {
        return days;
    }
}