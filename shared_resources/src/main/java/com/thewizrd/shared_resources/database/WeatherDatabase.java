package com.thewizrd.shared_resources.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.WeatherDBConverters;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAlerts;

@Database(entities = {Weather.class, WeatherAlerts.class}, version = Settings.CURRENT_DBVERSION)
@TypeConverters({WeatherDBConverters.class})
public abstract class WeatherDatabase extends RoomDatabase {
    public abstract WeatherDAO weatherDAO();
}
