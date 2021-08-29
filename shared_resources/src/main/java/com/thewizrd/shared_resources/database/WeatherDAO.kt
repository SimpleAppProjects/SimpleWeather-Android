package com.thewizrd.shared_resources.database

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import com.thewizrd.shared_resources.weatherdata.model.*
import java.time.ZonedDateTime

@Dao
interface WeatherDAO {
    /* WeatherData methods */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherData(weather: Weather)

    @Delete
    suspend fun deleteWeatherData(weather: Weather)

    @Query("DELETE FROM weatherdata WHERE `query` = :key")
    suspend fun deleteWeatherDataByKey(key: String?)

    @Query("DELETE FROM weatherdata WHERE `query` NOT IN (:keyQuery)")
    suspend fun deleteWeatherDataByKeyNotIn(keyQuery: List<String?>)

    @Transaction
    @Query("SELECT * FROM weatherdata")
    suspend fun loadAllWeatherData(): List<Weather>

    @Query("SELECT * FROM weatherdata WHERE `query` = :query")
    suspend fun getWeatherData(query: String?): Weather?

    @Query("SELECT * FROM weatherdata WHERE `locationblob` LIKE :searchQuery LIMIT 1")
    suspend fun getWeatherDataByCoord(searchQuery: String?): Weather?

    @Query("SELECT COUNT(*) FROM weatherdata")
    suspend fun getWeatherDataCount(): Int

    @Query("SELECT COUNT(*) FROM weatherdata")
    fun getWeatherDataCountSync(): Int

    @Query("SELECT COUNT(*) FROM weatherdata WHERE `query` = :query")
    suspend fun getWeatherDataCountByKey(query: String?): Int

    /* WeatherAlertData methods */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherAlertData(alert: WeatherAlerts)

    @Delete
    suspend fun deleteWeatherAlertData(alert: WeatherAlerts)

    @Query("DELETE FROM weatheralerts WHERE `query` = :key")
    suspend fun deleteWeatherAlertDataByKey(key: String?)

    @Query("DELETE FROM weatheralerts WHERE `query` NOT IN (:keyQuery)")
    suspend fun deleteWeatherAlertDataByKeyNotIn(keyQuery: List<String?>)

    @Transaction
    @Query("SELECT * FROM weatheralerts")
    suspend fun loadAllWeatherAlertData(): List<WeatherAlerts>

    @Query("SELECT * FROM weatheralerts WHERE `query` = :query")
    suspend fun getWeatherAlertData(query: String?): WeatherAlerts?

    @Query("SELECT * FROM weatheralerts WHERE `query` = :query")
    fun getLiveWeatherAlertData(query: String?): LiveData<WeatherAlerts>

    @Query("SELECT COUNT(*) FROM weatheralerts")
    suspend fun getWeatherAlertDataCount(): Int

    /* Forecasts methods */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForecast(forecast: Forecasts)

    @Delete
    suspend fun deleteForecast(forecast: Forecasts)

    @Query("DELETE FROM forecasts WHERE `query` = :key")
    suspend fun deleteForecastByKey(key: String?)

    @Query("DELETE FROM forecasts WHERE `query` NOT IN (:keyQuery)")
    suspend fun deleteForecastByKeyNotIn(keyQuery: List<String?>)

    @Transaction
    @Query("SELECT * FROM forecasts")
    suspend fun loadAllForecasts(): List<Forecasts>

    @Query("SELECT * FROM forecasts WHERE `query` = :query")
    suspend fun getForecastData(query: String?): Forecasts?

    @Query("SELECT * FROM forecasts WHERE `query` = :query")
    fun getLiveForecastData(query: String?): LiveData<Forecasts>

