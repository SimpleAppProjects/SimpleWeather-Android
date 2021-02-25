package com.thewizrd.shared_resources.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.util.Log;

import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.database.LocationsDatabase;
import com.thewizrd.shared_resources.database.WeatherDatabase;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.preferences.FeatureSettings;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

class VersionMigrations {
    static void performMigrations(final WeatherDatabase weatherDB, final LocationsDatabase locationDB) {
        Context context = SimpleLibrary.getInstance().getAppContext();
        long versionCode = 0;
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionCode = packageInfo.versionCode;
        } catch (Exception e) {
            Logger.writeLine(Log.DEBUG, e);
        }

        if (Settings.isWeatherLoaded() && Settings.getVersionCode() < versionCode) {
            // v4.2.0+ (Units)
            if (Settings.getVersionCode() < 294200000) {
                final String tempUnit = Settings.getTemperatureUnit();
                if (Units.CELSIUS.equals(tempUnit)) {
                    Settings.setDefaultUnits(Units.CELSIUS);
                } else {
                    Settings.setDefaultUnits(Units.FAHRENHEIT);
                }

                if (!SimpleLibrary.getInstance().getApp().isPhone()) {
                    Settings.setRefreshInterval(Settings.DEFAULTINTERVAL);
                }
            }

            if (Settings.getVersionCode() < 294310000) {
                if (WeatherAPI.HERE.equals(Settings.getAPI())) {
                    // Set default API to Yahoo
                    Settings.setAPI(WeatherAPI.YAHOO);
                    WeatherManager wm = WeatherManager.getInstance();
                    wm.updateAPI();

                    Settings.setPersonalKey(false);
                    Settings.setKeyVerified(true);
                }
            }

            if (Settings.getVersionCode() < 294320000) {
                // Update location keys
                // NWS key is different now
                if (Settings.IS_PHONE) {
                    DBUtils.updateLocationKey(locationDB);
                }
                Settings.saveLastGPSLocData(new LocationData());
            }

            if (Settings.getVersionCode() < 295000000) {
                if (WeatherAPI.YAHOO.equals(Settings.getAPI()) || WeatherAPI.HERE.equals(Settings.getAPI())) {
                    // Yahoo Weather API is no longer in service
                    // Set default API to WeatherUnlocked
                    Settings.setAPI(WeatherAPI.WEATHERUNLOCKED);
                    WeatherManager wm = WeatherManager.getInstance();
                    wm.updateAPI();

                    Settings.setPersonalKey(false);
                    Settings.setKeyVerified(true);
                }
            }

            Bundle bundle = new Bundle();
            bundle.putString("API", Settings.getAPI());
            bundle.putString("API_IsInternalKey", Boolean.toString(!Settings.usePersonalKey()));
            bundle.putLong("VersionCode", Settings.getVersionCode());
            bundle.putLong("CurrentVersionCode", versionCode);
            AnalyticsLogger.logEvent("App_Upgrading", bundle);
        }

        if (versionCode > 0) {
            if (Settings.getVersionCode() < versionCode) {
                FeatureSettings.setUpdateAvailable(false);
            }
            Settings.setVersionCode(versionCode);
        }
    }
}
