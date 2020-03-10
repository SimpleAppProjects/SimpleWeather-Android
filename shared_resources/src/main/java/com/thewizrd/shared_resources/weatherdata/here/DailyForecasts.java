package com.thewizrd.shared_resources.weatherdata.here;

import com.google.gson.annotations.SerializedName;
import com.vimeo.stag.UseStag;

@UseStag(UseStag.FieldOption.ALL)
public class DailyForecasts {

    @SerializedName("forecastLocation")
    private ForecastLocation forecastLocation;

    public void setForecastLocation(ForecastLocation forecastLocation) {
        this.forecastLocation = forecastLocation;
    }

    public ForecastLocation getForecastLocation() {
        return forecastLocation;
    }
}