package com.thewizrd.weather_api.nws.alerts;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true, generator = "java")
public class Parameters {

    @Json(name = "PIL")
    private List<String> pIL;

    @Json(name = "NWSheadline")
    private List<String> nWSheadline;

    @Json(name = "VTEC")
    private List<String> vTEC;

    @Json(name = "BLOCKCHANNEL")
    private List<String> bLOCKCHANNEL;

    @Json(name = "eventEndingTime")
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