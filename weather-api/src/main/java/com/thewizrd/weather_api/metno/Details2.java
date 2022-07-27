package com.thewizrd.weather_api.metno;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonClass;

@JsonClass(generateAdapter = true, generator = "java")
public class Details2 {

    @Json(name = "precipitation_amount")
    private Float precipitationAmount;

    @Json(name = "precipitation_amount_max")
    private Float precipitationAmountMax;

    @Json(name = "probability_of_thunder")
    private Float probabilityOfThunder;

    @Json(name = "precipitation_amount_min")
    private Float precipitationAmountMin;

    @Json(name = "probability_of_precipitation")
    private Float probabilityOfPrecipitation;

    public void setPrecipitationAmount(Float precipitationAmount) {
        this.precipitationAmount = precipitationAmount;
    }

    public Float getPrecipitationAmount() {
        return precipitationAmount;
    }

    public void setPrecipitationAmountMax(Float precipitationAmountMax) {
        this.precipitationAmountMax = precipitationAmountMax;
    }

    public Float getPrecipitationAmountMax() {
        return precipitationAmountMax;
    }

    public void setProbabilityOfThunder(Float probabilityOfThunder) {
        this.probabilityOfThunder = probabilityOfThunder;
    }

    public Float getProbabilityOfThunder() {
        return probabilityOfThunder;
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