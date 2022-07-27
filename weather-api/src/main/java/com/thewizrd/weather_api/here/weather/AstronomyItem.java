package com.thewizrd.weather_api.here.weather;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class AstronomyItem {

    @Json(name = "moonset")
    private String moonset;

    @Json(name = "sunrise")
    private String sunrise;

    @Json(name = "moonPhaseDesc")
    private String moonPhaseDesc;

    @Json(name = "iconName")
    private String iconName;

    @Json(name = "city")
    private String city;

    @Json(name = "sunset")
    private String sunset;

    @Json(name = "latitude")
    private float latitude;

    @Json(name = "utcTime")
    private String utcTime;

    @Json(name = "moonrise")
    private String moonrise;

    @Json(name = "moonPhase")
    private float moonPhase;

    @Json(name = "longitude")
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