package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class Alert {

    /* NWS Alerts */
    @SerializedName("type")
    private String type;

    @SerializedName("description")
    private String description;

    @SerializedName("date")
    private String date;

    @SerializedName("date_epoch")
    private String date_epoch;

    @SerializedName("expires")
    private String expires;

    @SerializedName("expires_epoch")
    private String expires_epoch;

    @SerializedName("message")
    private String message;

    @SerializedName("phenomena")
    private String phenomena;

    @SerializedName("significance")
    private String significance;

    /* Meteoalarm.eu Alerts */
    @SerializedName("wtype_meteoalarm")
    private String wtype_meteoalarm;

    @SerializedName("wtype_meteoalarm_name")
    private String wtype_meteoalarm_name;

    @SerializedName("level_meteoalarm")
    private String level_meteoalarm;

    @SerializedName("level_meteoalarm_name")
    private String level_meteoalarm_name;

    @SerializedName("level_meteoalarm_description")
    private String level_meteoalarm_description;

    @SerializedName("attribution")
    private String attribution;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate_epoch() {
        return date_epoch;
    }

    public void setDate_epoch(String date_epoch) {
        this.date_epoch = date_epoch;
    }

    public String getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
        this.expires = expires;
    }

    public String getExpires_epoch() {
        return expires_epoch;
    }

    public void setExpires_epoch(String expires_epoch) {
        this.expires_epoch = expires_epoch;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPhenomena() {
        return phenomena;
    }

    public void setPhenomena(String phenomena) {
        this.phenomena = phenomena;
    }

    public String getSignificance() {
        return significance;
    }

    public void setSignificance(String significance) {
        this.significance = significance;
    }

    public String getWtype_meteoalarm() {
        return wtype_meteoalarm;
    }

    public void setWtype_meteoalarm(String wtype_meteoalarm) {
        this.wtype_meteoalarm = wtype_meteoalarm;
    }

    public String getWtype_meteoalarm_name() {
        return wtype_meteoalarm_name;
    }

    public void setWtype_meteoalarm_name(String wtype_meteoalarm_name) {
        this.wtype_meteoalarm_name = wtype_meteoalarm_name;
    }

    public String getLevel_meteoalarm() {
        return level_meteoalarm;
    }

    public void setLevel_meteoalarm(String level_meteoalarm) {
        this.level_meteoalarm = level_meteoalarm;
    }

    public String getLevel_meteoalarm_name() {
        return level_meteoalarm_name;
    }

    public void setLevel_meteoalarm_name(String level_meteoalarm_name) {
        this.level_meteoalarm_name = level_meteoalarm_name;
    }

    public String getLevel_meteoalarm_description() {
        return level_meteoalarm_description;
    }

    public void setLevel_meteoalarm_description(String level_meteoalarm_description) {
        this.level_meteoalarm_description = level_meteoalarm_description;
    }

    public String getAttribution() {
        return attribution;
    }

    public void setAttribution(String attribution) {
        this.attribution = attribution;
    }
}
