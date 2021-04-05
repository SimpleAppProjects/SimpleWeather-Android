package com.thewizrd.shared_resources.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.thewizrd.shared_resources.tzdb.TZDB;
import com.thewizrd.shared_resources.utils.SettingsManager;

@Database(entities = {TZDB.class}, version = SettingsManager.CURRENT_DBVERSION)
public abstract class TZDatabase extends RoomDatabase {
    public abstract TzdbDAO tzdbDAO();
}
