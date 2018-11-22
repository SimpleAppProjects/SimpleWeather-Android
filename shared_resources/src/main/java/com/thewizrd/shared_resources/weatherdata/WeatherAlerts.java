package com.thewizrd.shared_resources.weatherdata;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import java.util.List;

@Entity(tableName = "weatheralerts")
public class WeatherAlerts {
    @PrimaryKey
    @NonNull
    private String query;
    @ColumnInfo(name = "weather_alerts")
    private List<WeatherAlert> alerts;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<WeatherAlert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<WeatherAlert> alerts) {
        this.alerts = alerts;
    }

    public WeatherAlerts() {
    }

    @Ignore
    public WeatherAlerts(String query, List<WeatherAlert> alerts) {
        this.query = query;
        this.alerts = alerts;
    }
}
