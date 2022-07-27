package com.thewizrd.weather_api.openweather.weather.onecall;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class Temp {

    @Json(name = "min")
    private float min;

    @Json(name = "max")
    private float max;

    @Json(name = "eve")
    private float eve;

    @Json(name = "night")
    private float night;

    @Json(name = "day")
    private float day;

    @Json(name = "morn")
    private float morn;

    public float getMin() {
        return min;
    }

    public void setMin(float min) {
        this.min = min;
    }

    public float getMax() {
        return max;
    }

    public void setMax(float max) {
        this.max = max;
    }

    public float getEve() {
        return eve;
    }

    public void setEve(float eve) {
        this.eve = eve;
    }

    public float getNight() {
        return night;
    }

    public void setNight(float night) {
        this.night = night;
    }

    public float getDay() {
        return day;
    }

    public void setDay(float day) {
        this.day = day;
    }

    public float getMorn() {
        return morn;
    }

    public void setMorn(float morn) {
        this.morn = morn;
    }
}