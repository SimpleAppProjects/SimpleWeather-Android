package com.thewizrd.simpleweather.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.WeatherAlertViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ImageUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.MainActivity;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class WeatherAlertNotificationBuilder {
    private static final String LOG_TAG = "WeatherAlertNotificationBuilder";

    // Sets an ID for the notification
    private static final String TAG = "SimpleWeather.WeatherAlerts";
    private static final String NOT_CHANNEL_ID = "SimpleWeather.weatheralerts";
    private static final int MIN_GROUPCOUNT = 3;
    private static final int SUMMARY_ID = -1;

    public static void createNotifications(LocationData location, List<WeatherAlert> alerts) {
        final Context context = App.getInstance().getAppContext();
        // Gets an instance of the NotificationManager service
        NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(context);
        initChannel();

        // Create click intent
        // Start WeatherNow Activity with weather data
        Intent intent = new Intent(context, MainActivity.class)
                .setAction(WeatherWidgetService.ACTION_SHOWALERTS)
                .putExtra("data", location.toJson())
                .putExtra(WeatherWidgetService.ACTION_SHOWALERTS, true)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        // Build update
        for (WeatherAlert alert : alerts) {
            final WeatherAlertViewModel alertVM = new WeatherAlertViewModel(alert);

            String title = String.format("%s - %s", alertVM.getTitle(), location.getName());

            RemoteViews view = new RemoteViews(context.getPackageName(), R.layout.alert_notification_layout);
            // Alert icon
            view.setImageViewResource(R.id.alert_icon, WeatherUtils.getDrawableFromAlertType(alertVM.getAlertType()));

            // Alert Title
            view.setTextViewText(R.id.alert_title, title);

            // Alert text
            view.setTextViewText(R.id.alert_text, alertVM.getExpireDate());

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(context, NOT_CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_error_white)
                            .setContentTitle(title)
                            .setContentText(alertVM.getExpireDate())
                            .setCustomContentView(view)
                            .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                            .setContentIntent(clickPendingIntent)
                            .setOnlyAlertOnce(true)
                            .setAutoCancel(true)
                            .setColor(WeatherUtils.getColorFromAlertSeverity(alert.getSeverity()))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                // Tell service to remove stored notification
                mBuilder.setDeleteIntent(GetDeleteNotificationIntent(alertVM.getAlertType().getValue()));
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    || WeatherAlertNotificationService.getNotificationsCount() >= MIN_GROUPCOUNT) {
                mBuilder.setGroup(TAG);
            }

            // Builds the notification and issues it.
            // Tag: location.query; id: weather alert type
            int notId = (int) (SystemClock.uptimeMillis() + alertVM.getAlertType().getValue());
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                WeatherAlertNotificationService.addNotification(notId, title);
            mNotifyMgr.notify(TAG, notId, mBuilder.build());
        }

        boolean buildSummary = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                NotificationManager mNotifyMgrV23 = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                StatusBarNotification[] statNotifs = mNotifyMgrV23.getActiveNotifications();

                if (statNotifs != null && statNotifs.length > 0) {
                    int count = 0;
                    for (StatusBarNotification not : statNotifs) {
                        if (TAG.equals(not.getTag()))
                            count++;
                    }

                    buildSummary = count >= MIN_GROUPCOUNT;
                }
            } catch (Exception ex) {
                Logger.writeLine(Log.DEBUG, ex, "SimpleWeather: %s: error accessing notifications", LOG_TAG);
            }
        } else {
            buildSummary = WeatherAlertNotificationService.getNotificationsCount() >= MIN_GROUPCOUNT;
        }

        if (buildSummary) {
            // Notification inboxStyle for grouped notifications
            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                // Add active notification titles to summary notification
                for (Map.Entry<Integer, String> notif : WeatherAlertNotificationService.getNotifications()) {
                    mNotifyMgr.cancel(TAG, notif.getKey());
                    inboxStyle.addLine(notif.getValue());
                }

                inboxStyle.setBigContentTitle(context.getString(R.string.title_fragment_alerts));
                inboxStyle.setSummaryText(context.getString(R.string.app_name));
            } else {
                inboxStyle.setSummaryText(context.getString(R.string.title_fragment_alerts));
            }

            Bitmap iconBmp = new AsyncTask<Bitmap>().await(new Callable<Bitmap>() {
                @Override
                public Bitmap call() throws Exception {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        return ImageUtils.tintedBitmapFromDrawable(context, R.drawable.ic_error_white,
                                Colors.BLACK);
                    } else {
                        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                        Bitmap tintedBmp = ImageUtils.tintedBitmapFromDrawable(context, R.drawable.ic_error_white,
                                Colors.WHITE);
                        return Bitmap.createScaledBitmap(tintedBmp, (int) (24 * metrics.density), (int) (24 * metrics.density), false);
                    }
                }
            });

            NotificationCompat.Builder mSummaryBuilder =
                    new NotificationCompat.Builder(context, NOT_CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_error_white)
                            .setLargeIcon(iconBmp)
                            .setContentTitle(context.getString(R.string.title_fragment_alerts))
                            .setContentText(context.getString(R.string.app_name))
                            .setStyle(inboxStyle)
                            .setGroup(TAG)
                            .setGroupSummary(true)
                            .setContentIntent(clickPendingIntent)
                            .setOnlyAlertOnce(true)
                            .setAutoCancel(true)
                            .setColor(Colors.SIMPLEBLUE);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                mSummaryBuilder.setDeleteIntent(GetDeleteAllNotificationsIntent());

            // Builds the summary notification and issues it.
            mNotifyMgr.notify(TAG, SUMMARY_ID, mSummaryBuilder.build());
        }
    }

    private static PendingIntent GetDeleteNotificationIntent(int notId) {
        Context context = App.getInstance().getAppContext();
        Intent intent = new Intent(context, WeatherNotificationBroadcastReceiver.class)
                .setAction(WeatherAlertNotificationService.ACTION_CANCELNOTIFICATION)
                .putExtra(WeatherAlertNotificationService.EXTRA_NOTIFICATIONID, notId);

        // Use notification id as unique request code
        return PendingIntent.getBroadcast(context, notId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent GetDeleteAllNotificationsIntent() {
        Context context = App.getInstance().getAppContext();
        Intent intent = new Intent(context, WeatherNotificationBroadcastReceiver.class)
                .setAction(WeatherAlertNotificationService.ACTION_CANCELALLNOTIFICATIONS);

        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static void initChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Context context = App.getInstance().getAppContext();

            NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel mChannel = mNotifyMgr.getNotificationChannel(NOT_CHANNEL_ID);

            if (mChannel == null) {
                String notchannel_name = context.getResources().getString(R.string.not_channel_name_alerts);
                String notchannel_desc = context.getResources().getString(R.string.not_channel_desc_alerts);

                mChannel = new NotificationChannel(NOT_CHANNEL_ID, notchannel_name, NotificationManager.IMPORTANCE_DEFAULT);
                mChannel.setDescription(notchannel_desc);
                // Configure the notification channel.
                mChannel.setShowBadge(true);
                mChannel.enableLights(true);
                mChannel.enableVibration(false);
                mNotifyMgr.createNotificationChannel(mChannel);
            }
        }
    }
}
