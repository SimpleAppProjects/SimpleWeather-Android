package com.thewizrd.weather_api.nws.alerts;

import com.google.gson.annotations.SerializedName;

import java.util.List;

//@UseStag(UseStag.FieldOption.ALL)
public class Geocode {

    @SerializedName("UGC")
    private List<String> uGC;

    @SerializedName("SAME")
    private List<String> sAME;

    public void setUGC(List<String> uGC) {
        this.uGC = uGC;
    }

    public List<String> getUGC() {
        return uGC;
    }

    public void setSAME(List<String> sAME) {
        this.sAME = sAME;
    }

    public List<String> getSAME() {
        return sAME;
    }
}