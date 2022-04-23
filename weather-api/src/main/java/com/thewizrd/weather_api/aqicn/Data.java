package com.thewizrd.weather_api.aqicn;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class Data {

	@SerializedName("iaqi")
	private Iaqi iaqi;

	@SerializedName("debug")
	private Debug debug;

	@SerializedName("city")
	private City city;

    @SerializedName("aqi")
    private int aqi;

    @SerializedName("forecast")
    private Forecast forecast;

	@SerializedName("time")
	private Time time;

	@SerializedName("idx")
	private int idx;

	@SerializedName("attributions")
	private List<AttributionsItem> attributions;

	@SerializedName("dominentpol")
	private String dominentpol;

	public void setIaqi(Iaqi iaqi){
		this.iaqi = iaqi;
	}

	public Iaqi getIaqi(){
		return iaqi;
	}

	public void setDebug(Debug debug){
		this.debug = debug;
	}

	public Debug getDebug(){
		return debug;
	}

	public void setCity(City city){
		this.city = city;
	}

	public City getCity(){
		return city;
	}

    public void setAqi(int aqi) {
        this.aqi = aqi;
    }

    public int getAqi() {
        return aqi;
    }

    public void setForecast(Forecast forecast) {
        this.forecast = forecast;
    }

    public Forecast getForecast() {
        return forecast;
    }

	public void setTime(Time time){
		this.time = time;
	}

	public Time getTime(){
		return time;
	}

	public void setIdx(int idx){
		this.idx = idx;
	}

	public int getIdx(){
		return idx;
	}

	public void setAttributions(List<AttributionsItem> attributions){
		this.attributions = attributions;
	}

	public List<AttributionsItem> getAttributions(){
		return attributions;
	}

	public void setDominentpol(String dominentpol){
		this.dominentpol = dominentpol;
	}

	public String getDominentpol(){
		return dominentpol;
	}
}