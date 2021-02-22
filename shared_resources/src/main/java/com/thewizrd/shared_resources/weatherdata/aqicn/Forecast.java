package com.thewizrd.shared_resources.weatherdata.aqicn;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Forecast {

    @SerializedName("daily")
    private Daily daily;

    public void setDaily(Daily daily) {
        this.daily = daily;
    }

    public Daily getDaily() {
        return daily;
    }
}