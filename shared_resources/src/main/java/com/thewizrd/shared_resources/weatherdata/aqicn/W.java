package com.thewizrd.shared_resources.weatherdata.aqicn;

import com.google.gson.annotations.SerializedName;

public class W {

    @SerializedName("v")
    private int V;

    public void setV(int V) {
        this.V = V;
    }

    public int getV() {
        return V;
    }
}