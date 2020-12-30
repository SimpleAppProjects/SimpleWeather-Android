package com.thewizrd.shared_resources.weatherdata;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.TypeConverters;

import com.thewizrd.shared_resources.database.SortableDateTimeConverters;

import java.time.ZonedDateTime;

@Entity(tableName = "hr_forecasts", primaryKeys = {"query", "dateblob"})
public class HourlyForecasts {
    @NonNull
    private String query;
    @NonNull
    @TypeConverters(SortableDateTimeConverters.class)
    @ColumnInfo(name = "dateblob")
    private ZonedDateTime date;
    @ColumnInfo(name = "hrforecastblob")
    private HourlyForecast hrForecast;

    public HourlyForecasts() {
    }

    public HourlyForecasts(@NonNull String query, @NonNull HourlyForecast forecast) {
        this.query = query;
        this.hrForecast = forecast;
        this.date = forecast.getDate();
    }

    @NonNull
    public String getQuery() {
        return query;
    }

    public void setQuery(@NonNull String query) {
        this.query = query;
    }

    @NonNull
    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(@NonNull ZonedDateTime date) {
        this.date = date;
    }

    @NonNull
    public HourlyForecast getHrForecast() {
        return hrForecast;
    }

    public void setHrForecast(@NonNull HourlyForecast hrForecast) {
        this.hrForecast = hrForecast;
    }
}
