package com.thewizrd.shared_resources.weatherdata.weatheryahoo;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Atmosphere {

    @SerializedName("rising")
    private String rising;

    @SerializedName("visibility")
    private String visibility;

    @SerializedName("humidity")
    private String humidity;

    @SerializedName("pressure")
    private String pressure;

    public void setRising(String rising) {
        this.rising = rising;
    }

    public String getRising() {
        return rising;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getHumidity() {
        return humidity;
    }

    public void setPressure(String pressure) {
        this.pressure = pressure;
    }

    public String getPressure() {
        return pressure;
    }

}