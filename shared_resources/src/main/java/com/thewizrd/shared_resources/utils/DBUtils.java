package com.thewizrd.shared_resources.utils;

import android.util.Log;

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.database.LocationsDatabase;
import com.thewizrd.shared_resources.database.WeatherDatabase;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import java.util.List;
import java.util.concurrent.Callable;

public class DBUtils {
    public static boolean weatherDataExists(final WeatherDatabase weatherDB) {
        return new AsyncTask<Boolean>().await(new Callable<Boolean>() {
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

    public static boolean locationDataExists(final LocationsDatabase locationDB) {
        return new AsyncTask<Boolean>().await(new Callable<Boolean>() {
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

    public static void setLocationData(final LocationsDatabase locationDB) {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                for (LocationData location : locationDB.locationsDAO().loadAllLocationData()) {
                    WeatherManager.getProvider(location.getSource())
                            .updateLocationData(location);
                }

                List<LocationData> result = locationDB.locationsDAO().loadAllLocationData();
            }
        });
    }
}
