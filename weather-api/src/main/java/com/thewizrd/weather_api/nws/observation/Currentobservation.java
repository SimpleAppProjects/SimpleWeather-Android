package com.thewizrd.weather_api.nws.observation;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Currentobservation {

    @SerializedName("Weatherimage")
    private String weatherimage;

    @SerializedName("timezone")
    private String timezone;

    @SerializedName("latitude")
    private String latitude;

    @SerializedName("Windd")
    private String windd;

    @SerializedName("Altimeter")
    private String altimeter;

    @SerializedName("Relh")
    private String relh;

    @SerializedName("WindChill")
    private String windChill;

    @SerializedName("Gust")
    private String gust;

    @SerializedName("Date")
    private String date;

    @SerializedName("Winds")
    private String winds;

    @SerializedName("Weather")
    private String weather;

    @SerializedName("Temp")
    private String temp;

    @SerializedName("SLP")
    private String sLP;

    @SerializedName("elev")
    private String elev;

    @SerializedName("name")
    private String name;

    @SerializedName("Dewp")
    private String dewp;

    @SerializedName("Visibility")
    private String visibility;

    @SerializedName("id")
    private String id;

    @SerializedName("state")
    private String state;

    @SerializedName("longitude")
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