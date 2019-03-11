package com.thewizrd.shared_resources.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.LocationDBConverters;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.Favorites;

@Database(entities = {LocationData.class, Favorites.class}, version = Settings.CURRENT_DBVERSION)
@TypeConverters({LocationDBConverters.class})
public abstract class LocationsDatabase extends RoomDatabase {
    public abstract LocationsDAO locationsDAO();
}
