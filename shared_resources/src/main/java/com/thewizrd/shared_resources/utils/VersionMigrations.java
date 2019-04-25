package com.thewizrd.shared_resources.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.database.LocationsDatabase;
import com.thewizrd.shared_resources.database.WeatherDatabase;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

class VersionMigrations {
    static void performMigrations(final WeatherDatabase weatherDB, final LocationsDatabase locationDB) throws PackageManager.NameNotFoundException {
        Context context = SimpleLibrary.getInstance().getAppContext();
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

        @SuppressLint("MissingPermission")
        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);

        if (Settings.isWeatherLoaded() && Settings.getVersionCode() < packageInfo.versionCode) {
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

                    if (StringUtils.isNullOrWhitespace(wm.getAPIKey())) {
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
            Bundle bundle = new Bundle();
            bundle.putString("API", Settings.getAPI());
            bundle.putString("API_IsInternalKey", Boolean.toString(!Settings.usePersonalKey()));
            mFirebaseAnalytics.logEvent("App_Upgrading", bundle);
        }
        Settings.setVersionCode(packageInfo.versionCode);
    }
}
