package com.thewizrd.simpleweather.notifications;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class WeatherNotificationService extends Service {
    private static final String TAG = "WeatherNotificationService";

    // Actions
    public static final String ACTION_REFRESHNOTIFICATION = "SimpleWeather.Droid.action.REFRESH_NOTIFICATION";
    public static final String ACTION_REMOVENOTIFICATION = "SimpleWeather.Droid.action.REMOVE_NOTIFICATION";
    public static final String ACTION_SHOWREFRESH = "SimpleWeather.Droid.action.SHOW_REFRESH";
    public static final String ACTION_STOPREFRESH = "SimpleWeather.Droid.action.STOP_REFRESH";

    // Extras
    public static final String EXTRA_FORCEREFRESH = "SimpleWeather.Droid.extra.FORCE_REFRESH";

    // Sets an ID for the notification
    private static final String NOT_CHANNEL_ID = "SimpleWeather.ongoingweather";

    private Context mContext;
    private static Notification mNotification;
    private static boolean isShowing = false;

    private static final int JOB_ID = 1003;
    private static final int PERSISTENT_NOT_ID = JOB_ID;

    public static void enqueueWork(Context context, Intent work) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(work);
        } else {
            context.startService(work);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this.getApplicationContext();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        startForeground(JOB_ID, getNotification());

        Tasks.call(Executors.newSingleThreadExecutor(), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                if (intent != null && ACTION_REFRESHNOTIFICATION.equals(intent.getAction())) {
                    showRefresh(true);

                    final boolean forceRefresh = intent.getBooleanExtra(WeatherNotificationService.EXTRA_FORCEREFRESH, false);

                    if (Settings.isWeatherLoaded()) {
                        Weather weather = new AsyncTask<Weather>().await(new Callable<Weather>() {
                            @Override
                            public Weather call() throws Exception {
                                LocationData locData = Settings.getHomeData();
                                WeatherDataLoader wLoader = new WeatherDataLoader(locData);
                                if (forceRefresh)
                                    wLoader.loadWeatherData(false);
                                else
                                    wLoader.forceLoadSavedWeatherData();

                                return wLoader.getWeather();
                            }
                        });

                        if (Settings.showOngoingNotification() && weather != null) {
                            // Gets an instance of the NotificationManager service
                            NotificationManager mNotifyMgr = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                            initChannel(mNotifyMgr);

                            // Update notification
                            mNotification = WeatherNotificationBuilder.updateNotification(NOT_CHANNEL_ID, new WeatherNowViewModel(weather));

                            mNotifyMgr.notify(PERSISTENT_NOT_ID, mNotification);
                            isShowing = true;
                        }
                    }
                } else if (intent != null && (ACTION_SHOWREFRESH.equals(intent.getAction()) || ACTION_STOPREFRESH.equals(intent.getAction()))) {
                    showRefresh(ACTION_SHOWREFRESH.equals(intent.getAction()));
                } else if (intent != null && ACTION_REMOVENOTIFICATION.equals(intent.getAction())) {
                    removeNotification();
                    ServiceCompat.stopForeground(WeatherNotificationService.this, ServiceCompat.STOP_FOREGROUND_REMOVE);
                    stopSelf();
                } else if (intent != null) {
                    Logger.writeLine(Log.INFO, "%s: Unhandled action: %s", TAG, intent.getAction());
                }
                return null;
            }
        });

        if (intent != null && ACTION_REMOVENOTIFICATION.equals(intent.getAction())) {
            return START_NOT_STICKY;
        } else {
            return START_STICKY;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static void initChannel(NotificationManager mNotifyMgr) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = mNotifyMgr.getNotificationChannel(NOT_CHANNEL_ID);

            if (mChannel == null) {
                Context context = App.getInstance().getAppContext();

                String notchannel_name = context.getResources().getString(R.string.not_channel_name_weather);
                String notchannel_desc = context.getResources().getString(R.string.not_channel_desc_weather);

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

    @SuppressLint("NewApi")
    static Notification getNotification() {
        // Gets an instance of the NotificationManager service
        Context context = App.getInstance().getAppContext();
        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        initChannel(mNotifyMgr);

        if (mNotification == null) {
            try {
                StatusBarNotification statNot = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    StatusBarNotification[] statNots = mNotifyMgr.getActiveNotifications();
                    if (statNots != null && statNots.length > 0) {
                        for (StatusBarNotification not : statNots) {
                            if (not.getId() == PERSISTENT_NOT_ID) {
                                statNot = not;
                                break;
                            }
                        }
                    }
                }

                if (statNot != null && statNot.getNotification() != null)
                    mNotification = statNot.getNotification();
                else {
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(context, NOT_CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_logo)
                                    .setPriority(NotificationCompat.PRIORITY_LOW)
                                    .setOnlyAlertOnce(true)
                                    .setOngoing(true);

                    mNotification = mBuilder.build();
                }
            } catch (Exception ex) {
                Logger.writeLine(Log.DEBUG, ex, "SimpleWeather: %s: error access notifications");
            } finally {
                if (mNotification == null) {
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(context, NOT_CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ic_logo)
                                    .setPriority(NotificationCompat.PRIORITY_LOW)
                                    .setOnlyAlertOnce(true)
                                    .setOngoing(true);

                    mNotification = mBuilder.build();
                } else if (mNotification.contentView == null) {
                    mNotification.contentView = new RemoteViews(context.getPackageName(), R.layout.weather_notification_layout);
                }
            }
        }

        return mNotification;
    }

    static void showRefresh(boolean show) {
        // Gets an instance of the NotificationManager service
        Context context = App.getInstance().getAppContext();
        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        getNotification();

        // Build update
        RemoteViews updateViews = null;

        if (mNotification.contentView == null)
            updateViews = new RemoteViews(context.getPackageName(), R.layout.weather_notification_layout);
        else
            updateViews = mNotification.contentView;

        updateViews.setViewVisibility(R.id.refresh_button, show ? View.GONE : View.VISIBLE);
        updateViews.setViewVisibility(R.id.refresh_progress, show ? View.VISIBLE : View.GONE);

        mNotification.contentView = updateViews;

        // Builds the notification and issues it.
        mNotifyMgr.notify(PERSISTENT_NOT_ID, mNotification);
        isShowing = true;
    }

    static void removeNotification() {
        Context context = App.getInstance().getAppContext();
        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotifyMgr.cancel(PERSISTENT_NOT_ID);
        isShowing = false;
    }

    public static boolean isNotificationShowing() {
        return isShowing;
    }
}