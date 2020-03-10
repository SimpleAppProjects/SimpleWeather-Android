package com.thewizrd.shared_resources.weatherdata.here;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class Rootobject {

    @SerializedName("alerts")
    private Alerts alerts;

    @SerializedName("hourlyForecasts")
    private HourlyForecasts hourlyForecasts;

    @SerializedName("metric")
    private boolean metric;

    @SerializedName("observations")
    private Observations observations;

    @SerializedName("astronomy")
    private Astronomy astronomy;

    @SerializedName("feedCreation")
    private String feedCreation;

    @SerializedName("dailyForecasts")
    private DailyForecasts dailyForecasts;

    @SerializedName("Type")
    private String type;

    @SerializedName("Message")
    private List<String> message;

    public void setAlerts(Alerts alerts) {
        this.alerts = alerts;
    }

    public Alerts getAlerts() {
        return alerts;
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