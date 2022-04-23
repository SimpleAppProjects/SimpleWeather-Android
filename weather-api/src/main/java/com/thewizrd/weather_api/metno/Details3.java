package com.thewizrd.weather_api.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Details3 {

    @SerializedName("air_temperature_max")
    private Float airTemperatureMax;

    @SerializedName("precipitation_amount")
    private Float precipitationAmount;

    @SerializedName("air_temperature_min")
    private Float airTemperatureMin;

    @SerializedName("precipitation_amount_max")
    private Float precipitationAmountMax;

    @SerializedName("precipitation_amount_min")
    private Float precipitationAmountMin;

    @SerializedName("probability_of_precipitation")
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