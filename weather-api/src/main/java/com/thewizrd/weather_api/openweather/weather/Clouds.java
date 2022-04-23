package com.thewizrd.weather_api.openweather.weather;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Clouds {

    @SerializedName("all")
    private int all;

    public void setAll(int all) {
        this.all = all;
    }

    public int getAll() {
        return all;
    }
}