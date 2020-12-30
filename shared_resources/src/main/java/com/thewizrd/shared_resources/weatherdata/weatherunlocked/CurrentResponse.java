package com.thewizrd.shared_resources.weatherdata.weatherunlocked;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class CurrentResponse {

    @SerializedName("windspd_mph")
    private float windspdMph;

    @SerializedName("feelslike_c")
    private float feelslikeC;

    @SerializedName("feelslike_f")
    private float feelslikeF;

    @SerializedName("vis_mi")
    private float visMi;

    @SerializedName("lon")
    private float lon;

    @SerializedName("alt_ft")
    private float altFt;

    @SerializedName("temp_c")
    private float tempC;

    @SerializedName("slp_in")
    private float slpIn;

    @SerializedName("temp_f")
    private float tempF;

    @SerializedName("windspd_kts")
    private float windspdKts;

    @SerializedName("winddir_compass")
    private String winddirCompass;

    @SerializedName("winddir_deg")
    private float winddirDeg;

    @SerializedName("dewpoint_f")
    private float dewpointF;

    @SerializedName("wx_desc")
    private String wxDesc;

    @SerializedName("lat")
    private float lat;

    @SerializedName("alt_m")
    private float altM;

    @SerializedName("wx_icon")
    private String wxIcon;

    @SerializedName("windspd_kmh")
    private float windspdKmh;

    @SerializedName("humid_pct")
    private float humidPct;

    @SerializedName("dewpoint_c")
    private float dewpointC;

	/*
	@SerializedName("vis_desc")
	private Object visDesc;
	 */

    @SerializedName("cloudtotal_pct")
    private float cloudtotalPct;

    @SerializedName("vis_km")
    private float visKm;

    @SerializedName("windspd_ms")
    private float windspdMs;

    @SerializedName("slp_mb")
    private float slpMb;

    @SerializedName("wx_code")
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