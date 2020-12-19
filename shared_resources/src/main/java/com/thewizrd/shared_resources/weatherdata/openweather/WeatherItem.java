package com.thewizrd.shared_resources.weatherdata.openweather;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class WeatherItem {

    @SerializedName("icon")
    private String icon;

    @SerializedName("description")
    private String description;

    @SerializedName("main")
    private String main;

    @SerializedName("id")
    private int id;

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setMain(String main) {
        this.main = main;
    }

    public String getMain() {
        return main;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}