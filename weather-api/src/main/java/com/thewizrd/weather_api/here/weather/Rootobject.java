package com.thewizrd.weather_api.here.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true)
public class Rootobject {

    @Json(name = "alerts")
    private Alerts alerts;

    @Json(name = "nwsAlerts")
    private NwsAlerts nwsAlerts;

    @Json(name = "hourlyForecasts")
    private HourlyForecasts hourlyForecasts;

    @Json(name = "metric")
    private boolean metric;

    @Json(name = "observations")
    private Observations observations;

    @Json(name = "astronomy")
    private Astronomy astronomy;

    @Json(name = "feedCreation")
    private String feedCreation;

    @Json(name = "dailyForecasts")
    private DailyForecasts dailyForecasts;

    @Json(name = "Type")
    private String type;

    @Json(name = "Message")
    private List<String> message;

    public void setAlerts(Alerts alerts) {
        this.alerts = alerts;
    }

    public Alerts getAlerts() {
        return alerts;
    }

    public void setNwsAlerts(NwsAlerts nwsAlerts) {
        this.nwsAlerts = nwsAlerts;
    }

    public NwsAlerts getNwsAlerts() {
        return nwsAlerts;
    }

    public void setHourlyForecasts(HourlyForecasts hourlyForecasts) {
        this.hourlyForecasts = hourlyForecasts;
    }

    public HourlyForecasts getHourlyForecasts() {
        return hourlyForecasts;
    }

    public void setMetric(boolean metric) {
        this.metric = metric;
    }

    public boolean isMetric() {
        return metric;
    }

    public void setObservations(Observations observations) {
        this.observations = observations;
    }

    public Observations getObservations() {
        return observations;
    }

    public void setAstronomy(Astronomy astronomy) {
        this.astronomy = astronomy;
    }

    public Astronomy getAstronomy() {
        return astronomy;
    }

    public void setFeedCreation(String feedCreation) {
        this.feedCreation = feedCreation;
    }

    public String getFeedCreation() {
        return feedCreation;
    }

    public void setDailyForecasts(DailyForecasts dailyForecasts) {
        this.dailyForecasts = dailyForecasts;
    }

    public DailyForecasts getDailyForecasts() {
        return dailyForecasts;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getMessage() {
        return message;
    }

    public void setMessage(List<String> message) {
        this.message = message;
    }
}