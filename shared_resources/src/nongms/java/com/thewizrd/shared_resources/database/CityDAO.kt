package com.thewizrd.shared_resources.database

import androidx.room.Dao
import androidx.room.Query

@Dao
interface CityDAO {
    @Query("SELECT * from `data` WHERE `name` LIKE '%'||:query||'%' LIMIT 10")
    suspend fun findLocationsByQuery(query: String): List<City>

    /**
     * Finds a location within the database near the given coordinates
     *
     * @param lat Latitude; should be a decimal number with a single decimal place
     * @param lon Longitude; should be a decimal number with a single decimal place
     */
    @Query("SELECT * from `data` WHERE `lat` LIKE (:lat || '%') AND `lon` LIKE (:lon || '%')")
    suspend fun findLocationsByCoordinate(lat: String, lon: String): List<City>
}