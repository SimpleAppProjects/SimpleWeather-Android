package com.thewizrd.weather_api.openweather.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class Wind {

    @Json(name = "deg")
    private float deg;

    @Json(name = "speed")
    private float speed;

    @Json(name = "gust")
    private Float gust;

    public void setDeg(float deg) {
        this.deg = deg;
    }

    public float getDeg() {
        return deg;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getSpeed() {
        return speed;
    }

    public void setGust(Float gust) {
        this.gust = gust;
    }

    public Float getGust() {
        return gust;
    }
}