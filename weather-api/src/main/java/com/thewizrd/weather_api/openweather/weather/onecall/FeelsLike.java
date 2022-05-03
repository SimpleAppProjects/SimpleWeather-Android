package com.thewizrd.weather_api.openweather.weather.onecall;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class FeelsLike {

    @SerializedName("eve")
    private float eve;

    @SerializedName("night")
    private float night;

    @SerializedName("day")
    private float day;

    @SerializedName("morn")
    private float morn;

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