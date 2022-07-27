package com.thewizrd.weather_api.metno;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class Details3 {

    @Json(name = "air_temperature_max")
    private Float airTemperatureMax;

    @Json(name = "precipitation_amount")
    private Float precipitationAmount;

    @Json(name = "air_temperature_min")
    private Float airTemperatureMin;

    @Json(name = "precipitation_amount_max")
    private Float precipitationAmountMax;

    @Json(name = "precipitation_amount_min")
    private Float precipitationAmountMin;

    @Json(name = "probability_of_precipitation")
    private Float probabilityOfPrecipitation;

    public void setAirTemperatureMax(Float airTemperatureMax) {
        this.airTemperatureMax = airTemperatureMax;
    }

    public Float getAirTemperatureMax() {
        return airTemperatureMax;
    }

    public void setPrecipitationAmount(Float precipitationAmount) {
        this.precipitationAmount = precipitationAmount;
    }

    public Float getPrecipitationAmount() {
        return precipitationAmount;
    }

    public void setAirTemperatureMin(Float airTemperatureMin) {
        this.airTemperatureMin = airTemperatureMin;
    }

    public Float getAirTemperatureMin() {
        return airTemperatureMin;
    }

    public void setPrecipitationAmountMax(Float precipitationAmountMax) {
        this.precipitationAmountMax = precipitationAmountMax;
    }

    public Float getPrecipitationAmountMax() {
        return precipitationAmountMax;
    }

    public void setPrecipitationAmountMin(Float precipitationAmountMin) {
        this.precipitationAmountMin = precipitationAmountMin;
    }

    public Float getPrecipitationAmountMin() {
        return precipitationAmountMin;
    }

    public void setProbabilityOfPrecipitation(Float probabilityOfPrecipitation) {
        this.probabilityOfPrecipitation = probabilityOfPrecipitation;
    }

    public Float getProbabilityOfPrecipitation() {
        return probabilityOfPrecipitation;
    }
}