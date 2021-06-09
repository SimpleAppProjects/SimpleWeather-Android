package com.thewizrd.shared_resources.database

import android.content.Context
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.JsonParser
import com.thewizrd.shared_resources.utils.DBMigrations
import com.thewizrd.shared_resources.utils.Logger
import com.thewizrd.shared_resources.utils.SettingsManager
import com.thewizrd.shared_resources.weatherdata.model.Forecasts
import com.thewizrd.shared_resources.weatherdata.model.HourlyForecasts
import com.thewizrd.shared_resources.weatherdata.model.Weather
import com.thewizrd.shared_resources.weatherdata.model.WeatherAlerts
import org.json.JSONArray
import org.json.JSONException

@Database(
    entities = [Weather::class, WeatherAlerts::class, Forecasts::class, HourlyForecasts::class],
    version = SettingsManager.CURRENT_DBVERSION
)
@TypeConverters(WeatherDBConverters::class)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDAO(): WeatherDAO

    companion object {
        @Volatile
        private var instance: WeatherDatabase? = null

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        internal fun getInstance(context: Context): WeatherDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }

        fun getWeatherDAO(context: Context) = getInstance(context).weatherDAO()

        private fun buildDatabase(context: Context): WeatherDatabase {
            return Room.databaseBuilder(
                context.applicationContext, WeatherDatabase::class.java,
                "weatherdata.db"
            )
                .addMigrations(*W_MIGRATION_SET)
                .fallbackToDestructiveMigration()
                .build()
        }

        private val W_MIGRATION_0_3 = object : Migration(0, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Since we didn't alter the table, there's nothing else to do here.
            }
        }

        private val W_MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new table
                database.execSQL(
                    "DROP TABLE IF EXISTS `weatherdata_new`"
                )
                database.execSQL(
                    "CREATE TABLE weatherdata_new (`locationblob` TEXT, `update_time` TEXT, `forecastblob` TEXT, `hrforecastblob` TEXT, `txtforecastblob` TEXT, `conditionblob` TEXT, `atmosphereblob` TEXT, `astronomyblob` TEXT, `precipitationblob` TEXT, `ttl` TEXT, `source` TEXT, `query` TEXT NOT NULL, `locale` TEXT, PRIMARY KEY(`query`))"
                )
                // Copy the data
                database.execSQL(
                    "INSERT INTO weatherdata_new (`locationblob`, `update_time`, `forecastblob`, `hrforecastblob`, `txtforecastblob`, `conditionblob`, `atmosphereblob`, `astronomyblob`, `precipitationblob`, `ttl`, `source`, `query`, `locale`) SELECT `locationblob`, `update_time`, `forecastblob`, `hrforecastblob`, `txtforecastblob`, `conditionblob`, `atmosphereblob`, `astronomyblob`, `precipitationblob`, `ttl`, `source`, `query`, `locale` FROM weatherdata"
                )
                // Remove the old table
                database.execSQL("DROP TABLE weatherdata")
                // Change the table name to the correct one
                database.execSQL("ALTER TABLE weatherdata_new RENAME TO weatherdata")

                // Create the new table
                database.execSQL(
                    "DROP TABLE IF EXISTS `weatheralerts_new`"
                )
                database.execSQL(
                    "CREATE TABLE weatheralerts_new (`query` TEXT NOT NULL, `weather_alerts` TEXT, PRIMARY KEY(`query`))"
                )
                // Copy the data
                database.execSQL(
                    "INSERT INTO weatheralerts_new (`query`, `weather_alerts`) SELECT `query`, `weather_alerts` FROM weatheralerts"
                )
                // Remove the old table
                database.execSQL("DROP TABLE weatheralerts")
                // Change the table name to the correct one
                database.execSQL("ALTER TABLE weatheralerts_new RENAME TO weatheralerts")
            }
        }

        private val W_MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new table
                database.execSQL(
                    "DROP TABLE IF EXISTS `weatherdata_new`"
                )
                database.execSQL(
                    "CREATE TABLE `weatherdata_new` (`ttl` varchar, `source` varchar, `query` varchar NOT NULL, `locale` varchar, `locationblob` varchar, `update_time` varchar, `conditionblob` varchar, `atmosphereblob` varchar, `astronomyblob` varchar, `precipitationblob` varchar, PRIMARY KEY(`query`))"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `forecasts` (`query` TEXT NOT NULL, `forecastblob` TEXT, `txtforecastblob` TEXT, PRIMARY KEY(`query`))"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `hr_forecasts` (`query` TEXT NOT NULL, `dateblob` TEXT NOT NULL, `hrforecastblob` TEXT, PRIMARY KEY(`query`, `dateblob`))"
                )
                // Copy the data
                database.execSQL(
                    "INSERT INTO weatherdata_new (`ttl`, `source`, `query`, `locale`, `locationblob`, `update_time`, `conditionblob`, `atmosphereblob`, `astronomyblob`, `precipitationblob`) SELECT `ttl`, `source`, `query`, `locale`, `locationblob`, `update_time`, `conditionblob`, `atmosphereblob`, `astronomyblob`, `precipitationblob` from weatherdata"
                )
                database.execSQL(
                    "INSERT INTO forecasts (`query`, `forecastblob`, `txtforecastblob`) SELECT `query`, `forecastblob`, `txtforecastblob` from weatherdata"
                )

                val cursor = database.query("select query from weatherdata")

                cursor.use { weatherQueryCursor ->
                    while (weatherQueryCursor.moveToNext()) {
                        val query = weatherQueryCursor.getString(0)

                        val hrForecastCursor = database.query(
                            "SELECT hrforecastblob from weatherdata WHERE query = ?",
                            arrayOf<Any>(query)
                        )

                        try {
                            while (hrForecastCursor.moveToNext()) {
                                val blobs = hrForecastCursor.getString(0)

                                if (!blobs.isNullOrBlank()) {
                                    val jsonArr = JSONArray(blobs)
                                    for (i in 0 until jsonArr.length()) {
                                        val json = jsonArr.getString(i)
                                        val child = JsonParser.parseString(json).asJsonObject
                                        val date = child["date"].asString

                                        if (!json.isNullOrBlank() && !date.isNullOrBlank()) {
                                            val dto =
                                                WeatherDBConverters.zonedDateTimeFromString(date)
                                            val dtoStr =
                                                SortableDateTimeConverters.zonedDateTimetoString(dto)

                                            database.execSQL(
                                                "INSERT INTO hr_forecasts (`query`, `dateblob`, `hrforecastblob`) VALUES (?, ?, ?)",
                                                arrayOf<Any>(query, dtoStr, json)
                                            )
                                        }
                                    }
                                }
                            }
                        } catch (e: JSONException) {
                            Logger.writeLine(Log.ERROR, e, "Error parsing json!")
                        } finally {
                            hrForecastCursor.close()
                        }
                    }
                }

                // Remove the old table
                database.execSQL("DROP TABLE weatherdata")
                // Change the table name to the correct one
                database.execSQL("ALTER TABLE weatherdata_new RENAME TO weatherdata")
            }
        }

        private val W_MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new table
                database.execSQL(
                    "DROP TABLE IF EXISTS `weatherdata_new`"
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `weatherdata_new` (`locationblob` TEXT, `update_time` TEXT, `conditionblob` TEXT, `atmosphereblob` TEXT, `astronomyblob` TEXT, `precipitationblob` TEXT, `ttl` INTEGER NOT NULL, `source` TEXT, `query` TEXT NOT NULL, `locale` TEXT, PRIMARY KEY(`query`))"
                )

                // Copy the data
                database.execSQL(
                    "INSERT INTO weatherdata_new (`locationblob`, `update_time`, `conditionblob`, `atmosphereblob`, `astronomyblob`, `precipitationblob`, `ttl`, `source`, `query`, `locale`) SELECT `locationblob`, `update_time`, `conditionblob`, `atmosphereblob`, `astronomyblob`, `precipitationblob`, IFNULL(CAST(`ttl` AS INTEGER), 120), `source`, `query`, `locale` FROM weatherdata"
                )

                // Remove the old table
                database.execSQL("DROP TABLE weatherdata")
                // Change the table name to the correct one
                database.execSQL("ALTER TABLE weatherdata_new RENAME TO weatherdata")
            }
        }

        private val W_MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Update data
                val cursor =
                    database.query("SELECT * from weatherdata WHERE `source` = 'openweather' OR `source` = 'Metno'")

                cursor.use { weatherCursor ->
                    while (weatherCursor.moveToNext()) {
                        val query = weatherCursor.getString(weatherCursor.getColumnIndex("query"))

                        // Update forecasts
                        database.execSQL(
                            "UPDATE forecasts SET `forecastblob` = `REPLACE`(`forecastblob`, ?, ?) WHERE `query` = ?",
                            arrayOf<Any>("\"pop\"", "\"cloudiness\"", query)
                        )
                        database.execSQL(
                            "UPDATE hr_forecasts SET `hrforecastblob` = `REPLACE`(`hrforecastblob`, ?, ?) WHERE `query` = ?",
                            arrayOf<Any>("\"pop\"", "\"cloudiness\"", query)
                        )

                        // Update weather data
                        database.execSQL(
                            "UPDATE weatherdata SET `precipitationblob` = `REPLACE`(`precipitationblob`, ?, ?) WHERE `query` = ?",
                            arrayOf<Any>("\"pop\"", "\"cloudiness\"", query)
                        )
                    }
                }
            }
        }
        private val W_MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE forecasts ADD COLUMN `minforecastblob` TEXT")
            }
        }

        @RestrictTo(RestrictTo.Scope.TESTS)
        internal val W_MIGRATION_SET = arrayOf(
            W_MIGRATION_0_3, W_MIGRATION_3_4, W_MIGRATION_4_5,
            W_MIGRATION_5_6, W_MIGRATION_6_7, W_MIGRATION_7_8
        )
    }
}