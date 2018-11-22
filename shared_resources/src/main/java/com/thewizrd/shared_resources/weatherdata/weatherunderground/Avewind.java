package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class Avewind {

    @SerializedName("kph")
    private int kph;

    @SerializedName("mph")
    private int mph;

    @SerializedName("dir")
    private String dir;

    @SerializedName("degrees")
    private int degrees;

    public void setKph(int kph) {
        this.kph = kph;
    }

    public int getKph() {
        return kph;
    }

    public void setMph(int mph) {
        this.mph = mph;
    }

    public int getMph() {
        return mph;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public String getDir() {
        return dir;
    }

    public void setDegrees(int degrees) {
        this.degrees = degrees;
    }

    public int getDegrees() {
        return degrees;
    }
}