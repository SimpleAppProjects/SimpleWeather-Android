package com.thewizrd.shared_resources.weatherdata.metno;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class Details1 {

    @SerializedName("probability_of_precipitation")
    private Float probabilityOfPrecipitation;

    public void setProbabilityOfPrecipitation(Float probabilityOfPrecipitation) {
        this.probabilityOfPrecipitation = probabilityOfPrecipitation;
    }

    public Float getProbabilityOfPrecipitation() {
        return probabilityOfPrecipitation;
    }
}