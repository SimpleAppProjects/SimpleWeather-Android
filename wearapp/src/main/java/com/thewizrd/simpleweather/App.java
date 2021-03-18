package com.thewizrd.simpleweather;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.thewizrd.extras.ExtrasLibrary;
import com.thewizrd.shared_resources.AppState;
import com.thewizrd.shared_resources.ApplicationLib;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.receivers.CommonActionsBroadcastReceiver;

public class App extends Application implements ApplicationLib, Application.ActivityLifecycleCallbacks {
    private static ApplicationLib sInstance = null;

    private Context context;
    private Bundle appProperties;
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
        return false;
    }

    @Override
    public Bundle getProperties() {
        return appProperties;
    }

    public void onCreate() {
        super.onCreate();
        context = LocaleUtils.attachBaseContext(getApplicationContext());
        appProperties = new Bundle();
        sInstance = this;

        registerActivityLifecycleCallbacks(this);
        applicationState = AppState.CLOSED;
        mActivitiesStarted = 0;

        sharedPreferenceChangeListener = new Settings.SettingsListener(context);

        // Init shared library
        SimpleLibrary.init(this);
        ExtrasLibrary.Companion.initialize(this);

        // Start logger
        Logger.init(context);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        FirebaseCrashlytics.getInstance().sendUnsentReports();
        FirebaseAnalytics.getInstance(context).setUserProperty("device_type", "watch");

        FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        firebaseRemoteConfig.setDefaultsAsync(com.thewizrd.shared_resources.R.xml.remote_config_defaults);

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

        // Load data if needed
        Settings.loadIfNeeded();
    }

    private void registerCommonReceiver() {
        mCommonReceiver = new CommonActionsBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(CommonActions.ACTION_SETTINGS_UPDATEAPI);
        filter.addAction(CommonActions.ACTION_SETTINGS_UPDATEGPS);
        filter.addAction(CommonActions.ACTION_SETTINGS_UPDATEUNIT);
        filter.addAction(CommonActions.ACTION_SETTINGS_UPDATEDATASYNC);
        filter.addAction(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE);

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
    public void onActivityStarted(@NonNull Activity activity) {
        if (mActivitiesStarted == 0)
            applicationState = AppState.FOREGROUND;

        mActivitiesStarted++;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        mActivitiesStarted--;

        if (mActivitiesStarted == 0)
            applicationState = AppState.BACKGROUND;
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        if (activity.getLocalClassName().contains("MainActivity")) {
            applicationState = AppState.CLOSED;
        }
    }
}
