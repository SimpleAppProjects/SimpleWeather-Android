package com.thewizrd.shared_resources.weatherdata.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Collection;

@Entity(tableName = "weatheralerts")
public class WeatherAlerts {
    @PrimaryKey
    @NonNull
    private String query;
    @ColumnInfo(name = "weather_alerts")
    private Collection<WeatherAlert> alerts;

    @NonNull
    public String getQuery() {
        return query;
    }

    public void setQuery(@NonNull String query) {
        this.query = query;
    }

    @Nullable
    public Collection<WeatherAlert> getAlerts() {
        return alerts;
    }

    public void setAlerts(@Nullable Collection<WeatherAlert> alerts) {
        this.alerts = alerts;
    }

    public WeatherAlerts() {
    }

    @Ignore
    public WeatherAlerts(@NonNull String query, @Nullable Collection<WeatherAlert> alerts) {
        this.query = query;
        this.alerts = alerts;
    }
}
