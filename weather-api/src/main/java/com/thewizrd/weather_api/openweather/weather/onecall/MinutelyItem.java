package com.thewizrd.weather_api.openweather.weather.onecall;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class MinutelyItem {

    @SerializedName("dt")
    private long dt;

    @SerializedName("precipitation")
    private float precipitation;

    public void setDt(long dt) {
        this.dt = dt;
    }

    public long getDt() {
        return dt;
    }

    public void setPrecipitation(float precipitation) {
        this.precipitation = precipitation;
    }

    public float getPrecipitation() {
        return precipitation;
    }
}