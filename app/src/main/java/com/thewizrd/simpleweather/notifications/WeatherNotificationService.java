package com.thewizrd.simpleweather.notifications;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class WeatherNotificationService extends JobIntentService {
    private static final String TAG = "WeatherNotificationService";

    // Actions
    public static final String ACTION_REFRESHNOTIFICATION = "SimpleWeather.Droid.action.REFRESH_NOTIFICATION";
    public static final String ACTION_REMOVENOTIFICATION = "SimpleWeather.Droid.action.REMOVE_NOTIFICATION";

    // Extras
    public static final String EXTRA_FORCEREFRESH = "SimpleWeather.Droid.extra.FORCE_REFRESH";

    private static final int JOB_ID = 1002;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, WeatherNotificationService.class,
                JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull final Intent intent) {
        Tasks.call(Executors.newSingleThreadExecutor(), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (ACTION_REFRESHNOTIFICATION.equals(intent.getAction())) {
                    WeatherNotificationBuilder.showRefresh(true);
                    WeatherWidgetService.enqueueWork(getApplicationContext(), intent);
                } else {
                    Logger.writeLine(Log.INFO, "%s: Unhandled action: %s", TAG, intent.getAction());
                }
                return null;
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (ACTION_REMOVENOTIFICATION.equals(intent.getAction())) {
                    WeatherNotificationBuilder.removeNotification();
                }
            }
        });
    }
}