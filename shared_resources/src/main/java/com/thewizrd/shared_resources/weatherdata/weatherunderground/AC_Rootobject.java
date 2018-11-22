package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AC_Rootobject {

    @SerializedName("AC_RESULTS")
    private List<AC_RESULTS> results;

    public void setRESULTS(List<AC_RESULTS> results) {
        this.results = results;
    }

    public List<AC_RESULTS> getRESULTS() {
        return results;
    }
}