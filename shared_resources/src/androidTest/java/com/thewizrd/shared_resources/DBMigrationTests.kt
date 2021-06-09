package com.thewizrd.shared_resources

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thewizrd.shared_resources.database.LocationsDatabase
import com.thewizrd.shared_resources.database.TZDatabase
import com.thewizrd.shared_resources.database.WeatherDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DBMigrationTests {
    private val TEST_DB = "migration-test"

    @Rule
    @JvmField
    val weatherDBHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WeatherDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Rule
    @JvmField
    val locationDBHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LocationsDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Rule
    @JvmField
    val tzdbDBHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TZDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrateAll() {
        tzdbDBMigrateAll()
        locationsDBMigrateAll()
        weatherDBMigrateAll()
    }

    @Throws(IOException::class)
    private fun weatherDBMigrateAll() {
        val dbName = "$TEST_DB-${WeatherDatabase::class.java.name}"

        // Create earliest version of the database.
        // We migrated to Room from db v3
        weatherDBHelper.createDatabase(dbName, 3).apply {
            close()
        }

        // Open latest version of the database. Room will validate the schema
        // once all migrations execute.
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            WeatherDatabase::class.java,
            dbName
        ).addMigrations(*WeatherDatabase.W_MIGRATION_SET).build().apply {
            openHelper.writableDatabase
            close()
        }
    }

    @Throws(IOException::class)
    private fun locationsDBMigrateAll() {
        val dbName = "$TEST_DB-${LocationsDatabase::class.java.name}"

        // Create earliest version of the database.
        // We migrated to Room from db v3
        locationDBHelper.createDatabase(dbName, 3).apply {
            close()
        }

        // Open latest version of the database. Room will validate the schema
        // once all migrations execute.
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            LocationsDatabase::class.java,
            dbName
        ).addMigrations(*LocationsDatabase.LOC_MIGRATION_SET).build().apply {
            openHelper.writableDatabase
            close()
        }
    }

    @Throws(IOException::class)
    private fun tzdbDBMigrateAll() {
        val dbName = "$TEST_DB-${TZDatabase::class.java.name}"

        // Create earliest version of the database.
        // We migrated to Room from db v5
        tzdbDBHelper.createDatabase(dbName, 5).apply {
            close()
        }

        // Open latest version of the database. Room will validate the schema
        // once all migrations execute.
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            TZDatabase::class.java,
            dbName
        ).addMigrations(*TZDatabase.TZDB_MIGRATION_SET).build().apply {
            openHelper.writableDatabase
            close()
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate7To8() {
        tzdbDBMigrate(7, 8)
        locationsDBMigrate(7, 8)
        weatherDBMigrate(7, 8)
    }

    @Throws(IOException::class)
    private fun weatherDBMigrate(from: Int, to: Int) {
        val dbName = "$TEST_DB-${WeatherDatabase::class.java.name}"

        var db = weatherDBHelper.createDatabase(dbName, from).apply {
            // db has schema version 1. insert some data using SQL queries.
            // You cannot use DAO classes because they expect the latest schema.
            //execSQL(...)

            // Prepare for the next version.
            close()
        }

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        db = weatherDBHelper.runMigrationsAndValidate(
            TEST_DB, to, true,
            *WeatherDatabase.W_MIGRATION_SET
        )

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
    }

    @Throws(IOException::class)
    private fun locationsDBMigrate(from: Int, to: Int) {
        val dbName = "$TEST_DB-${LocationsDatabase::class.java.name}"

        var db = locationDBHelper.createDatabase(dbName, from).apply {
            // db has schema version 1. insert some data using SQL queries.
            // You cannot use DAO classes because they expect the latest schema.
            //execSQL(...)

            // Prepare for the next version.
            close()
        }

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        db = locationDBHelper.runMigrationsAndValidate(
            dbName, to, true,
            *LocationsDatabase.LOC_MIGRATION_SET
        )

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
    }

    @Throws(IOException::class)
    private fun tzdbDBMigrate(from: Int, to: Int) {
        val dbName = "$TEST_DB-${TZDatabase::class.java.name}"

        var db = tzdbDBHelper.createDatabase(dbName, from).apply {
            // db has schema version 1. insert some data using SQL queries.
            // You cannot use DAO classes because they expect the latest schema.
            //execSQL(...)

            // Prepare for the next version.
            close()
        }

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        db = tzdbDBHelper.runMigrationsAndValidate(
            dbName, to, true,
            *TZDatabase.TZDB_MIGRATION_SET
        )

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
    }
}