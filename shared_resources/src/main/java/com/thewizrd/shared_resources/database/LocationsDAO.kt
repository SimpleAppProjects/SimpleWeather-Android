package com.thewizrd.shared_resources.database

import androidx.room.*
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.weatherdata.model.Favorites

@Dao
interface LocationsDAO {
    /* LocationData methods */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationData(Location: LocationData)

    @Update
    suspend fun updateLocationData(location: LocationData)

    @Delete
    suspend fun deleteLocationData(Location: LocationData)

    @Query("DELETE FROM locations")
    suspend fun deleteAllLocationData()

    @Query("DELETE FROM locations WHERE `query` = :key")
    suspend fun deleteLocationDataByKey(key: String)

    @Transaction
    @Query("SELECT * FROM locations")
    suspend fun loadAllLocationData(): List<LocationData>

    @Query(
        "SELECT locations.* FROM locations" +
                " INNER JOIN favorites ON locations.`query` = favorites.`query`" +
                " ORDER BY favorites.position"
    )
    @Transaction
    suspend fun getFavorites(): List<LocationData>

    @Query(
        "SELECT locations.* FROM locations" +
                " INNER JOIN favorites ON locations.`query` = favorites.`query`" +
                " ORDER BY favorites.position LIMIT 1"
    )
    @Transaction
    suspend fun getFirstFavorite(): LocationData?

    @Query("SELECT * FROM locations WHERE `query` = :query")
    suspend fun getLocation(query: String?): LocationData?

    @Query("SELECT COUNT(*) FROM locations")
    suspend fun getLocationDataCount(): Int

    @Query("SELECT COUNT(*) FROM locations")
    fun getLocationDataCountSync(): Int

    /* Favorites methods */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: Favorites)

    @Update
    suspend fun updateFavorite(favorite: Favorites)

    @Delete
    suspend fun deleteFavoriteData(favorite: Favorites)

    @Query("DELETE FROM favorites")
    suspend fun deleteAllFavoriteData()

    @Query("DELETE FROM favorites WHERE `query` = :key")
    suspend fun deleteFavoritesByKey(key: String?)

    @Transaction
    @Query("SELECT * FROM favorites")
    suspend fun loadAllFavorites(): List<Favorites>

    @Query("SELECT * FROM favorites WHERE `query` = :query LIMIT 1")
    suspend fun getFavorite(query: String?): Favorites?

    @Transaction
    @Query("SELECT * FROM favorites ORDER BY `position`")
    suspend fun loadAllFavoritesByPosition(): List<Favorites>

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getFavoritesCount(): Int

    @Query("UPDATE favorites SET `position` = :toPos WHERE `query` = :key")
    suspend fun updateFavPosition(key: String?, toPos: Int)
}