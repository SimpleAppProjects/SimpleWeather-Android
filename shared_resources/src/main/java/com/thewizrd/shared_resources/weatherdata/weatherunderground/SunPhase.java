package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class SunPhase {

    @SerializedName("sunrise")
    private Sunrise sunrise;

    @SerializedName("sunset")
    private Sunset sunset;

    public void setSunrise(Sunrise sunrise) {
        this.sunrise = sunrise;
    }

    public Sunrise getSunrise() {
        return sunrise;
    }

    public void setSunset(Sunset sunset) {
        this.sunset = sunset;
    }

    public Sunset getSunset() {
        return sunset;
    }
}