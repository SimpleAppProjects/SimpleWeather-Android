package com.thewizrd.simpleweather.notifications;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.MainActivity;
import com.thewizrd.simpleweather.R;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

public class WeatherNotificationBuilder {
    private static final String TAG = "WeatherNotificationBuilder";

    // Sets an ID for the notification
    static final int PERSISTENT_NOT_ID = 1;
    private static final String NOT_CHANNEL_ID = "SimpleWeather.ongoingweather";

    private static Notification mNotification;
    private static boolean isShowing = false;

    public static void updateNotification(Weather weather) {
        Context context = App.getInstance().getAppContext();

        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        initChannel(mNotifyMgr);

        WeatherManager wm = WeatherManager.getInstance();

        // Build update
        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.notification_layout);

        String temp = (Settings.isFahrenheit() ?
                Math.round(weather.getCondition().getTempF()) : Math.round(weather.getCondition().getTempC())) + "º";
        String condition = weather.getCondition().getWeather();
        String hiTemp;
        String loTemp;
        try {
            hiTemp = (Settings.isFahrenheit() ?
                    Math.round(Double.valueOf(weather.getForecast()[0].getHighF())) : Math.round(Double.valueOf(weather.getForecast()[0].getHighC()))) + "º";
        } catch (NumberFormatException nFe) {
            hiTemp = "--º";
            Logger.writeLine(Log.ERROR, nFe);
        }
        try {
            loTemp = (Settings.isFahrenheit() ?
                    Math.round(Double.valueOf(weather.getForecast()[0].getLowF())) : Math.round(Double.valueOf(weather.getForecast()[0].getLowC()))) + "º";
        } catch (NumberFormatException nFe) {
            loTemp = "--º";
            Logger.writeLine(Log.ERROR, nFe);
        }

        // Weather icon
        updateViews.setImageViewResource(R.id.weather_icon,
                wm.getWeatherIconResource(weather.getCondition().getIcon()));

        // Location Name
        updateViews.setTextViewText(R.id.location_name, weather.getLocation().getName());

        // Condition text
        updateViews.setTextViewText(R.id.condition_weather, String.format("%s - %s", temp, condition));

        // Details
        updateViews.setTextViewText(R.id.condition_details,
                String.format("%s / %s", hiTemp, loTemp));

        // Update Time
        String timeformat = LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a"));

        if (DateFormat.is24HourFormat(context))
            timeformat = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        updateViews.setTextViewText(R.id.update_time, timeformat);

        // Progress bar
        updateViews.setViewVisibility(R.id.refresh_button, View.VISIBLE);
        updateViews.setViewVisibility(R.id.refresh_progress, View.GONE);
        Intent refreshClickIntent = new Intent(context, WeatherNotificationBroadcastReceiver.class)
                .setAction(WeatherNotificationService.ACTION_REFRESHNOTIFICATION)
                .putExtra(WeatherNotificationService.EXTRA_FORCEREFRESH, true);
        PendingIntent prgPendingIntent = PendingIntent.getBroadcast(context, 0, refreshClickIntent, 0);
        updateViews.setOnClickPendingIntent(R.id.refresh_button, prgPendingIntent);

        int level = 0;
        try {
            level = Integer.valueOf(temp.replace("º", ""));
        } catch (NumberFormatException e) {
            // Do nothing
        }

        int resId = level < 0 ? R.drawable.notification_temp_neg : R.drawable.notification_temp_pos;

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context, NOT_CHANNEL_ID)
                        .setContent(updateViews)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setOngoing(true);

        if (Settings.getNotificationIcon().equals(Settings.TEMPERATURE_ICON))
            mBuilder.setSmallIcon(resId, Math.abs(level));
        else if (Settings.getNotificationIcon().equals(Settings.CONDITION_ICON))
            mBuilder.setSmallIcon(wm.getWeatherIconResource(weather.getCondition().getIcon()));

        Intent onClickIntent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(context, 0, onClickIntent, 0);
        mBuilder.setContentIntent(clickPendingIntent);

        // Builds the notification and issues it.
        mNotification = mBuilder.build();
        mNotifyMgr.notify(PERSISTENT_NOT_ID, mNotification);
        isShowing = true;
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
            }
        }

        return mNotification;
    }

    public static void showRefresh(boolean show) {
        // Gets an instance of the NotificationManager service
        Context context = App.getInstance().getAppContext();
        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        getNotification();

        // Build update
        RemoteViews updateViews = null;

        if (mNotification.contentView == null)
            updateViews = new RemoteViews(context.getPackageName(), R.layout.notification_layout);
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

    public static boolean isShowing() {
        return isShowing;
    }
}
