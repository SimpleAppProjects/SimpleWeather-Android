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
            // v1.3.7 - Yahoo (YQL) is no longer in service
            // Update location data from HERE Geocoder service
            if (WeatherAPI.HERE.equals(Settings.getAPI()) && Settings.getVersionCode() < 271370400) {
                if (Settings.IS_PHONE) {
                    DBUtils.setLocationData(locationDB, WeatherAPI.HERE);
                    Settings.saveLastGPSLocData(new LocationData());
                } else if (Settings.getLastGPSLocData() != null) {
                    WeatherManager.getProvider(WeatherAPI.HERE)
                            .updateLocationData(Settings.getLastGPSLocData());
                }
            }

            // v1.3.8+ - Yahoo API is back in service (but updated)
            // Update location data from current geocoder service
            if (WeatherAPI.YAHOO.equals(Settings.getAPI()) && Settings.getVersionCode() < 271380000) {
                if (Settings.IS_PHONE) {
                    DBUtils.setLocationData(locationDB, WeatherAPI.YAHOO);
                    Settings.saveLastGPSLocData(new LocationData());
                } else if (Settings.getLastGPSLocData() != null) {
                    WeatherManager.getProvider(WeatherAPI.YAHOO)
                            .updateLocationData(Settings.getLastGPSLocData());
                }
            }

            // v1.3.8+
            if (Settings.getVersionCode() < 271380100) {
                // Added Onboarding Wizard
                Settings.setOnBoardingComplete(true);

                // The current WeatherUnderground API is no longer in service
                // Disable this provider and migrate to HERE
                if (WeatherAPI.WEATHERUNDERGROUND.equals(Settings.getAPI())) {
                    // Set default API to HERE
                    Settings.setAPI(WeatherAPI.HERE);
                    WeatherManager wm = WeatherManager.getInstance();
                    wm.updateAPI();

                    if (wm.isKeyRequired() && StringUtils.isNullOrWhitespace(wm.getAPIKey())) {
                        // If (internal) key doesn't exist, fallback to Yahoo
                        Settings.setAPI(WeatherAPI.YAHOO);
                        wm.updateAPI();
                        Settings.setPersonalKey(true);
                        Settings.setKeyVerified(false);
                    } else {
                        // If key exists, go ahead
                        Settings.setPersonalKey(false);
                        Settings.setKeyVerified(true);
                    }

                    if (Settings.IS_PHONE) {
                        DBUtils.setLocationData(locationDB, WeatherAPI.HERE);
                        Settings.saveLastGPSLocData(new LocationData());
                    } else if (Settings.getLastGPSLocData() != null) {
                        WeatherManager.getProvider(WeatherAPI.HERE)
                                .updateLocationData(Settings.getLastGPSLocData());
                    }
                }
            }

            // v3.0.0+
            if (Settings.getVersionCode() < 293000000) {
                // Update Met.no and OWM to use LocationIQ
                final String API = Settings.getAPI();
                if (WeatherAPI.METNO.equals(API) || WeatherAPI.OPENWEATHERMAP.equals(API)) {
                    if (Settings.IS_PHONE) {
                        DBUtils.setLocationData(locationDB, API);
                        Settings.saveLastGPSLocData(new LocationData());
                    } else if (Settings.getLastGPSLocData() != null) {
                        WeatherManager.getProvider(API)
                                .updateLocationData(Settings.getLastGPSLocData());
                    }
                }
            }

            // v4.2.0+ (Units)
            if (Settings.getVersionCode() < 294200000) {
                final String tempUnit = Settings.getTemperatureUnit();
                if (Units.CELSIUS.equals(tempUnit)) {
                    Settings.setDefaultUnits(Units.CELSIUS);
                } else {
                    Settings.setDefaultUnits(Units.FAHRENHEIT);
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
