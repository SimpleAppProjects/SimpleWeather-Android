package com.thewizrd.common

import android.content.Context
import com.thewizrd.common.migrations.VersionMigrations
import com.thewizrd.shared_resources.database.LocationsDatabase
import com.thewizrd.shared_resources.database.WeatherDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

private lateinit var _commonModule: CommonModule

var commonModule: CommonModule
    get() = _commonModule
    set(value) {
        _commonModule = value
        value.init()
    }

abstract class CommonModule {
    abstract val context: Context

    internal fun init() {
        runBlocking(Dispatchers.IO) {
            /* Version-specific Migrations */
            VersionMigrations.performMigrations(
                context,
                WeatherDatabase.getWeatherDAO(context),
                LocationsDatabase.getLocationsDAO(context)
            )
        }
    }
}