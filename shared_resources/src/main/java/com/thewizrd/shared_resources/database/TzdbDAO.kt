package com.thewizrd.shared_resources.database

import androidx.room.*
import com.thewizrd.shared_resources.tzdb.TZDB

@Dao
interface TzdbDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTZData(tzdb: TZDB)

    @Delete
    suspend fun deleteTZData(tzdb: TZDB)

    @Transaction
    @Query("SELECT `tz_long` FROM tzdb WHERE `latitude` = :lat AND `longitude` = :lon")
    suspend fun getTimeZoneData(lat: Double, lon: Double): String?
}