    @Query("SELECT COUNT(*) FROM forecasts GROUP BY `query`")
    suspend fun getForecastDataCountGroupedByQuery(): Int

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHourlyForecasts(forecasts: Collection<HourlyForecasts>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHourlyForecast(forecast: HourlyForecasts)

    @Delete
    suspend fun deleteHourlyForecast(forecast: HourlyForecasts)

    @Query("DELETE FROM hr_forecasts WHERE `query` = :key")
    suspend fun deleteHourlyForecastByKey(key: String?)

    @Query("DELETE FROM hr_forecasts WHERE `query` NOT IN (:keyQuery)")
    suspend fun deleteHourlyForecastByKeyNotIn(keyQuery: List<String?>)

    @Transaction
    @Query("SELECT `hrforecastblob` FROM hr_forecasts WHERE `query` = :key ORDER BY `dateblob`")
    suspend fun getHourlyForecastsByQueryOrderByDate(key: String?): List<HourlyForecast>

    @Transaction
    @Query("SELECT `hrforecastblob` FROM hr_forecasts WHERE `query` = :key ORDER BY `dateblob` LIMIT :loadSize")
    suspend fun getHourlyForecastsByQueryOrderByDateByLimit(
        key: String?,
        loadSize: Int
    ): List<HourlyForecast>

    @Transaction
    @Query("SELECT `hrforecastblob` FROM hr_forecasts WHERE `query` = :key AND `dateblob` >= :date ORDER BY `dateblob` LIMIT :loadSize")
    suspend fun getHourlyForecastsByQueryOrderByDateByLimitFilterByDate(
        key: String?, loadSize: Int,
        @TypeConverters(SortableDateTimeConverters::class) date: ZonedDateTime
    ): List<HourlyForecast>

    @Transaction
    @Query("SELECT `hrforecastblob` FROM hr_forecasts WHERE `query` = :key AND `dateblob` >= :date ORDER BY `dateblob` LIMIT :loadSize OFFSET :offset")
    suspend fun getHourlyForecastsByQueryOrderByDateByLimitByOffsetFilterByDate(
        key: String?, loadSize: Int, offset: Int,
        @TypeConverters(SortableDateTimeConverters::class) date: ZonedDateTime
    ): List<HourlyForecast>

    @Transaction
    @Query("SELECT `hrforecastblob` FROM hr_forecasts WHERE `query` = :key ORDER BY `dateblob` LIMIT :loadSize")
    fun getLiveHourlyForecastsByQueryOrderByDateByLimit(
        key: String?,
        loadSize: Int
    ): LiveData<List<HourlyForecast>>

    @Transaction
    @Query("SELECT `hrforecastblob` FROM hr_forecasts WHERE `query` = :key AND `dateblob` >= :date ORDER BY `dateblob` LIMIT :loadSize")
    fun getLiveHourlyForecastsByQueryOrderByDateByLimitFilterByDate(
        key: String?,
        loadSize: Int,
        @TypeConverters(SortableDateTimeConverters::class) date: ZonedDateTime
    ): LiveData<List<HourlyForecast>>

    @Transaction
    @Query("SELECT `hrforecastblob` FROM hr_forecasts WHERE `query` = :key ORDER BY `dateblob`")
    fun loadHourlyForecastsByQueryOrderByDate(key: String?): DataSource.Factory<Int, HourlyForecast>

    @Transaction
    @Query("SELECT `hrforecastblob` FROM hr_forecasts WHERE `query` = :key AND `dateblob` >= :date ORDER BY `dateblob`")
    fun loadHourlyForecastsByQueryOrderByDateFilterByDate(
        key: String?, @TypeConverters(SortableDateTimeConverters::class) date: ZonedDateTime
    ): DataSource.Factory<Int, HourlyForecast>

    @Query("SELECT COUNT(*) FROM hr_forecasts WHERE `query` = :key")
    suspend fun getHourlyForecastCountByQuery(key: String?): Int

    @Query("SELECT COUNT(*) FROM (SELECT COUNT(*) FROM hr_forecasts GROUP BY `query`)")
    suspend fun getHourlyForecastCountGroupedByQuery(): Int

    @Query("SELECT `hrforecastblob` FROM hr_forecasts WHERE `query` = :key AND `dateblob` >= :date ORDER BY `dateblob` LIMIT 1")
    suspend fun getFirstHourlyForecastDataByDate(
        key: String?, @TypeConverters(SortableDateTimeConverters::class) date: ZonedDateTime
    ): HourlyForecast?
}