package com.thewizrd.shared_resources.weatherdata.openweather;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Wind {

    @SerializedName("deg")
    private float deg;

    @SerializedName("speed")
    private float speed;

    @SerializedName("gust")
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