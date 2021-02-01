package com.thewizrd.shared_resources.weatherdata.weatheryahoo;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class Rootobject {

    @SerializedName("location")
    private Location location;

    @SerializedName("current_observation")
    private CurrentObservation currentObservation;

    @SerializedName("forecasts")
    private List<ForecastsItem> forecasts;

    public void setLocation(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public void setCurrentObservation(CurrentObservation currentObservation) {
        this.currentObservation = currentObservation;
    }

    public CurrentObservation getCurrentObservation() {
        return currentObservation;
    }

    public void setForecasts(List<ForecastsItem> forecasts) {
        this.forecasts = forecasts;
    }

    public List<ForecastsItem> getForecasts() {
        return forecasts;
    }
}