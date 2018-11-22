package com.thewizrd.shared_resources.weatherdata.weatherunderground;

import com.google.gson.annotations.SerializedName;

public class Forecast {

    @SerializedName("simpleforecast")
    private Simpleforecast simpleforecast;

    @SerializedName("txt_forecast")
    private TxtForecast txtForecast;

    public void setSimpleforecast(Simpleforecast simpleforecast) {
        this.simpleforecast = simpleforecast;
    }

    public Simpleforecast getSimpleforecast() {
        return simpleforecast;
    }

    public void setTxtForecast(TxtForecast txtForecast) {
        this.txtForecast = txtForecast;
    }

    public TxtForecast getTxtForecast() {
        return txtForecast;
    }
}