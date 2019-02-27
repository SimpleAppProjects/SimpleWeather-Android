package com.thewizrd.shared_resources.locationdata.here;

import com.google.gson.annotations.SerializedName;

public class ResultItem {

    @SerializedName("relevance")
    private double relevance;

    @SerializedName("matchLevel")
    private String matchLevel;

    @SerializedName("matchQuality")
    private MatchQuality matchQuality;

    @SerializedName("matchType")
    private String matchType;

    @SerializedName("distance")
    private double distance;

    @SerializedName("location")
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