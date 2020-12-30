package com.thewizrd.shared_resources.weatherdata.weatherunlocked;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class DaysItem {

    @SerializedName("date")
    private String date;

    @SerializedName("windgst_max_kts")
    private float windgstMaxKts;

    @SerializedName("sunset_time")
    private String sunsetTime;

    @SerializedName("snow_total_in")
    private float snowTotalIn;

    @SerializedName("rain_total_mm")
    private float rainTotalMm;

    @SerializedName("slp_max_mb")
    private float slpMaxMb;

    @SerializedName("rain_total_in")
    private float rainTotalIn;

    @SerializedName("windspd_max_kts")
    private float windspdMaxKts;

    @SerializedName("temp_max_f")
    private float tempMaxF;

    @SerializedName("snow_total_mm")
    private float snowTotalMm;

    @SerializedName("windspd_max_mph")
    private float windspdMaxMph;

    @SerializedName("windgst_max_ms")
    private float windgstMaxMs;

    @SerializedName("sunrise_time")
    private String sunriseTime;

    @SerializedName("windgst_max_mph")
    private float windgstMaxMph;

    @SerializedName("temp_min_f")
    private float tempMinF;

    @SerializedName("precip_total_mm")
    private float precipTotalMm;

    @SerializedName("slp_min_mb")
    private float slpMinMb;

    @SerializedName("prob_precip_pct")
    private float probPrecipPct;

    @SerializedName("temp_min_c")
    private float tempMinC;

    @SerializedName("windspd_max_ms")
    private float windspdMaxMs;

    @SerializedName("moonset_time")
    private String moonsetTime;

    @SerializedName("humid_max_pct")
    private float humidMaxPct;

    @SerializedName("precip_total_in")
    private float precipTotalIn;

    @SerializedName("windspd_max_kmh")
    private float windspdMaxKmh;

    @SerializedName("slp_min_in")
    private float slpMinIn;

    @SerializedName("Timeframes")
    private List<TimeframesItem> timeframes;

    @SerializedName("humid_min_pct")
    private float humidMinPct;

    @SerializedName("moonrise_time")
    private String moonriseTime;

    @SerializedName("temp_max_c")
    private float tempMaxC;

    @SerializedName("slp_max_in")
    private float slpMaxIn;

    @SerializedName("windgst_max_kmh")
    private float windgstMaxKmh;

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public void setWindgstMaxKts(float windgstMaxKts) {
        this.windgstMaxKts = windgstMaxKts;
    }

    public float getWindgstMaxKts() {
        return windgstMaxKts;
    }

    public void setSunsetTime(String sunsetTime) {
        this.sunsetTime = sunsetTime;
    }

    public String getSunsetTime() {
        return sunsetTime;
    }

    public void setSnowTotalIn(float snowTotalIn) {
        this.snowTotalIn = snowTotalIn;
    }

    public float getSnowTotalIn() {
        return snowTotalIn;
    }

    public void setRainTotalMm(float rainTotalMm) {
        this.rainTotalMm = rainTotalMm;
    }

    public float getRainTotalMm() {
        return rainTotalMm;
    }

    public void setSlpMaxMb(float slpMaxMb) {
        this.slpMaxMb = slpMaxMb;
    }

    public float getSlpMaxMb() {
        return slpMaxMb;
    }

    public void setRainTotalIn(float rainTotalIn) {
        this.rainTotalIn = rainTotalIn;
    }

    public float getRainTotalIn() {
        return rainTotalIn;
    }

    public void setWindspdMaxKts(float windspdMaxKts) {
        this.windspdMaxKts = windspdMaxKts;
    }

    public float getWindspdMaxKts() {
        return windspdMaxKts;
    }

    public void setTempMaxF(float tempMaxF) {
        this.tempMaxF = tempMaxF;
    }

    public float getTempMaxF() {
        return tempMaxF;
    }

    public void setSnowTotalMm(float snowTotalMm) {
        this.snowTotalMm = snowTotalMm;
    }

    public float getSnowTotalMm() {
        return snowTotalMm;
    }

    public void setWindspdMaxMph(float windspdMaxMph) {
        this.windspdMaxMph = windspdMaxMph;
    }

    public float getWindspdMaxMph() {
        return windspdMaxMph;
    }

    public void setWindgstMaxMs(float windgstMaxMs) {
        this.windgstMaxMs = windgstMaxMs;
    }

    public float getWindgstMaxMs() {
        return windgstMaxMs;
    }

    public void setSunriseTime(String sunriseTime) {
        this.sunriseTime = sunriseTime;
    }

    public String getSunriseTime() {
        return sunriseTime;
    }

    public void setWindgstMaxMph(float windgstMaxMph) {
        this.windgstMaxMph = windgstMaxMph;
    }

    public float getWindgstMaxMph() {
        return windgstMaxMph;
    }

    public void setTempMinF(float tempMinF) {
        this.tempMinF = tempMinF;
    }

    public float getTempMinF() {
        return tempMinF;
    }

    public void setPrecipTotalMm(float precipTotalMm) {
        this.precipTotalMm = precipTotalMm;
    }

    public float getPrecipTotalMm() {
        return precipTotalMm;
    }

    public void setSlpMinMb(float slpMinMb) {
        this.slpMinMb = slpMinMb;
    }

    public float getSlpMinMb() {
        return slpMinMb;
    }

    public void setProbPrecipPct(float probPrecipPct) {
        this.probPrecipPct = probPrecipPct;
    }

    public float getProbPrecipPct() {
        return probPrecipPct;
    }

    public void setTempMinC(float tempMinC) {
        this.tempMinC = tempMinC;
    }

    public float getTempMinC() {
        return tempMinC;
    }

    public void setWindspdMaxMs(float windspdMaxMs) {
        this.windspdMaxMs = windspdMaxMs;
    }

    public float getWindspdMaxMs() {
        return windspdMaxMs;
    }

    public void setMoonsetTime(String moonsetTime) {
        this.moonsetTime = moonsetTime;
    }

    public String getMoonsetTime() {
        return moonsetTime;
    }

    public void setHumidMaxPct(float humidMaxPct) {
        this.humidMaxPct = humidMaxPct;
    }

    public float getHumidMaxPct() {
        return humidMaxPct;
    }

    public void setPrecipTotalIn(float precipTotalIn) {
        this.precipTotalIn = precipTotalIn;
    }

    public float getPrecipTotalIn() {
        return precipTotalIn;
    }

    public void setWindspdMaxKmh(float windspdMaxKmh) {
        this.windspdMaxKmh = windspdMaxKmh;
    }

    public float getWindspdMaxKmh() {
        return windspdMaxKmh;
    }

    public void setSlpMinIn(float slpMinIn) {
        this.slpMinIn = slpMinIn;
    }

    public float getSlpMinIn() {
        return slpMinIn;
    }

    public void setTimeframes(List<TimeframesItem> timeframes) {
        this.timeframes = timeframes;
    }

    public List<TimeframesItem> getTimeframes() {
        return timeframes;
    }

    public void setHumidMinPct(float humidMinPct) {
        this.humidMinPct = humidMinPct;
    }

    public float getHumidMinPct() {
        return humidMinPct;
    }

    public void setMoonriseTime(String moonriseTime) {
        this.moonriseTime = moonriseTime;
    }

    public String getMoonriseTime() {
        return moonriseTime;
    }

    public void setTempMaxC(float tempMaxC) {
        this.tempMaxC = tempMaxC;
    }

    public float getTempMaxC() {
        return tempMaxC;
    }

    public void setSlpMaxIn(float slpMaxIn) {
        this.slpMaxIn = slpMaxIn;
    }

    public float getSlpMaxIn() {
        return slpMaxIn;
    }

    public void setWindgstMaxKmh(float windgstMaxKmh) {
        this.windgstMaxKmh = windgstMaxKmh;
    }

    public float getWindgstMaxKmh() {
        return windgstMaxKmh;
    }
}