package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class Wdir {

    @SerializedName("dir")
    private String dir;

    @SerializedName("degrees")
    private String degrees;

    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getDir() {
        return dir;
    }

    public void setDegrees(String degrees) {
        this.degrees = degrees;
    }

    public String getDegrees() {
        return degrees;
    }
}