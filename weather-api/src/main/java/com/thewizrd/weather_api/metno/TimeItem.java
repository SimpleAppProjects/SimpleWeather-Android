package com.thewizrd.weather_api.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class TimeItem {

    @SerializedName("date")
    private String date;

    @SerializedName("moonposition")
    private Moonposition moonposition;

    @SerializedName("solarnoon")
    private Solarnoon solarnoon;

    @SerializedName("sunrise")
    private Sunrise sunrise;

    @SerializedName("moonphase")
    private Moonphase moonphase;

    @SerializedName("moonshadow")
    private Moonshadow moonshadow;

    @SerializedName("sunset")
    private Sunset sunset;

    @SerializedName("moonrise")
    private Moonrise moonrise;

    @SerializedName("solarmidnight")
    private Solarmidnight solarmidnight;

    @SerializedName("low_moon")
    private LowMoon lowMoon;

    @SerializedName("high_moon")
    private HighMoon highMoon;

    @SerializedName("moonset")
    private Moonset moonset;

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public void setMoonposition(Moonposition moonposition) {
        this.moonposition = moonposition;
    }

    public Moonposition getMoonposition() {
        return moonposition;
    }

    public void setSolarnoon(Solarnoon solarnoon) {
        this.solarnoon = solarnoon;
    }

    public Solarnoon getSolarnoon() {
        return solarnoon;
    }

    public void setSunrise(Sunrise sunrise) {
        this.sunrise = sunrise;
    }

    public Sunrise getSunrise() {
        return sunrise;
    }

    public void setMoonphase(Moonphase moonphase) {
        this.moonphase = moonphase;
    }

    public Moonphase getMoonphase() {
        return moonphase;
    }

    public void setMoonshadow(Moonshadow moonshadow) {
        this.moonshadow = moonshadow;
    }

    public Moonshadow getMoonshadow() {
        return moonshadow;
    }

    public void setSunset(Sunset sunset) {
        this.sunset = sunset;
    }

    public Sunset getSunset() {
        return sunset;
    }

    public void setMoonrise(Moonrise moonrise) {
        this.moonrise = moonrise;
    }

    public Moonrise getMoonrise() {
        return moonrise;
    }

    public void setSolarmidnight(Solarmidnight solarmidnight) {
        this.solarmidnight = solarmidnight;
    }

    public Solarmidnight getSolarmidnight() {
        return solarmidnight;
    }

    public void setLowMoon(LowMoon lowMoon) {
        this.lowMoon = lowMoon;
    }

    public LowMoon getLowMoon() {
        return lowMoon;
    }

    public void setHighMoon(HighMoon highMoon) {
        this.highMoon = highMoon;
    }

    public HighMoon getHighMoon() {
        return highMoon;
    }

    public Moonset getMoonset() {
        return moonset;
    }

    public void setMoonset(Moonset moonset) {
        this.moonset = moonset;
    }
}