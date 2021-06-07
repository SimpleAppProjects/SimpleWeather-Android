package com.thewizrd.shared_resources.utils

import android.content.Context
import android.os.Bundle
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.thewizrd.shared_resources.database.LocationsDatabase
import com.thewizrd.shared_resources.database.WeatherDatabase

internal object DBMigrations {
    internal val MIGRATION_0_3 = object : Migration(0, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Since we didn't alter the table, there's nothing else to do here.
        }
    }

    // TODO: remove obsolete migration path
    suspend fun performMigrations(
        context: Context,
        weatherDB: WeatherDatabase,
        locationDB: LocationsDatabase
    ) {
        // Migrate old data if available
        val settingsMgr = SettingsManager(context)

        val vDB = settingsMgr.getDBVersion()
        if (vDB < SettingsManager.CURRENT_DBVERSION) {
            val args = Bundle().apply {
                putInt("Version", settingsMgr.getDBVersion())
                putInt("CurrentDBVersion", SettingsManager.CURRENT_DBVERSION)
            }
            AnalyticsLogger.logEvent("DBMigrations: performMigrations", args)

            when (vDB) {
                0, 1, 2, 3 ->
                    // LocationData updates: added new fields
                    if (settingsMgr.isPhone && locationDB.locationsDAO()
                            .getLocationDataCount() > 0
                    ) {
                        DBUtils.setLocationData(locationDB, settingsMgr.getAPI())
                    }
                4 -> {
                    // no-op
                }
                else -> {
                    // no-op
                }
            }

            settingsMgr.setDBVersion(SettingsManager.CURRENT_DBVERSION)
        }
    }
}