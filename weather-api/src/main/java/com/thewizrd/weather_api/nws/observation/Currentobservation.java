package com.thewizrd.weather_api.nws.observation;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true)
public class Currentobservation {

    @Json(name = "Weatherimage")
    private String weatherimage;

    @Json(name = "timezone")
    private String timezone;

    @Json(name = "latitude")
    private String latitude;

    @Json(name = "Windd")
    private String windd;

    @Json(name = "Altimeter")
    private String altimeter;

    @Json(name = "Relh")
    private String relh;

    @Json(name = "WindChill")
    private String windChill;

    @Json(name = "Gust")
    private String gust;

    @Json(name = "Date")
    private String date;

    @Json(name = "Winds")
    private String winds;

    @Json(name = "Weather")
    private String weather;

    @Json(name = "Temp")
    private String temp;

    @Json(name = "SLP")
    private String sLP;

    @Json(name = "elev")
    private String elev;

    @Json(name = "name")
    private String name;

    @Json(name = "Dewp")
    private String dewp;

    @Json(name = "Visibility")
    private String visibility;

    @Json(name = "id")
    private String id;

    @Json(name = "state")
    private String state;

    @Json(name = "longitude")
    private String longitude;

    public void setWeatherimage(String weatherimage) {
        this.weatherimage = weatherimage;
    }

    public String getWeatherimage() {
        return weatherimage;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setWindd(String windd) {
        this.windd = windd;
    }

    public String getWindd() {
        return windd;
    }

    public void setAltimeter(String altimeter) {
        this.altimeter = altimeter;
    }

    public String getAltimeter() {
        return altimeter;
    }

    public void setRelh(String relh) {
        this.relh = relh;
    }

    public String getRelh() {
        return relh;
    }

    public void setWindChill(String windChill) {
        this.windChill = windChill;
    }

    public String getWindChill() {
        return windChill;
    }

    public void setGust(String gust) {
        this.gust = gust;
    }

    public String getGust() {
        return gust;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public void setWinds(String winds) {
        this.winds = winds;
    }

    public String getWinds() {
        return winds;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public String getWeather() {
        return weather;
    }

    public void setTemp(String temp) {
        this.temp = temp;
    }

    public String getTemp() {
        return temp;
    }

    public void setSLP(String sLP) {
        this.sLP = sLP;
    }

    public String getSLP() {
        return sLP;
    }

    public void setElev(String elev) {
        this.elev = elev;
    }

    public String getElev() {
        return elev;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setDewp(String dewp) {
        this.dewp = dewp;
    }

    public String getDewp() {
        return dewp;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLongitude() {
        return longitude;
    }
}