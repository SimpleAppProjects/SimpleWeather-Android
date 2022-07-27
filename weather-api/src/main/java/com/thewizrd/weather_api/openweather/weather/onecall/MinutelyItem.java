package com.thewizrd.weather_api.openweather.weather.onecall;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class MinutelyItem {

    @Json(name = "dt")
    private long dt;

    @Json(name = "precipitation")
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