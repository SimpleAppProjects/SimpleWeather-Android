package com.thewizrd.shared_resources.locationdata.weatherunderground;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class AC_Rootobject {

    @SerializedName("RESULTS")
    private List<AC_RESULTS> results;

    public void setResults(List<AC_RESULTS> results) {
        this.results = results;
    }

    public List<AC_RESULTS> getResults() {
        return results;
    }
}