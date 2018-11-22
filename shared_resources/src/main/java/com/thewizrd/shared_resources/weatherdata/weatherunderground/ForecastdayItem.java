package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class ForecastdayItem {

    @SerializedName("icon_url")
    private String iconUrl;

    @SerializedName("fcttext")
    private String fcttext;

    @SerializedName("pop")
    private String pop;

    @SerializedName("period")
    private int period;

    @SerializedName("icon")
    private String icon;

    @SerializedName("title")
    private String title;

    @SerializedName("fcttext_metric")
    private String fcttextMetric;

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setFcttext(String fcttext) {
        this.fcttext = fcttext;
    }

    public String getFcttext() {
        return fcttext;
    }

    public void setPop(String pop) {
        this.pop = pop;
    }

    public String getPop() {
        return pop;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public int getPeriod() {
        return period;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setFcttextMetric(String fcttextMetric) {
        this.fcttextMetric = fcttextMetric;
    }

    public String getFcttextMetric() {
        return fcttextMetric;
    }
}