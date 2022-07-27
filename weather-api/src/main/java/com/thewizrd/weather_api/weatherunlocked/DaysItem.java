package com.thewizrd.weather_api.weatherunlocked;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

import java.util.List;

@JsonClass(generateAdapter = true, generator = "java")
public class DaysItem {

    @Json(name = "date")
    private String date;

    @Json(name = "windgst_max_kts")
    private float windgstMaxKts;

    @Json(name = "sunset_time")
    private String sunsetTime;

    @Json(name = "snow_total_in")
    private float snowTotalIn;

    @Json(name = "rain_total_mm")
    private float rainTotalMm;

    @Json(name = "slp_max_mb")
    private float slpMaxMb;

    @Json(name = "rain_total_in")
    private float rainTotalIn;

    @Json(name = "windspd_max_kts")
    private float windspdMaxKts;

    @Json(name = "temp_max_f")
    private float tempMaxF;

    @Json(name = "snow_total_mm")
    private float snowTotalMm;

    @Json(name = "windspd_max_mph")
    private float windspdMaxMph;

    @Json(name = "windgst_max_ms")
    private float windgstMaxMs;

    @Json(name = "sunrise_time")
    private String sunriseTime;

    @Json(name = "windgst_max_mph")
    private float windgstMaxMph;

    @Json(name = "temp_min_f")
    private float tempMinF;

    @Json(name = "precip_total_mm")
    private float precipTotalMm;

    @Json(name = "slp_min_mb")
    private float slpMinMb;

    @Json(name = "prob_precip_pct")
    private float probPrecipPct;

    @Json(name = "temp_min_c")
    private float tempMinC;

    @Json(name = "windspd_max_ms")
    private float windspdMaxMs;

    @Json(name = "moonset_time")
    private String moonsetTime;

    @Json(name = "humid_max_pct")
    private float humidMaxPct;

    @Json(name = "precip_total_in")
    private float precipTotalIn;

    @Json(name = "windspd_max_kmh")
    private float windspdMaxKmh;

    @Json(name = "slp_min_in")
    private float slpMinIn;

    @Json(name = "Timeframes")
    private List<TimeframesItem> timeframes;

    @Json(name = "humid_min_pct")
    private float humidMinPct;

    @Json(name = "moonrise_time")
    private String moonriseTime;

    @Json(name = "temp_max_c")
    private float tempMaxC;

    @Json(name = "slp_max_in")
    private float slpMaxIn;

    @Json(name = "windgst_max_kmh")
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