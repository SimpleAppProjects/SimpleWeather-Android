package com.thewizrd.shared_resources.weatherdata.weatheryahoo;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Atmosphere {

    @SerializedName("rising")
    private int rising;

    @SerializedName("visibility")
    private float visibility;

    @SerializedName("humidity")
    private int humidity;

    @SerializedName("pressure")
    private float pressure;

    public void setRising(int rising) {
        this.rising = rising;
    }

    public int getRising() {
        return rising;
    }

    public void setVisibility(float visibility) {
        this.visibility = visibility;
    }

    public float getVisibility() {
        return visibility;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
    }

    public float getPressure() {
        return pressure;
    }
}