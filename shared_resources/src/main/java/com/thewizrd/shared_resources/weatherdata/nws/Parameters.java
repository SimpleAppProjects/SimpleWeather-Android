package com.thewizrd.shared_resources.weatherdata.nws;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Parameters {

    @SerializedName("PIL")
    private List<String> pIL;

    @SerializedName("NWSheadline")
    private List<String> nWSheadline;

    @SerializedName("VTEC")
    private List<String> vTEC;

    @SerializedName("BLOCKCHANNEL")
    private List<String> bLOCKCHANNEL;

    @SerializedName("eventEndingTime")
    private List<String> eventEndingTime;

    public void setPIL(List<String> pIL) {
        this.pIL = pIL;
    }

    public List<String> getPIL() {
        return pIL;
    }

    public void setNWSheadline(List<String> nWSheadline) {
        this.nWSheadline = nWSheadline;
    }

    public List<String> getNWSheadline() {
        return nWSheadline;
    }

    public void setVTEC(List<String> vTEC) {
        this.vTEC = vTEC;
    }

    public List<String> getVTEC() {
        return vTEC;
    }

    public void setBLOCKCHANNEL(List<String> bLOCKCHANNEL) {
        this.bLOCKCHANNEL = bLOCKCHANNEL;
    }

    public List<String> getBLOCKCHANNEL() {
        return bLOCKCHANNEL;
    }

    public void setEventEndingTime(List<String> eventEndingTime) {
        this.eventEndingTime = eventEndingTime;
    }

    public List<String> getEventEndingTime() {
        return eventEndingTime;
    }
}