package com.thewizrd.shared_resources.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;

import com.thewizrd.shared_resources.weatherdata.Favorites;
import com.thewizrd.shared_resources.weatherdata.LocationData;

import java.util.List;

@Dao
public interface LocationsDAO {
    /* LocationData methods */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertLocationData(LocationData Location);

    @Update
    public void updateLocationData(LocationData location);

    @Delete
    public void deleteLocationData(LocationData Location);

    @Query("DELETE FROM locations")
    public void deleteAllLocationData();

    @Query("DELETE FROM locations WHERE `query` = :key")
    public void deleteLocationDataByKey(String key);

    @Transaction
    @Query("SELECT * FROM locations")
    public List<LocationData> loadAllLocationData();

    @Transaction
    @Query("SELECT locations.`query`, locations.`name`, locations.`latitude`," +
            " locations.`longitude`, locations.`tz_long`, locations.`locationType`, locations.`source` FROM locations" +
            " INNER JOIN favorites ON locations.`query` = favorites.`query`" +
            " ORDER BY favorites.position")
    public List<LocationData> getFavorites();

    @Query("SELECT * FROM locations WHERE `query` = :query")
    public LocationData getLocation(String query);

    @Query("SELECT COUNT(*) FROM locations")
    public int getLocationDataCount();

    /* Favorites methods */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public void insertFavorite(Favorites favorite);

    @Update
    public void updateFavorite(Favorites favorite);

    @Delete
    public void deleteFavoriteData(Favorites favorite);

    @Query("DELETE FROM favorites")
    public void deleteAllFavoriteData();

    @Query("DELETE FROM favorites WHERE `query` = :key")
    public void deleteFavoritesByKey(String key);

    @Transaction
    @Query("SELECT * FROM favorites")
    public List<Favorites> loadAllFavorites();

    @Query("SELECT * FROM favorites WHERE `query` = :query")
    public List<Favorites> getFavoritesData(String query);

    @Transaction
    @Query("SELECT * FROM favorites ORDER BY `position`")
    public List<Favorites> loadAllFavoritesByPosition();

    @Query("SELECT COUNT(*) FROM favorites")
    public int getFavoritesCount();

    @Query("UPDATE favorites SET `position` = :toPos WHERE `query` = :key")
    public void updateFavPosition(String key, int toPos);
}
