package com.thewizrd.shared_resources.weatherdata.aqicn;

import com.google.gson.annotations.SerializedName;

public class Time {

    @SerializedName("s")
    private String S;

    @SerializedName("v")
    private int V;

    @SerializedName("tz")
    private String tz;

    public void setS(String S) {
        this.S = S;
    }

    public String getS() {
        return S;
    }

    public void setV(int V) {
        this.V = V;
    }

    public int getV() {
        return V;
    }

    public void setTz(String tz) {
        this.tz = tz;
    }

    public String getTz() {
        return tz;
    }
}