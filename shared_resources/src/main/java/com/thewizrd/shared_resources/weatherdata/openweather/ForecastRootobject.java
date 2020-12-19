package com.thewizrd.shared_resources.weatherdata.openweather;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

// Forecast / Daily
@UseStag(UseStag.FieldOption.ALL)
public class ForecastRootobject {

    @SerializedName("city")
    private City city;

    @SerializedName("list")
    private List<ListItem> list;

    public void setCity(City city) {
        this.city = city;
    }

    public City getCity() {
        return city;
    }

    public void setList(List<ListItem> list) {
        this.list = list;
    }

    public List<ListItem> getList() {
        return list;
    }
}