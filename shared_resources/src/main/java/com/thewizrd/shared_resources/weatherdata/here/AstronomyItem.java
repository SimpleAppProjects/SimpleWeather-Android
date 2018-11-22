package com.thewizrd.shared_resources.weatherdata.here;

import com.google.gson.annotations.SerializedName;

public class AstronomyItem {

    @SerializedName("moonset")
    private String moonset;

    @SerializedName("sunrise")
    private String sunrise;

    @SerializedName("moonPhaseDesc")
    private String moonPhaseDesc;

    @SerializedName("iconName")
    private String iconName;

    @SerializedName("city")
    private String city;

    @SerializedName("sunset")
    private String sunset;

    @SerializedName("latitude")
    private float latitude;

    @SerializedName("utcTime")
    private String utcTime;

    @SerializedName("moonrise")
    private String moonrise;

    @SerializedName("moonPhase")
    private float moonPhase;

    @SerializedName("longitude")
    private float longitude;

    public void setMoonset(String moonset) {
        this.moonset = moonset;
    }

    public String getMoonset() {
        return moonset;
    }

    public void setSunrise(String sunrise) {
        this.sunrise = sunrise;
    }

    public String getSunrise() {
        return sunrise;
    }

    public void setMoonPhaseDesc(String moonPhaseDesc) {
        this.moonPhaseDesc = moonPhaseDesc;
    }

    public String getMoonPhaseDesc() {
        return moonPhaseDesc;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public String getIconName() {
        return iconName;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCity() {
        return city;
    }

    public void setSunset(String sunset) {
        this.sunset = sunset;
    }

    public String getSunset() {
        return sunset;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setUtcTime(String utcTime) {
        this.utcTime = utcTime;
    }

    public String getUtcTime() {
        return utcTime;
    }

    public void setMoonrise(String moonrise) {
        this.moonrise = moonrise;
    }

    public String getMoonrise() {
        return moonrise;
    }

    public void setMoonPhase(float moonPhase) {
        this.moonPhase = moonPhase;
    }

    public float getMoonPhase() {
        return moonPhase;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public float getLongitude() {
        return longitude;
    }
}