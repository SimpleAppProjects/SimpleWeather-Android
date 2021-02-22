package com.thewizrd.shared_resources.weatherdata.aqicn;

import com.google.gson.annotations.SerializedName;

public class So2 {

    @SerializedName("v")
    private double V;

    public void setV(double V) {
        this.V = V;
    }

    public double getV() {
        return V;
    }
}