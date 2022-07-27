package com.thewizrd.weather_api.nws.alerts;

import com.squareup.moshi.Json;

import java.util.List;

//@JsonClass(generateAdapter = true)
public class Geocode {

    @Json(name = "UGC")
    private List<String> uGC;

    @Json(name = "SAME")
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