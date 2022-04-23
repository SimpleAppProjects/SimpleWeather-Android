package com.thewizrd.weather_api.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Details2 {

    @SerializedName("precipitation_amount")
    private Float precipitationAmount;

    @SerializedName("precipitation_amount_max")
    private Float precipitationAmountMax;

    @SerializedName("probability_of_thunder")
    private Float probabilityOfThunder;

    @SerializedName("precipitation_amount_min")
    private Float precipitationAmountMin;

    @SerializedName("probability_of_precipitation")
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