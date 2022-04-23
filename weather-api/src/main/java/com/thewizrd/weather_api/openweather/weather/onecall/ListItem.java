package com.thewizrd.weather_api.openweather.weather.onecall;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class ListItem {

    @SerializedName("dt")
    private long dt;

    @SerializedName("components")
    private Components components;

    @SerializedName("main")
    private Main main;

    public void setDt(long dt) {
        this.dt = dt;
    }

    public long getDt() {
        return dt;
    }

    public void setComponents(Components components) {
        this.components = components;
    }

    public Components getComponents() {
        return components;
    }

    public void setMain(Main main) {
        this.main = main;
    }

    public Main getMain() {
        return main;
    }
}