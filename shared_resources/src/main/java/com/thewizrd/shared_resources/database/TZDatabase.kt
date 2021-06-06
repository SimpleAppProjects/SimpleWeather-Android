package com.thewizrd.shared_resources.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.thewizrd.shared_resources.tzdb.TZDB
import com.thewizrd.shared_resources.utils.SettingsManager

@Database(entities = [TZDB::class], version = SettingsManager.CURRENT_DBVERSION)
abstract class TZDatabase : RoomDatabase() {
    abstract fun tzdbDAO(): TzdbDAO

    companion object {
        @Volatile
        private var instance: TZDatabase? = null

        fun getInstance(context: Context): TZDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }

        private fun buildDatabase(context: Context): TZDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                TZDatabase::class.java,
                "tzdb.db"
            )
                .addMigrations(*TZDB_MIGRATION_SET)
                .fallbackToDestructiveMigration()
                .build()
        }

        /* Migration sets */
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
        private val TZDB_MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Since we didn't alter the table, there's nothing else to do here.
            }
        }

        private val TZDB_MIGRATION_SET = arrayOf(
            TZDB_MIGRATION_0_5, TZDB_MIGRATION_5_6, TZDB_MIGRATION_6_7, TZDB_MIGRATION_7_8
        )
    }
}