package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class MoonPhase {

    @SerializedName("moonset")
    private Moonset moonset;

    @SerializedName("sunrise")
    private Sunrise sunrise;

    @SerializedName("sunset")
    private Sunset sunset;

    @SerializedName("phaseofMoon")
    private String phaseofMoon;

    @SerializedName("ageOfMoon")
    private String ageOfMoon;

    @SerializedName("hemisphere")
    private String hemisphere;

    @SerializedName("moonrise")
    private Moonrise moonrise;

    @SerializedName("percentIlluminated")
    private String percentIlluminated;

    @SerializedName("current_time")
    private CurrentTime currentTime;

    public void setMoonset(Moonset moonset) {
        this.moonset = moonset;
    }

    public Moonset getMoonset() {
        return moonset;
    }

    public void setSunrise(Sunrise sunrise) {
        this.sunrise = sunrise;
    }

    public Sunrise getSunrise() {
        return sunrise;
    }

    public void setSunset(Sunset sunset) {
        this.sunset = sunset;
    }

    public Sunset getSunset() {
        return sunset;
    }

    public void setPhaseofMoon(String phaseofMoon) {
        this.phaseofMoon = phaseofMoon;
    }

    public String getPhaseofMoon() {
        return phaseofMoon;
    }

    public void setAgeOfMoon(String ageOfMoon) {
        this.ageOfMoon = ageOfMoon;
    }

    public String getAgeOfMoon() {
        return ageOfMoon;
    }

    public void setHemisphere(String hemisphere) {
        this.hemisphere = hemisphere;
    }

    public String getHemisphere() {
        return hemisphere;
    }

    public void setMoonrise(Moonrise moonrise) {
        this.moonrise = moonrise;
    }

    public Moonrise getMoonrise() {
        return moonrise;
    }

    public void setPercentIlluminated(String percentIlluminated) {
        this.percentIlluminated = percentIlluminated;
    }

    public String getPercentIlluminated() {
        return percentIlluminated;
    }

    public void setCurrentTime(CurrentTime currentTime) {
        this.currentTime = currentTime;
    }

    public CurrentTime getCurrentTime() {
        return currentTime;
    }
}