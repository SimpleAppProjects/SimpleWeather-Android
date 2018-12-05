package com.thewizrd.shared_resources.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAlerts;

import java.util.List;

@Dao
public interface WeatherDAO {
    /* WeatherData methods */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertWeatherData(Weather weather);

    @Delete
    public void deleteWeatherData(Weather weather);

    @Query("DELETE FROM weatherdata WHERE `query` = :key")
    public void deleteWeatherDataByKey(String key);

    @Transaction
    @Query("SELECT * FROM weatherdata")
    public List<Weather> loadAllWeatherData();

    @Query("SELECT * FROM weatherdata WHERE `query` = :query")
    public Weather getWeatherData(String query);

    @Query("SELECT COUNT(*) FROM weatherdata")
    public int getWeatherDataCount();

    /* WeatherAlertData methods */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertWeatherAlertData(WeatherAlerts alert);

    @Delete
    public void deleteWeatherAlertData(WeatherAlerts alert);

    @Query("DELETE FROM weatheralerts WHERE `query` = :key")
    public void deleteWeatherAlertDataByKey(String key);

    @Transaction
    @Query("SELECT * FROM weatheralerts")
    public List<WeatherAlerts> loadAllWeatherAlertData();

    @Query("SELECT * FROM weatheralerts WHERE `query` = :query")
    public WeatherAlerts getWeatherAlertData(String query);

    @Query("SELECT COUNT(*) FROM weatheralerts")
    public int getWeatherAlertDataCount();
}
