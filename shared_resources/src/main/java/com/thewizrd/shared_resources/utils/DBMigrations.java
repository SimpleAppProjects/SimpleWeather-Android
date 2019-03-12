package com.thewizrd.shared_resources.utils;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.migration.Migration;
import android.support.annotation.NonNull;

import com.thewizrd.shared_resources.database.LocationsDatabase;
import com.thewizrd.shared_resources.database.WeatherDatabase;

class DBMigrations {
    static final Migration MIGRATION_0_3 = new Migration(0, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Since we didn't alter the table, there's nothing else to do here.
        }
    };

    static final Migration W_MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create the new table
            database.execSQL(
                    "CREATE TABLE weatherdata_new (`locationblob` TEXT, `update_time` TEXT, `forecastblob` TEXT, `hrforecastblob` TEXT, `txtforecastblob` TEXT, `conditionblob` TEXT, `atmosphereblob` TEXT, `astronomyblob` TEXT, `precipitationblob` TEXT, `ttl` TEXT, `source` TEXT, `query` TEXT NOT NULL, `locale` TEXT, PRIMARY KEY(`query`))");
            // Copy the data
            database.execSQL(
                    "INSERT INTO weatherdata_new (`locationblob`, `update_time`, `forecastblob`, `hrforecastblob`, `txtforecastblob`, `conditionblob`, `atmosphereblob`, `astronomyblob`, `precipitationblob`, `ttl`, `source`, `query`, `locale`) SELECT `locationblob`, `update_time`, `forecastblob`, `hrforecastblob`, `txtforecastblob`, `conditionblob`, `atmosphereblob`, `astronomyblob`, `precipitationblob`, `ttl`, `source`, `query`, `locale` FROM weatherdata");
            // Remove the old table
            database.execSQL("DROP TABLE weatherdata");
            // Change the table name to the correct one
            database.execSQL("ALTER TABLE weatherdata_new RENAME TO weatherdata");

            // Create the new table
            database.execSQL(
                    "CREATE TABLE weatheralerts_new (`query` TEXT NOT NULL, `weather_alerts` TEXT, PRIMARY KEY(`query`))");
            // Copy the data
            database.execSQL(
                    "INSERT INTO weatheralerts_new (`query`, `weather_alerts`) SELECT `query`, `weather_alerts` FROM weatheralerts");
            // Remove the old table
            database.execSQL("DROP TABLE weatheralerts");
            // Change the table name to the correct one
            database.execSQL("ALTER TABLE weatheralerts_new RENAME TO weatheralerts");
        }
    };

    static final Migration LOC_MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create the new table
            database.execSQL(
                    "CREATE TABLE locations_new (`query` TEXT NOT NULL, `name` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `tz_long` TEXT, `locationType` INTEGER, `source` TEXT, `locsource` TEXT, PRIMARY KEY(`query`))");
            // Copy the data
            database.execSQL(
                    "INSERT INTO locations_new (`query`, `name`, `latitude`, `longitude`, `tz_long`, `locationType`, `source`) SELECT `query`, `name`, `latitude`, `longitude`, `tz_long`, `locationType`, `source` FROM locations");
            // Remove the old table
            database.execSQL("DROP TABLE locations");
            // Change the table name to the correct one
            database.execSQL("ALTER TABLE locations_new RENAME TO locations");

            // Create the new table
            database.execSQL(
                    "CREATE TABLE favorites_new (`query` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`query`))");
            // Copy the data
            database.execSQL(
                    "INSERT INTO favorites_new (`query`, `position`) SELECT `query`, `position` FROM favorites");
            // Remove the old table
            database.execSQL("DROP TABLE favorites");
            // Change the table name to the correct one
            database.execSQL("ALTER TABLE favorites_new RENAME TO favorites");
        }
    };

    static void performMigrations(final WeatherDatabase weatherDB, final LocationsDatabase locationDB) {
        // Migrate old data if available
        int vDB = Settings.getDBVersion();

        if (vDB < Settings.CURRENT_DBVERSION) {
            switch (vDB) {
                // Move data from json to db
                case 0:
                    // Not available here
                    break;
                // Add and set tz_long column in db
                case 1:
                    if (Settings.IS_PHONE && locationDB.locationsDAO().getLocationDataCount() > 0) {
                        DBUtils.setLocationData(locationDB, Settings.getAPI());
                    }
                    break;
                // Room DB migration
                case 2:
                    // Move db from appdata to db folder
                    // Handled in init method
                    break;
                // LocationData updates: added new fields
                case 3:
                    if (Settings.IS_PHONE && locationDB.locationsDAO().getLocationDataCount() > 0) {
                        DBUtils.setLocationData(locationDB, Settings.getAPI());
                    }
                    break;
                default:
                    break;
            }

            Settings.setDBVersion(Settings.CURRENT_DBVERSION);
        }
    }
}
