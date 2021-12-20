package com.thewizrd.shared_resources.weatherdata.aqicn;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class P {

    @SerializedName("v")
    private double V;

    public void setV(double V) {
        this.V = V;
    }

    public double getV() {
        return V;
    }
}