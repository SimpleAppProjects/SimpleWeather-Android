package com.thewizrd.shared_resources.utils;

import android.util.Log;

import com.thewizrd.shared_resources.database.LocationsDatabase;
import com.thewizrd.shared_resources.database.WeatherDatabase;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import java.util.List;
import java.util.concurrent.Callable;

class DBUtils {
    static boolean weatherDataExists(final WeatherDatabase weatherDB) {
        return AsyncTask.await(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    int count = weatherDB.weatherDAO().getWeatherDataCount();
                    return count > 0;
                } catch (Exception e) {
                    Logger.writeLine(Log.ERROR, e);
                    return false;
                }
            }
        });
    }

    static boolean locationDataExists(final LocationsDatabase locationDB) {
        return AsyncTask.await(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                try {
                    locationDB.locationsDAO().getFavoritesCount();
                    int count = locationDB.locationsDAO().getLocationDataCount();
                    return count > 0;
                } catch (Exception e) {
                    Logger.writeLine(Log.ERROR, e);
                    return false;
                }
            }
        });
    }

    static void setLocationData(final LocationsDatabase locationDB, final String API) {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                for (LocationData location : locationDB.locationsDAO().loadAllLocationData()) {
                    WeatherManager.getProvider(API)
                            .updateLocationData(location);
                }

                List<LocationData> result = locationDB.locationsDAO().loadAllLocationData();
            }
        });
    }

    static void updateLocationKey(final LocationsDatabase locationDB) {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                for (LocationData location : locationDB.locationsDAO().loadAllLocationData()) {
                    String oldKey = location.getQuery();

                    location.setQuery(WeatherManager.getProvider(location.getWeatherSource())
                            .updateLocationQuery(location));

                    Settings.updateLocationWithKey(location, oldKey);
                }
            }
        });
    }
}
