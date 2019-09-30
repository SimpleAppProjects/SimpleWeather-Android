package com.thewizrd.simpleweather;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.multidex.MultiDex;

import com.jakewharton.threetenabp.AndroidThreeTen;
import com.thewizrd.shared_resources.AppState;
import com.thewizrd.shared_resources.ApplicationLib;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.receivers.CommonActionsBroadcastReceiver;

import static com.thewizrd.shared_resources.utils.Settings.loadIfNeeded;

public class App extends Application implements ApplicationLib, Application.ActivityLifecycleCallbacks {
    public static final int HOMEIDX = 0;

    private static ApplicationLib sInstance = null;

    private Context context;
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;
    private AppState applicationState;
    private int mActivitiesStarted;

    private CommonActionsBroadcastReceiver mCommonReceiver;

    public static synchronized ApplicationLib getInstance() {
        return sInstance;
    }

    @Override
    public Context getAppContext() {
        return context;
    }

    @Override
    public SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    public SharedPreferences.OnSharedPreferenceChangeListener getSharedPreferenceListener() {
        return sharedPreferenceChangeListener;
    }

    @Override
    public AppState getAppState() {
        return applicationState;
    }

    @Override
    public boolean isPhone() {
        return true;
    }

    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        sInstance = this;

        registerActivityLifecycleCallbacks(this);
        applicationState = AppState.CLOSED;
        mActivitiesStarted = 0;

        // Init shared library
        SimpleLibrary.init(this);
        AndroidThreeTen.init(this);

        // Start logger
        Logger.init(context);

        // Init common action broadcast receiver
        registerCommonReceiver();

        final Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                Logger.writeLine(Log.ERROR, e);

                if (oldHandler != null)
                    oldHandler.uncaughtException(t, e);
                else
                    System.exit(2);
            }
        });

        sharedPreferenceChangeListener = new Settings.SettingsListener(context);

        // Load data if needed
        loadIfNeeded();

        // Set Default Theme
        switch (Settings.getUserThemeMode()) {
            case FOLLOW_SYSTEM: // System
            default:
                if (Build.VERSION.SDK_INT >= 29)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                else
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                break;
            case DARK: // Dark
            case AMOLED_DARK: // Dark (AMOLED)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    private void registerCommonReceiver() {
        mCommonReceiver = new CommonActionsBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(CommonActions.ACTION_SETTINGS_UPDATEAPI);
        filter.addAction(CommonActions.ACTION_SETTINGS_UPDATEGPS);
        filter.addAction(CommonActions.ACTION_SETTINGS_UPDATEUNIT);
        filter.addAction(CommonActions.ACTION_SETTINGS_UPDATEREFRESH);
        filter.addAction(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE);
        filter.addAction(CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE);
        filter.addAction(CommonActions.ACTION_WEATHER_UPDATEWIDGETLOCATION);
        filter.addAction(CommonActions.ACTION_WEATHER_UPDATEWIDGETWEATHER);
        filter.addAction(CommonActions.ACTION_WIDGET_REFRESHWIDGETS);
        filter.addAction(CommonActions.ACTION_WIDGET_RESETWIDGETS);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mCommonReceiver, filter);
    }

    private void unregisterCommonReceiver() {
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(mCommonReceiver);
    }

    @Override
    public void onTerminate() {
        unregisterCommonReceiver();
        super.onTerminate();
        unregisterActivityLifecycleCallbacks(this);
        // Shutdown logger
        Logger.shutdown();
        SimpleLibrary.unRegister();
        sInstance = null;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (activity.getLocalClassName().contains("LaunchActivity") ||
                activity.getLocalClassName().contains("MainActivity")) {
            applicationState = AppState.FOREGROUND;
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (mActivitiesStarted == 0)
            applicationState = AppState.FOREGROUND;

        mActivitiesStarted++;
    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
        mActivitiesStarted--;

        if (mActivitiesStarted == 0)
            applicationState = AppState.BACKGROUND;
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (activity.getLocalClassName().contains("MainActivity")) {
            applicationState = AppState.CLOSED;
        }
    }
}
