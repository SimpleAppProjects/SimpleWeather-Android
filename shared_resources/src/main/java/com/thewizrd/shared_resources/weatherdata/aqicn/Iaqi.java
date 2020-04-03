package com.thewizrd.shared_resources.weatherdata.aqicn;

import com.google.gson.annotations.SerializedName;

public class Iaqi {

    @SerializedName("pm25")
    private Pm25 pm25;

    public void setPm25(Pm25 pm25) {
        this.pm25 = pm25;
    }

    public Pm25 getPm25() {
        return pm25;
    }
}