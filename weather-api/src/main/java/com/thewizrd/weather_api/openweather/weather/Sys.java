package com.thewizrd.weather_api.openweather.weather;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Sys {

    @SerializedName("country")
    private String country;

    @SerializedName("sunrise")
    private int sunrise;

    @SerializedName("sunset")
    private int sunset;

    @SerializedName("id")
    private int id;

    @SerializedName("type")
    private int type;

    @SerializedName("message")
    private String message;

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountry() {
        return country;
    }

    public void setSunrise(int sunrise) {
        this.sunrise = sunrise;
    }

    public int getSunrise() {
        return sunrise;
    }

    public void setSunset(int sunset) {
        this.sunset = sunset;
    }

    public int getSunset() {
        return sunset;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}