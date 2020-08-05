package com.thewizrd.simpleweather.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherRequest;
import com.thewizrd.simpleweather.R;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class WeatherNotificationService extends JobIntentService {
    private static final String TAG = "WeatherNotificationService";

    // Actions
    public static final String ACTION_REFRESHNOTIFICATION = "SimpleWeather.Droid.action.REFRESH_NOTIFICATION";
    public static final String ACTION_REMOVENOTIFICATION = "SimpleWeather.Droid.action.REMOVE_NOTIFICATION";

    // Extras
    public static final String EXTRA_FORCEREFRESH = "SimpleWeather.Droid.extra.FORCE_REFRESH";

    // Sets an ID for the notification
    private static final String NOT_CHANNEL_ID = "SimpleWeather.ongoingweather";

    private static final int JOB_ID = 1003;
    private static final int PERSISTENT_NOT_ID = JOB_ID;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, WeatherNotificationService.class,
                JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (ACTION_REFRESHNOTIFICATION.equals(intent.getAction())) {
            final boolean forceRefresh = intent.getBooleanExtra(WeatherNotificationService.EXTRA_FORCEREFRESH, false);

            if (Settings.isWeatherLoaded()) {
                Weather weather = new AsyncTask<Weather>().await(new Callable<Weather>() {
                    @Override
                    public Weather call() {
                        LocationData locData = Settings.getHomeData();
                        WeatherDataLoader wLoader = new WeatherDataLoader(locData);
                        WeatherRequest.Builder request = new WeatherRequest.Builder();
                        if (forceRefresh)
                            request.forceRefresh(false);
                        else
                            request.forceLoadSavedData();
                        try {
                            return Tasks.await(wLoader.loadWeatherData(request.build()));
                        } catch (ExecutionException | InterruptedException e) {
                            return null;
                        }
                    }
                });

                if (Settings.showOngoingNotification() && weather != null) {
                    // Gets an instance of the NotificationManager service
                    NotificationManager mNotifyMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    initChannel(mNotifyMgr);

                    // Update notification
                    Notification mNotification = WeatherNotificationBuilder.updateNotification(NOT_CHANNEL_ID, new WeatherNowViewModel(weather));
                    mNotifyMgr.notify(PERSISTENT_NOT_ID, mNotification);
                } else if (!Settings.showOngoingNotification()) {
                    removeNotification();
                }
            }
        } else if (ACTION_REMOVENOTIFICATION.equals(intent.getAction())) {
            removeNotification();
        } else {
            Logger.writeLine(Log.INFO, "%s: Unhandled action: %s", TAG, intent.getAction());
        }
    }

    private void initChannel(NotificationManager mNotifyMgr) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = mNotifyMgr.getNotificationChannel(NOT_CHANNEL_ID);

            if (mChannel == null) {
                String notchannel_name = getResources().getString(R.string.not_channel_name_weather);
                String notchannel_desc = getResources().getString(R.string.not_channel_desc_weather);

                mChannel = new NotificationChannel(NOT_CHANNEL_ID, notchannel_name, NotificationManager.IMPORTANCE_LOW);
                mChannel.setDescription(notchannel_desc);
                // Configure the notification channel.
                mChannel.setShowBadge(true);
                mChannel.enableLights(false);
                mChannel.enableVibration(false);
                mNotifyMgr.createNotificationChannel(mChannel);
            }
        }
    }

    private void removeNotification() {
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(PERSISTENT_NOT_ID);
    }
}