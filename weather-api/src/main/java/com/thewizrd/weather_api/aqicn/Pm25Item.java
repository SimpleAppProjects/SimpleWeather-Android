package com.thewizrd.weather_api.aqicn;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Pm25Item {

    @SerializedName("avg")
    private int avg;

    @SerializedName("min")
    private int min;

    @SerializedName("max")
    private int max;

    @SerializedName("day")
    private String day;

    public void setAvg(int avg) {
        this.avg = avg;
    }

    public int getAvg() {
        return avg;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMin() {
        return min;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getMax() {
        return max;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getDay() {
        return day;
    }
}