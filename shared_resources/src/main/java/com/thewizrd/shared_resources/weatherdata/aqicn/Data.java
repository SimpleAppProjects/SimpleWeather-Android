package com.thewizrd.shared_resources.weatherdata.aqicn;

import com.google.gson.annotations.SerializedName;

public class Data {

    @SerializedName("iaqi")
    private Iaqi iaqi;

    @SerializedName("city")
    private City city;

    @SerializedName("aqi")
    private int aqi;

    @SerializedName("time")
    private Time time;

    @SerializedName("idx")
    private int idx;

    public void setIaqi(Iaqi iaqi) {
        this.iaqi = iaqi;
    }

    public Iaqi getIaqi() {
        return iaqi;
    }

    public void setCity(City city) {
        this.city = city;
    }

    public City getCity() {
        return city;
    }

    public void setAqi(int aqi) {
        this.aqi = aqi;
    }

    public int getAqi() {
        return aqi;
    }

    public void setTime(Time time) {
        this.time = time;
    }

    public Time getTime() {
        return time;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public int getIdx() {
        return idx;
    }
}