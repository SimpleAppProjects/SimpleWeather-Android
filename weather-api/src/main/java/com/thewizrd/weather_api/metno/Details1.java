package com.thewizrd.weather_api.metno;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class Details1 {

    @Json(name = "probability_of_precipitation")
    private Float probabilityOfPrecipitation;

    public void setProbabilityOfPrecipitation(Float probabilityOfPrecipitation) {
        this.probabilityOfPrecipitation = probabilityOfPrecipitation;
    }

    public Float getProbabilityOfPrecipitation() {
        return probabilityOfPrecipitation;
    }
}