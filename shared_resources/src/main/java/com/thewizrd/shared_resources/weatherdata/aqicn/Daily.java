package com.thewizrd.shared_resources.weatherdata.aqicn;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

import java.util.List;

@UseStag(UseStag.FieldOption.ALL)
public class Daily {

	/*
	@SerializedName("o3")
	private List<O3Item> o3;

	@SerializedName("pm25")
	private List<Pm25Item> pm25;

	@SerializedName("pm10")
	private List<Pm10Item> pm10;
	*/

    @SerializedName("uvi")
    private List<UviItem> uvi;

	/*
	public void setO3(List<O3Item> o3){
		this.o3 = o3;
	}

	public List<O3Item> getO3(){
		return o3;
	}

	public void setPm25(List<Pm25Item> pm25){
		this.pm25 = pm25;
	}

	public List<Pm25Item> getPm25(){
		return pm25;
	}

	public void setPm10(List<Pm10Item> pm10){
		this.pm10 = pm10;
	}

	public List<Pm10Item> getPm10(){
		return pm10;
	}
	*/

    public void setUvi(List<UviItem> uvi) {
        this.uvi = uvi;
    }

    public List<UviItem> getUvi() {
        return uvi;
    }
}