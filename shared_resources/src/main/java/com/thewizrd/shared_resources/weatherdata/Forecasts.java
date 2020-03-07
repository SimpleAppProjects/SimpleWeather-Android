package com.thewizrd.shared_resources.weatherdata;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.List;

@Entity(tableName = "forecasts")
public class Forecasts {
    @PrimaryKey
    @NonNull
    private String query;
    @ColumnInfo(name = "forecastblob")
    private List<Forecast> forecast;
    @ColumnInfo(name = "txtforecastblob")
    private List<TextForecast> txtForecast;

    public Forecasts() {
    }

    @Ignore
    public Forecasts(@NonNull String query, List<Forecast> forecast, List<TextForecast> txtForecast) {
        this.query = query;
        this.forecast = forecast;
        this.txtForecast = txtForecast;
    }

    @NonNull
    public String getQuery() {
        return query;
    }

    public void setQuery(@NonNull String query) {
        this.query = query;
    }

    public List<Forecast> getForecast() {
        return forecast;
    }

    public void setForecast(List<Forecast> forecast) {
        this.forecast = forecast;
    }

    public List<TextForecast> getTxtForecast() {
        return txtForecast;
    }

    public void setTxtForecast(List<TextForecast> txtForecast) {
        this.txtForecast = txtForecast;
    }
}
