package com.thewizrd.shared_resources.weatherdata.openweather;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

// Forecast / Daily
@UseStag(UseStag.FieldOption.ALL)
public class ForecastRootobject {

    @SerializedName("city")
    private City city;

    @SerializedName("cnt")
    private int cnt;

    @SerializedName("cod")
    private String cod;

    @SerializedName("message")
    private String message;

    @SerializedName("list")
    private List<ListItem> list;

    public void setCity(City city) {
        this.city = city;
    }

    public City getCity() {
        return city;
    }

    public void setCnt(int cnt) {
        this.cnt = cnt;
    }

    public int getCnt() {
        return cnt;
    }

    public void setCod(String cod) {
        this.cod = cod;
    }

    public String getCod() {
        return cod;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setList(List<ListItem> list) {
        this.list = list;
    }

    public List<ListItem> getList() {
        return list;
    }
}