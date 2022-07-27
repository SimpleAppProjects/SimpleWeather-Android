package com.thewizrd.weather_api.tomorrow;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class Rootobject {

    @Json(name = "data")
    private Data data;

    //@Json(name = "warnings")
    //private List<WarningsItem> warnings;

    public void setData(Data data) {
        this.data = data;
    }

    public Data getData() {
        return data;
    }

	/*
	public void setWarnings(List<WarningsItem> warnings){
		this.warnings = warnings;
	}

	public List<WarningsItem> getWarnings(){
		return warnings;
	}
	 */
}