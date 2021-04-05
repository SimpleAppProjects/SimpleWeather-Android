package com.thewizrd.shared_resources.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.thewizrd.shared_resources.utils.SettingsManager;
import com.thewizrd.shared_resources.weatherdata.Forecasts;
import com.thewizrd.shared_resources.weatherdata.HourlyForecasts;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAlerts;

@Database(entities = {Weather.class, WeatherAlerts.class, Forecasts.class, HourlyForecasts.class}, version = SettingsManager.CURRENT_DBVERSION)
@TypeConverters({WeatherDBConverters.class})
public abstract class WeatherDatabase extends RoomDatabase {
    public abstract WeatherDAO weatherDAO();
}
