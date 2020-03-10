package com.thewizrd.shared_resources.weatherdata.nws;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class ObservationStationsResponse {

    @SerializedName("observationStations")
    private List<String> observationStations;

    public void setObservationStations(List<String> observationStations) {
        this.observationStations = observationStations;
    }

    public List<String> getObservationStations() {
        return observationStations;
    }
}