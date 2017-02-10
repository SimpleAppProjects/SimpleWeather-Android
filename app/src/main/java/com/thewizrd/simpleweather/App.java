package com.thewizrd.simpleweather;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by bryan on 2/8/2017.
 */

public class App extends Application {
    private static Context context;

    public void onCreate(){
        super.onCreate();
        context = getApplicationContext();
    }

    public static SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static ContentResolver getCntentResolver() {
        return context.getContentResolver();
    }

    public static Context getAppContext() {
        return context;
    }
}
