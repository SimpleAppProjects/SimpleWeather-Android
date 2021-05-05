package com.thewizrd.shared_resources.tzdb

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.database.TZDatabase
import com.thewizrd.shared_resources.utils.AnalyticsLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object TZDBCache {
    private var tzDB: TZDatabase? = null

    private val TZDB_MIGRATION_0_5: Migration = object : Migration(0, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Since we didn't alter the table, there's nothing else to do here.
        }
    }

    private val TZDB_MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Since we didn't alter the table, there's nothing else to do here.
        }
    }

    private val TZDB_MIGRATION_6_7: Migration = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Since we didn't alter the table, there's nothing else to do here.
        }
    }

    private val TZDB_MIGRATION_SET = arrayOf(
            TZDB_MIGRATION_0_5, TZDB_MIGRATION_5_6, TZDB_MIGRATION_6_7
    )

    suspend fun getTimeZone(latitude: Double, longitude: Double): String? {
        if (latitude != 0.0 && longitude != 0.0) {
            AnalyticsLogger.logEvent("TZDBCache: querying")
            // Initialize db if it hasn't been already
            if (tzDB == null) {
                val context = SimpleLibrary.getInstance().appContext
                tzDB = Room.databaseBuilder(context,
                        TZDatabase::class.java, "tzdb.db")
                        .addMigrations(*TZDB_MIGRATION_SET)
                        .fallbackToDestructiveMigration()
                        .build()
            }

            // Search db if result already exists
            val dbResult = tzDB?.tzdbDAO()?.getTimeZoneData(latitude, longitude)

            if (!dbResult.isNullOrBlank())
                return dbResult

            // Search tz lookup
            val result = TimeZoneProvider().getTimeZone(latitude, longitude)

            if (!result.isNullOrBlank()) {
                // Cache result
                GlobalScope.launch(Dispatchers.IO) {
                    val tzdb = TZDB(latitude, longitude, result)
                    tzDB!!.tzdbDAO().insertTZData(tzdb)

                    // Run GC since tz lookup takes up a good chunk of memory
                    System.gc()
                }
            }

            return result
        }

        return "UTC"
    }
}