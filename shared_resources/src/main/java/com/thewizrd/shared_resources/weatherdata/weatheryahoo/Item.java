package com.thewizrd.shared_resources.weatherdata.weatheryahoo;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Item {

    @SerializedName("condition")
    private Condition condition;

    @SerializedName("link")
    private String link;

    @SerializedName("description")
    private String description;

    @SerializedName("guid")
    private Guid guid;

    @SerializedName("forecast")
    private List<ForecastItem> forecast;

    @SerializedName("title")
    private String title;

    @SerializedName("pubDate")
    private String pubDate;

    @SerializedName("lat")
    private String lat;

    @SerializedName("long")
    private String _long;

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setGuid(Guid guid) {
        this.guid = guid;
    }

    public Guid getGuid() {
        return guid;
    }

    public void setForecast(List<ForecastItem> forecast) {
        this.forecast = forecast;
    }

    public List<ForecastItem> getForecast() {
        return forecast;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setPubDate(String pubDate) {
        this.pubDate = pubDate;
    }

    public String getPubDate() {
        return pubDate;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLat() {
        return lat;
    }

    public void setLong(String _long) {
        this._long = _long;
    }

    public String getLong() {
        return _long;
    }
}