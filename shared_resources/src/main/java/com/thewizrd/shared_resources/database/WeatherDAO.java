package com.thewizrd.shared_resources.database;

import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.TypeConverters;

import com.thewizrd.shared_resources.weatherdata.Forecasts;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.HourlyForecasts;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAlerts;

import java.time.ZonedDateTime;
import java.util.Collection;
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

    @Query("DELETE FROM weatherdata WHERE `query` NOT IN (:keyQuery)")
    public void deleteWeatherDataByKeyNotIn(List<String> keyQuery);

    @Transaction
    @Query("SELECT * FROM weatherdata")
    public List<Weather> loadAllWeatherData();

    @Query("SELECT * FROM weatherdata WHERE `query` = :query")
    public Weather getWeatherData(String query);

    @Query("SELECT * FROM weatherdata WHERE `locationblob` LIKE :searchQuery LIMIT 1")
    public Weather getWeatherDataByCoord(String searchQuery);

    @Query("SELECT COUNT(*) FROM weatherdata")
    public int getWeatherDataCount();

    @Query("SELECT COUNT(*) FROM weatherdata WHERE `query` = :query")
    public int getWeatherDataCountByKey(String query);

    /* WeatherAlertData methods */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertWeatherAlertData(WeatherAlerts alert);

    @Delete
    public void deleteWeatherAlertData(WeatherAlerts alert);

    @Query("DELETE FROM weatheralerts WHERE `query` = :key")
    public void deleteWeatherAlertDataByKey(String key);

    @Query("DELETE FROM weatheralerts WHERE `query` NOT IN (:keyQuery)")
    public void deleteWeatherAlertDataByKeyNotIn(List<String> keyQuery);

    @Transaction
    @Query("SELECT * FROM weatheralerts")
    public List<WeatherAlerts> loadAllWeatherAlertData();

    @Query("SELECT * FROM weatheralerts WHERE `query` = :query")
    public WeatherAlerts getWeatherAlertData(String query);

    @Query("SELECT * FROM weatheralerts WHERE `query` = :query")
    public LiveData<WeatherAlerts> getLiveWeatherAlertData(String query);

    @Query("SELECT COUNT(*) FROM weatheralerts")
    public int getWeatherAlertDataCount();

    /* Forecasts methods */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertForecast(Forecasts forecast);

    @Delete
    public void deleteForecast(Forecasts forecast);

    @Query("DELETE FROM forecasts WHERE `query` = :key")
    public void deleteForecastByKey(String key);

    @Query("DELETE FROM forecasts WHERE `query` NOT IN (:keyQuery)")
    public void deleteForecastByKeyNotIn(List<String> keyQuery);

    @Transaction
    @Query("SELECT * FROM forecasts")
    public List<Forecasts> loadAllForecasts();

    @Query("SELECT * FROM forecasts WHERE `query` = :query")
    public Forecasts getForecastData(String query);

    @Query("SELECT * FROM forecasts WHERE `query` = :query")
    public LiveData<Forecasts> getLiveForecastData(String query);

    @Query("SELECT COUNT(*) FROM forecasts GROUP BY `query`")
    public int getForecastDataCountGroupedByQuery();

    //

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertAllHourlyForecasts(Collection<HourlyForecasts> forecasts);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertHourlyForecast(HourlyForecasts forecast);

    @Delete
    public void deleteHourlyForecast(HourlyForecasts forecast);

    @Query("DELETE FROM hr_forecasts WHERE `query` = :key")
    public void deleteHourlyForecastByKey(String key);

    @Query("DELETE FROM hr_forecasts WHERE `query` NOT IN (:keyQuery)")
    public void deleteHourlyForecastByKeyNotIn(List<String> keyQuery);

    @Transaction
    @Query("SELECT `hrforecastblob` FROM hr_forecasts WHERE `query` = :key ORDER BY `dateblob`")
    public List<HourlyForecast> getHourlyForecastsByQueryOrderByDate(String key);

    @Transaction
    @Query("SELECT `hrforecastblob` FROM hr_forecasts WHERE `query` = :key ORDER BY `dateblob` LIMIT :loadSize")
    public List<HourlyForecast> getHourlyForecastsByQueryOrderByDateByLimit(String key, int loadSize);

    @Transaction
    @Query("SELECT `hrforecastblob` FROM hr_forecasts WHERE `query` = :key AND `dateblob` >= :date ORDER BY `dateblob` LIMIT :loadSize")
    public List<HourlyForecast> getHourlyForecastsByQueryOrderByDateByLimitFilterByDate(String key, int loadSize, @TypeConverters(SortableDateTimeConverters.class) ZonedDateTime date);

    @Transaction
    @Query("SELECT `hrforecastblob` FROM hr_forecasts WHERE `query` = :key AND `dateblob` >= :date ORDER BY `dateblob` LIMIT :loadSize OFFSET :offset")
    public List<HourlyForecast> getHourlyForecastsByQueryOrderByDateByLimitByOffsetFilterByDate(String key, int loadSize, int offset, @TypeConverters(SortableDateTimeConverters.class) ZonedDateTime date);

    @Transaction
    @Query("SELECT `hrforecastblob` FROM hr_forecasts WHERE `query` = :key ORDER BY `dateblob` LIMIT :loadSize")
    public LiveData<List<HourlyForecast>> getLiveHourlyForecastsByQueryOrderByDateByLimit(String key, int loadSize);

    @Transaction
    @Query("SELECT `hrforecastblob` FROM hr_forecasts WHERE `query` = :key AND `dateblob` >= :date ORDER BY `dateblob` LIMIT :loadSize")
    public LiveData<List<HourlyForecast>> getLiveHourlyForecastsByQueryOrderByDateByLimitFilterByDate(String key, int loadSize, @TypeConverters(SortableDateTimeConverters.class) ZonedDateTime date);

    @Transaction
    @Query("SELECT `hrforecastblob` FROM hr_forecasts WHERE `query` = :key ORDER BY `dateblob`")
    public DataSource.Factory<Integer, HourlyForecast> loadHourlyForecastsByQueryOrderByDate(String key);

    @Transaction
    @Query("SELECT `hrforecastblob` FROM hr_forecasts WHERE `query` = :key AND `dateblob` >= :date ORDER BY `dateblob`")
    public DataSource.Factory<Integer, HourlyForecast> loadHourlyForecastsByQueryOrderByDateFilterByDate(String key, @TypeConverters(SortableDateTimeConverters.class) ZonedDateTime date);

    @Query("SELECT COUNT(*) FROM hr_forecasts WHERE `query` = :key")
    public int getHourlyForecastCountByQuery(String key);

    @Query("SELECT COUNT(*) FROM (SELECT COUNT(*) FROM hr_forecasts GROUP BY `query`)")
    public int getHourlyForecastCountGroupedByQuery();

    @Query("SELECT `hrforecastblob` FROM hr_forecasts WHERE `query` = :key AND `dateblob` >= :date ORDER BY `dateblob` LIMIT 1")
    public HourlyForecast getFirstHourlyForecastDataByDate(String key, @TypeConverters(SortableDateTimeConverters.class) ZonedDateTime date);
}
