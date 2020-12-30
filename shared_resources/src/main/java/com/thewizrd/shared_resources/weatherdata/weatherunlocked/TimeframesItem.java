package com.thewizrd.shared_resources.weatherdata.weatherunlocked;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class TimeframesItem {

    @SerializedName("date")
    private String date;

    @SerializedName("windspd_mph")
    private float windspdMph;

    @SerializedName("feelslike_c")
    private float feelslikeC;

    @SerializedName("feelslike_f")
    private float feelslikeF;

    @SerializedName("vis_mi")
    private float visMi;

    @SerializedName("cloud_low_pct")
    private float cloudLowPct;

    @SerializedName("temp_c")
    private float tempC;

    @SerializedName("slp_in")
    private float slpIn;

    @SerializedName("temp_f")
    private float tempF;

    @SerializedName("windspd_kts")
    private float windspdKts;

    @SerializedName("windgst_kts")
    private float windgstKts;

    @SerializedName("snow_in")
    private float snowIn;

    @SerializedName("winddir_compass")
    private String winddirCompass;

    @SerializedName("winddir_deg")
    private float winddirDeg;

    @SerializedName("snow_accum_in")
    private float snowAccumIn;

    @SerializedName("dewpoint_f")
    private float dewpointF;

    @SerializedName("wx_desc")
    private String wxDesc;

    @SerializedName("windgst_mph")
    private float windgstMph;

    @SerializedName("snow_accum_cm")
    private float snowAccumCm;

    @SerializedName("utcdate")
    private String utcdate;

    @SerializedName("wx_icon")
    private String wxIcon;

    @SerializedName("windspd_kmh")
    private float windspdKmh;

    @SerializedName("prob_precip_pct")
    private String probPrecipPct;

    @SerializedName("cloud_mid_pct")
    private float cloudMidPct;

    @SerializedName("utctime")
    private int utctime;

    @SerializedName("humid_pct")
    private float humidPct;

    @SerializedName("dewpoint_c")
    private float dewpointC;

    @SerializedName("snow_mm")
    private float snowMm;

    @SerializedName("precip_in")
    private float precipIn;

    @SerializedName("rain_mm")
    private float rainMm;

    @SerializedName("precip_mm")
    private float precipMm;

    @SerializedName("cloud_high_pct")
    private float cloudHighPct;

    @SerializedName("cloudtotal_pct")
    private float cloudtotalPct;

    @SerializedName("windgst_ms")
    private float windgstMs;

    @SerializedName("rain_in")
    private float rainIn;

    @SerializedName("vis_km")
    private float visKm;

    @SerializedName("windspd_ms")
    private float windspdMs;

    @SerializedName("time")
    private int time;

    @SerializedName("windgst_kmh")
    private float windgstKmh;

    @SerializedName("slp_mb")
    private float slpMb;

    @SerializedName("wx_code")
    private int wxCode;

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public void setWindspdMph(float windspdMph) {
        this.windspdMph = windspdMph;
    }

    public float getWindspdMph() {
        return windspdMph;
    }

    public void setFeelslikeC(float feelslikeC) {
        this.feelslikeC = feelslikeC;
    }

    public float getFeelslikeC() {
        return feelslikeC;
    }

    public void setFeelslikeF(float feelslikeF) {
        this.feelslikeF = feelslikeF;
    }

    public float getFeelslikeF() {
        return feelslikeF;
    }

    public void setVisMi(float visMi) {
        this.visMi = visMi;
    }

    public float getVisMi() {
        return visMi;
    }

    public void setCloudLowPct(float cloudLowPct) {
        this.cloudLowPct = cloudLowPct;
    }

    public float getCloudLowPct() {
        return cloudLowPct;
    }

    public void setTempC(float tempC) {
        this.tempC = tempC;
    }

    public float getTempC() {
        return tempC;
    }

    public void setSlpIn(float slpIn) {
        this.slpIn = slpIn;
    }

    public float getSlpIn() {
        return slpIn;
    }

    public void setTempF(float tempF) {
        this.tempF = tempF;
    }

    public float getTempF() {
        return tempF;
    }

    public void setWindspdKts(float windspdKts) {
        this.windspdKts = windspdKts;
    }

    public float getWindspdKts() {
        return windspdKts;
    }

    public void setWindgstKts(float windgstKts) {
        this.windgstKts = windgstKts;
    }

    public float getWindgstKts() {
        return windgstKts;
    }

    public void setSnowIn(float snowIn) {
        this.snowIn = snowIn;
    }

    public float getSnowIn() {
        return snowIn;
    }

    public void setWinddirCompass(String winddirCompass) {
        this.winddirCompass = winddirCompass;
    }

    public String getWinddirCompass() {
        return winddirCompass;
    }

    public void setWinddirDeg(float winddirDeg) {
        this.winddirDeg = winddirDeg;
    }

    public float getWinddirDeg() {
        return winddirDeg;
    }

    public void setSnowAccumIn(float snowAccumIn) {
        this.snowAccumIn = snowAccumIn;
    }

    public float getSnowAccumIn() {
        return snowAccumIn;
    }

    public void setDewpointF(float dewpointF) {
        this.dewpointF = dewpointF;
    }

    public float getDewpointF() {
        return dewpointF;
    }

    public void setWxDesc(String wxDesc) {
        this.wxDesc = wxDesc;
    }

    public String getWxDesc() {
        return wxDesc;
    }

    public void setWindgstMph(float windgstMph) {
        this.windgstMph = windgstMph;
    }

    public float getWindgstMph() {
        return windgstMph;
    }

    public void setSnowAccumCm(float snowAccumCm) {
        this.snowAccumCm = snowAccumCm;
    }

    public float getSnowAccumCm() {
        return snowAccumCm;
    }

    public void setUtcdate(String utcdate) {
        this.utcdate = utcdate;
    }

    public String getUtcdate() {
        return utcdate;
    }

    public void setWxIcon(String wxIcon) {
        this.wxIcon = wxIcon;
    }

    public String getWxIcon() {
        return wxIcon;
    }

    public void setWindspdKmh(float windspdKmh) {
        this.windspdKmh = windspdKmh;
    }

    public float getWindspdKmh() {
        return windspdKmh;
    }

    public void setProbPrecipPct(String probPrecipPct) {
        this.probPrecipPct = probPrecipPct;
    }

    public String getProbPrecipPct() {
        return probPrecipPct;
    }

    public void setCloudMidPct(float cloudMidPct) {
        this.cloudMidPct = cloudMidPct;
    }

    public float getCloudMidPct() {
        return cloudMidPct;
    }

    public void setUtctime(int utctime) {
        this.utctime = utctime;
    }

    public int getUtctime() {
        return utctime;
    }

    public void setHumidPct(float humidPct) {
        this.humidPct = humidPct;
    }

    public float getHumidPct() {
        return humidPct;
    }

    public void setDewpointC(float dewpointC) {
        this.dewpointC = dewpointC;
    }

    public float getDewpointC() {
        return dewpointC;
    }

    public void setSnowMm(float snowMm) {
        this.snowMm = snowMm;
    }

    public float getSnowMm() {
        return snowMm;
    }

    public void setPrecipIn(float precipIn) {
        this.precipIn = precipIn;
    }

    public float getPrecipIn() {
        return precipIn;
    }

    public void setRainMm(float rainMm) {
        this.rainMm = rainMm;
    }

    public float getRainMm() {
        return rainMm;
    }

    public void setPrecipMm(float precipMm) {
        this.precipMm = precipMm;
    }

    public float getPrecipMm() {
        return precipMm;
    }

    public void setCloudHighPct(float cloudHighPct) {
        this.cloudHighPct = cloudHighPct;
    }

    public float getCloudHighPct() {
        return cloudHighPct;
    }

    public void setCloudtotalPct(float cloudtotalPct) {
        this.cloudtotalPct = cloudtotalPct;
    }

    public float getCloudtotalPct() {
        return cloudtotalPct;
    }

    public void setWindgstMs(float windgstMs) {
        this.windgstMs = windgstMs;
    }

    public float getWindgstMs() {
        return windgstMs;
    }

    public void setRainIn(float rainIn) {
        this.rainIn = rainIn;
    }

    public float getRainIn() {
        return rainIn;
    }

    public void setVisKm(float visKm) {
        this.visKm = visKm;
    }

    public float getVisKm() {
        return visKm;
    }

    public void setWindspdMs(float windspdMs) {
        this.windspdMs = windspdMs;
    }

    public float getWindspdMs() {
        return windspdMs;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public int getTime() {
        return time;
    }

    public void setWindgstKmh(float windgstKmh) {
        this.windgstKmh = windgstKmh;
    }

    public float getWindgstKmh() {
        return windgstKmh;
    }

    public void setSlpMb(float slpMb) {
        this.slpMb = slpMb;
    }

    public float getSlpMb() {
        return slpMb;
    }

    public void setWxCode(int wxCode) {
        this.wxCode = wxCode;
    }

    public int getWxCode() {
        return wxCode;
    }
}