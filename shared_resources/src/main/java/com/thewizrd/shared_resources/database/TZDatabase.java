package com.thewizrd.shared_resources.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.thewizrd.shared_resources.tzdb.TZDB;
import com.thewizrd.shared_resources.utils.Settings;

@Database(entities = {TZDB.class}, version = Settings.CURRENT_DBVERSION)
public abstract class TZDatabase extends RoomDatabase {
    public abstract TzdbDAO tzdbDAO();
}
