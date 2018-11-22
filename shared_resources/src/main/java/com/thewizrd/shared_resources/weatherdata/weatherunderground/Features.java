package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class Features {

    @SerializedName("alerts")
    private int alerts;

    @SerializedName("hourly")
    private int hourly;

    @SerializedName("astronomy")
    private int astronomy;

    @SerializedName("conditions")
    private int conditions;

    @SerializedName("forecast10day")
    private int forecast10day;

    public void setAlerts(int alerts) {
        this.alerts = alerts;
    }

    public int getAlerts() {
        return alerts;
    }

    public void setHourly(int hourly) {
        this.hourly = hourly;
    }

    public int getHourly() {
        return hourly;
    }

    public void setAstronomy(int astronomy) {
        this.astronomy = astronomy;
    }

    public int getAstronomy() {
        return astronomy;
    }

    public void setConditions(int conditions) {
        this.conditions = conditions;
    }

    public int getConditions() {
        return conditions;
    }

    public void setForecast10day(int forecast10day) {
        this.forecast10day = forecast10day;
    }

    public int getForecast10day() {
        return forecast10day;
    }
}