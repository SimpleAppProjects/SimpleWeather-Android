package com.thewizrd.shared_resources.tzdb;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.database.TZDatabase;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.StringUtils;

import java.util.concurrent.Callable;

public class TZDBCache {
    private static TZDatabase tzDB;

    private static final Migration TZDB_MIGRATION_0_5 = new Migration(0, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Since we didn't alter the table, there's nothing else to do here.
        }
    };

    private static final Migration TZDB_MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Since we didn't alter the table, there's nothing else to do here.
        }
    };

    private static final Migration TZDB_MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Since we didn't alter the table, there's nothing else to do here.
        }
    };

    private static final Migration[] TZDB_MIGRATION_SET = new Migration[]{
            TZDB_MIGRATION_0_5, TZDB_MIGRATION_5_6, TZDB_MIGRATION_6_7
    };

    public static String getTimeZone(final double latitude, final double longitude) {
        if (latitude != 0 && longitude != 0) {
            AnalyticsLogger.logEvent("TZDBCache: querying");
            // Initialize db if it hasn't been already
            if (tzDB == null) {
                Context context = SimpleLibrary.getInstance().getAppContext();
                tzDB = Room.databaseBuilder(context,
                        TZDatabase.class, "tzdb.db")
                        .addMigrations(TZDB_MIGRATION_SET)
                        .fallbackToDestructiveMigration()
                        .build();
            }

            // Search db if result already exists
            final String dbResult = AsyncTask.await(new Callable<String>() {
                @Override
                public String call() {
                    return tzDB.tzdbDAO().getTimeZoneData(latitude, longitude);
                }
            });

            if (!StringUtils.isNullOrWhitespace(dbResult))
                return dbResult;

            // Search tz lookup
            final String result = AsyncTask.await(new Callable<String>() {
                @Override
                public String call() {
                    return new TimeZoneProvider().getTimeZone(latitude, longitude);
                }
            });

            if (!StringUtils.isNullOrWhitespace(result)) {
                // Cache result
                AsyncTask.run(new Runnable() {
                    @Override
                    public void run() {
                        TZDB tzdb = new TZDB(latitude, longitude, result);
                        tzDB.tzdbDAO().insertTZData(tzdb);

                        // Run GC since tz lookup takes up a good chunk of memory
                        System.gc();
                    }
                });
            }

            return result;
        }

        return "UTC";
    }
}
