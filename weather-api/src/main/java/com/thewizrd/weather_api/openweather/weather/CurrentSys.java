package com.thewizrd.weather_api.openweather.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class CurrentSys {

    @Json(name = "country")
    private String country;

    @Json(name = "sunrise")
    private Long sunrise;

    @Json(name = "sunset")
    private Long sunset;

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    public void setSunrise(Long sunrise) {
        this.sunrise = sunrise;
    }

    public Long getSunrise() {
        return sunrise;
    }

    public void setSunset(Long sunset) {
        this.sunset = sunset;
    }

    public Long getSunset() {
        return sunset;
    }
}