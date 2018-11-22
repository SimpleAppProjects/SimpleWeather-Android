package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Rootobject {

    @SerializedName("alerts")
    private List<Alert> alerts;

    @SerializedName("sun_phase")
    private SunPhase sunPhase;

    @SerializedName("moon_phase")
    private MoonPhase moonPhase;

    @SerializedName("response")
    private Response response;

    @SerializedName("forecast")
    private Forecast forecast;

    @SerializedName("current_observation")
    private CurrentObservation currentObservation;

    @SerializedName("hourly_forecast")
    private List<HourlyForecastItem> hourlyForecast;

    @SerializedName("query_zone")
    private String queryZone;

    public void setAlerts(List<Alert> alerts) {
        this.alerts = alerts;
    }

    public List<Alert> getAlerts() {
        return alerts;
    }

    public void setSunPhase(SunPhase sunPhase) {
        this.sunPhase = sunPhase;
    }

    public SunPhase getSunPhase() {
        return sunPhase;
    }

    public void setMoonPhase(MoonPhase moonPhase) {
        this.moonPhase = moonPhase;
    }

    public MoonPhase getMoonPhase() {
        return moonPhase;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public Response getResponse() {
        return response;
    }

    public void setForecast(Forecast forecast) {
        this.forecast = forecast;
    }

    public Forecast getForecast() {
        return forecast;
    }

    public void setCurrentObservation(CurrentObservation currentObservation) {
        this.currentObservation = currentObservation;
    }

    public CurrentObservation getCurrentObservation() {
        return currentObservation;
    }

    public void setHourlyForecast(List<HourlyForecastItem> hourlyForecast) {
        this.hourlyForecast = hourlyForecast;
    }

    public List<HourlyForecastItem> getHourlyForecast() {
        return hourlyForecast;
    }

    public void setQueryZone(String queryZone) {
        this.queryZone = queryZone;
    }

    public String getQueryZone() {
        return queryZone;
    }
}