package com.thewizrd.weather_api.database

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [City::class],
    version = 1
)
abstract class CityDatabase : RoomDatabase() {
    abstract fun cityDAO(): CityDAO

    companion object {
        @Volatile
        private var instance: CityDatabase? = null

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal fun getInstance(context: Context): CityDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }

        fun getCityDAO(context: Context) = getInstance(context).cityDAO()

        private fun buildDatabase(context: Context): CityDatabase {
            return Room.databaseBuilder(
                context.applicationContext, CityDatabase::class.java,
                "city.list.db"
            )
                .createFromAsset("citydb/city.list.db")
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}