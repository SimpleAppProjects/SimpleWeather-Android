package com.thewizrd.shared_resources.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.thewizrd.shared_resources.tzdb.TZDB;

@Dao
public interface TzdbDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertTZData(TZDB tzdb);

    @Delete
    public void deleteTZData(TZDB tzdb);

    @Transaction
    @Query("SELECT `tz_long` FROM tzdb WHERE `latitude` = :lat AND `longitude` = :lon")
    public String getTimeZoneData(double lat, double lon);
}
