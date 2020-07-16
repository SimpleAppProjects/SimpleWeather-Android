package com.thewizrd.simpleweather.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.main.MainActivity;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

public class WeatherNotificationBuilder {
    private static final String TAG = "WeatherNotificationBuilder";

    static Notification updateNotification(String notificationID, final WeatherNowViewModel viewModel) {
        Context context = App.getInstance().getAppContext();

        WeatherManager wm = WeatherManager.getInstance();

        // Build update
        RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.weather_notification_layout);

        String condition = viewModel.getCurCondition();
        String hiTemp = viewModel.getHiTemp();
        String loTemp = viewModel.getLoTemp();
        String temp = viewModel.getCurTemp() != null ?
                StringUtils.removeNonDigitChars(viewModel.getCurTemp().toString()) : "--";

        // Weather icon
        updateViews.setImageViewResource(R.id.weather_icon,
                WeatherUtils.getWeatherIconResource(viewModel.getWeatherIcon()));

        // Location Name
        updateViews.setTextViewText(R.id.location_name, viewModel.getLocation());

        // Condition text
        updateViews.setTextViewText(R.id.condition_weather,
                String.format("%sº - %s", !StringUtils.isNullOrWhitespace(temp) ? temp : "--", condition));

        // Details
        updateViews.setTextViewText(R.id.condition_details,
                String.format("%s | %s",
                        !StringUtils.isNullOrWhitespace(hiTemp) ? hiTemp : "--",
                        !StringUtils.isNullOrWhitespace(loTemp) ? loTemp : "--"));

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
            level = Integer.parseInt(temp.replace("º", ""));
        } catch (NumberFormatException e) {
            // Do nothing
        }

        int resId = level < 0 ? R.drawable.notification_temp_neg : R.drawable.notification_temp_pos;

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context, notificationID)
                        .setContent(updateViews)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setOngoing(true);

        if (Settings.getNotificationIcon().equals(Settings.TEMPERATURE_ICON))
            mBuilder.setSmallIcon(resId, Math.abs(level));
        else if (Settings.getNotificationIcon().equals(Settings.CONDITION_ICON))
            mBuilder.setSmallIcon(WeatherUtils.getWeatherIconResource(viewModel.getWeatherIcon()));

        Intent onClickIntent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent clickPendingIntent = PendingIntent.getActivity(context, 0, onClickIntent, 0);
        mBuilder.setContentIntent(clickPendingIntent);

        // Builds the notification and issues it.
        float temp_f = viewModel.getCurTemp().toString().endsWith(WeatherIcons.FAHRENHEIT) ?
                Float.parseFloat(temp) : ConversionMethods.CtoF(Float.parseFloat(temp));
        mBuilder.setColor(WeatherUtils.getColorFromTempF(temp_f));
        return mBuilder.build();
    }
}
