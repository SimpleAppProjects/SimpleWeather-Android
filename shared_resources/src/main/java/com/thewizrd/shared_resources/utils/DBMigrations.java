package com.thewizrd.shared_resources.utils;

import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thewizrd.shared_resources.database.LocationsDatabase;
import com.thewizrd.shared_resources.database.SortableDateTimeConverters;
import com.thewizrd.shared_resources.database.WeatherDBConverters;
import com.thewizrd.shared_resources.database.WeatherDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.threeten.bp.ZonedDateTime;

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

    static final Migration W_MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create the new table
            database.execSQL(
                    "CREATE TABLE `weatherdata_new` (`ttl` varchar, `source` varchar, `query` varchar NOT NULL, `locale` varchar, `locationblob` varchar, `update_time` varchar, `conditionblob` varchar, `atmosphereblob` varchar, `astronomyblob` varchar, `precipitationblob` varchar, PRIMARY KEY(`query`))");
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `forecasts` (`query` TEXT NOT NULL, `forecastblob` TEXT, `txtforecastblob` TEXT, PRIMARY KEY(`query`))");
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `hr_forecasts` (`query` TEXT NOT NULL, `dateblob` TEXT NOT NULL, `hrforecastblob` TEXT, PRIMARY KEY(`query`, `dateblob`))");
            // Copy the data
            database.execSQL(
                    "INSERT INTO weatherdata_new (`ttl`, `source`, `query`, `locale`, `locationblob`, `update_time`, `conditionblob`, `atmosphereblob`, `astronomyblob`, `precipitationblob`) SELECT `ttl`, `source`, `query`, `locale`, `locationblob`, `update_time`, `conditionblob`, `atmosphereblob`, `astronomyblob`, `precipitationblob` from weatherdata");
            database.execSQL(
                    "INSERT INTO forecasts (`query`, `forecastblob`, `txtforecastblob`) SELECT `query`, `forecastblob`, `txtforecastblob` from weatherdata");
            Cursor weatherQueryCursor = database.query("select query from weatherdata");
            try {
                while (weatherQueryCursor.moveToNext()) {
                    String query = weatherQueryCursor.getString(0);

                    Cursor hrForecastCursor = database.query("SELECT hrforecastblob from weatherdata WHERE query = ?", new Object[]{query});
                    try {
                        while (hrForecastCursor.moveToNext()) {
                            String blobs = hrForecastCursor.getString(0);

                            if (!StringUtils.isNullOrWhitespace(blobs)) {
                                JSONArray jsonArr = new JSONArray(blobs);

                                for (int i = 0; i < jsonArr.length(); i++) {
                                    String json = jsonArr.getString(i);
                                    JsonObject child = JsonParser.parseString(json).getAsJsonObject();
                                    String date = child.get("date").getAsString();

                                    if (!StringUtils.isNullOrWhitespace(json) && !StringUtils.isNullOrWhitespace(date)) {
                                        ZonedDateTime dto = WeatherDBConverters.zonedDateTimeFromString(date);
                                        String dtoStr = SortableDateTimeConverters.zonedDateTimetoString(dto);

                                        database.execSQL(
                                                "INSERT INTO hr_forecasts (`query`, `date`, `hr_forecast`) VALUES (?, ?, ?)",
                                                new Object[]{query, dtoStr, json});
                                    }
                                }
                            }
                        }
                    } catch (JSONException e) {
                        Logger.writeLine(Log.ERROR, e, "Error parsing json!");
                    } finally {
                        hrForecastCursor.close();
                    }
                }
            } finally {
                weatherQueryCursor.close();
            }

            // Remove the old table
            database.execSQL("DROP TABLE weatherdata");
            // Change the table name to the correct one
            database.execSQL("ALTER TABLE weatherdata_new RENAME TO weatherdata");
        }
    };

    static final Migration LOC_MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Since we didn't alter the table, there's nothing else to do here.
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
                case 4:
                    // Migration for incremental loading
                    // Handled in init method
                    break;
                default:
                    break;
            }

            Settings.setDBVersion(Settings.CURRENT_DBVERSION);
        }
    }
}
