package com.thewizrd.weather_api.weatherunlocked;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class CurrentResponse {

    @Json(name = "windspd_mph")
    private float windspdMph;

    @Json(name = "feelslike_c")
    private float feelslikeC;

    @Json(name = "feelslike_f")
    private float feelslikeF;

    @Json(name = "vis_mi")
    private float visMi;

    @Json(name = "lon")
    private float lon;

    @Json(name = "alt_ft")
    private float altFt;

    @Json(name = "temp_c")
    private float tempC;

    @Json(name = "slp_in")
    private float slpIn;

    @Json(name = "temp_f")
    private float tempF;

    @Json(name = "windspd_kts")
    private float windspdKts;

    @Json(name = "winddir_compass")
    private String winddirCompass;

    @Json(name = "winddir_deg")
    private float winddirDeg;

    @Json(name = "dewpoint_f")
    private float dewpointF;

    @Json(name = "wx_desc")
    private String wxDesc;

    @Json(name = "lat")
    private float lat;

    @Json(name = "alt_m")
    private float altM;

    @Json(name = "wx_icon")
    private String wxIcon;

    @Json(name = "windspd_kmh")
    private float windspdKmh;

    @Json(name = "humid_pct")
    private float humidPct;

    @Json(name = "dewpoint_c")
    private float dewpointC;

	/*
	@Json(name = "vis_desc")
	private Object visDesc;
	 */

    @Json(name = "cloudtotal_pct")
    private float cloudtotalPct;

    @Json(name = "vis_km")
    private float visKm;

    @Json(name = "windspd_ms")
    private float windspdMs;

    @Json(name = "slp_mb")
    private float slpMb;

    @Json(name = "wx_code")
    private int wxCode;

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

    public void setLon(float lon) {
        this.lon = lon;
    }

    public float getLon() {
        return lon;
    }

    public void setAltFt(float altFt) {
        this.altFt = altFt;
    }

    public float getAltFt() {
        return altFt;
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

    public void setLat(float lat) {
        this.lat = lat;
    }

    public float getLat() {
        return lat;
    }

    public void setAltM(float altM) {
        this.altM = altM;
    }

    public float getAltM() {
        return altM;
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

	/*
	public void setVisDesc(Object visDesc){
		this.visDesc = visDesc;
	}

	public Object getVisDesc(){
		return visDesc;
	}
	 */

    public void setCloudtotalPct(float cloudtotalPct) {
        this.cloudtotalPct = cloudtotalPct;
    }

    public float getCloudtotalPct() {
        return cloudtotalPct;
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