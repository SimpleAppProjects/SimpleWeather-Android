package com.thewizrd.shared_resources.database

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.DBMigrations
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.shared_resources.weatherdata.model.Favorites

@Database(
    entities = [LocationData::class, Favorites::class],
    version = SettingsManager.CURRENT_DBVERSION
)
@TypeConverters(LocationDBConverters::class)
abstract class LocationsDatabase : RoomDatabase() {
    abstract fun locationsDAO(): LocationsDAO

    companion object {
        @Volatile
        private var instance: LocationsDatabase? = null

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal fun getInstance(context: Context): LocationsDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }

        fun getLocationsDAO(context: Context) = getInstance(context).locationsDAO()

        private fun buildDatabase(context: Context): LocationsDatabase {
            return Room.databaseBuilder(
                context.applicationContext, LocationsDatabase::class.java,
                "locations.db"
            )
                .addMigrations(*LOC_MIGRATION_SET)
                .fallbackToDestructiveMigration()
                .build()
        }

        private val LOC_MIGRATION_0_3 = object : Migration(0, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Since we didn't alter the table, there's nothing else to do here.
            }
        }

        private val LOC_MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new table
                database.execSQL(
                    "DROP TABLE IF EXISTS `locations_new`"
                )
                database.execSQL(
                    "CREATE TABLE locations_new (`query` TEXT NOT NULL, `name` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `tz_long` TEXT, `locationType` INTEGER, `source` TEXT, `locsource` TEXT, PRIMARY KEY(`query`))"
                )
                // Copy the data
                database.execSQL(
                    "INSERT INTO locations_new (`query`, `name`, `latitude`, `longitude`, `tz_long`, `locationType`, `source`) SELECT `query`, `name`, `latitude`, `longitude`, `tz_long`, `locationType`, `source` FROM locations"
                )
                // Remove the old table
                database.execSQL("DROP TABLE locations")
                // Change the table name to the correct one
                database.execSQL("ALTER TABLE locations_new RENAME TO locations")

                // Create the new table
                database.execSQL(
                    "DROP TABLE IF EXISTS `favorites_new`"
                )
                database.execSQL(
                    "CREATE TABLE favorites_new (`query` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`query`))"
                )
                // Copy the data
                database.execSQL(
                    "INSERT INTO favorites_new (`query`, `position`) SELECT `query`, `position` FROM favorites"
                )
                // Remove the old table
                database.execSQL("DROP TABLE favorites")
                // Change the table name to the correct one
                database.execSQL("ALTER TABLE favorites_new RENAME TO favorites")
            }
        }

        private val LOC_MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Since we didn't alter the table, there's nothing else to do here.
            }
        }
        private val LOC_MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Since we didn't alter the table, there's nothing else to do here.
            }
        }
        private val LOC_MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Since we didn't alter the table, there's nothing else to do here.
            }
        }
        private val LOC_MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Since we didn't alter the table, there's nothing else to do here.
            }
        }
        private val LOC_MIGRATION_4_8 = object : Migration(4, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Since we didn't alter the table, there's nothing else to do here.
            }
        }

        @RestrictTo(RestrictTo.Scope.TESTS)
        internal val LOC_MIGRATION_SET = arrayOf(
            LOC_MIGRATION_0_3, LOC_MIGRATION_3_4,
            LOC_MIGRATION_4_5, LOC_MIGRATION_5_6, LOC_MIGRATION_6_7, LOC_MIGRATION_7_8,
            LOC_MIGRATION_4_8
        )
    }
}