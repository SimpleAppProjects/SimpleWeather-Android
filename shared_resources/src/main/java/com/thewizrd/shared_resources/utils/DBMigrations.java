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
            // Since we didn't alter the table, there's nothing else to do here.
        }
    };

    static final Migration LOC_MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add locsource column to locations table
            database.execSQL("ALTER TABLE locations ADD COLUMN locsource TEXT");
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
