package com.thewizrd.weather_api.here.location;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class ResultItem {

    @Json(name = "relevance")
    private double relevance;

    @Json(name = "matchLevel")
    private String matchLevel;

    @Json(name = "matchQuality")
    private MatchQuality matchQuality;

    @Json(name = "matchType")
    private String matchType;

    @Json(name = "distance")
    private double distance;

    @Json(name = "location")
    private Location location;

    public void setRelevance(double relevance) {
        this.relevance = relevance;
    }

    public double getRelevance() {
        return relevance;
    }

    public void setMatchLevel(String matchLevel) {
        this.matchLevel = matchLevel;
    }

    public String getMatchLevel() {
        return matchLevel;
    }

    public void setMatchQuality(MatchQuality matchQuality) {
        this.matchQuality = matchQuality;
    }

    public MatchQuality getMatchQuality() {
        return matchQuality;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getDistance() {
        return distance;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }
}