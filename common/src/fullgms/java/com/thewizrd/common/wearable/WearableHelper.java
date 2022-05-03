package com.thewizrd.common.wearable;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.wearable.PutDataRequest;
import com.thewizrd.shared_resources.utils.Logger;

public class WearableHelper {
    // Name of capability listed in Phone app's wear.xml
    public static final String CAPABILITY_PHONE_APP = "com.thewizrd.simpleweather_phone_app";
    // Name of capability listed in Wear app's wear.xml
    public static final String CAPABILITY_WEAR_APP = "com.thewizrd.simpleweather_wear_app";

    // For WearableListenerService
    public static final String StartActivityPath = "/start-activity";
    public static final String SettingsPath = "/settings";
    public static final String LocationPath = "/data/location";
    public static final String WeatherPath = "/data/weather";
    public static final String ErrorPath = "/error";
    public static final String IsSetupPath = "/isweatherloaded";
    public static final String PingPath = "/ping";

    public static boolean isGooglePlayServicesInstalled(@NonNull Context context) {
        final GoogleApiAvailability gPlayAvailability = GoogleApiAvailability.getInstance();

        final int queryResult = gPlayAvailability.isGooglePlayServicesAvailable(context);
        if (queryResult == ConnectionResult.SUCCESS) {
            Logger.writeLine(Log.INFO, "App: Google Play Services is installed on this device.");
            return true;
        }

        if (gPlayAvailability.isUserResolvableError(queryResult)) {
            String errorString = gPlayAvailability.getErrorString(queryResult);
            Logger.writeLine(Log.INFO,
                    "App: There is a problem with Google Play Services on this device: %s - %s",
                    queryResult, errorString);
        }

        return false;
    }

    public static Uri getWearDataUri(String NodeId, String Path) {
        return new Uri.Builder()
                .scheme(PutDataRequest.WEAR_URI_SCHEME)
                .authority(NodeId)
                .path(Path)
                .build();
    }
}
