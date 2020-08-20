package com.thewizrd.simpleweather.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WeatherNotificationBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Relay intent to Weather(Alert)NotificationService
        if (WeatherAlertNotificationService.ACTION_CANCELNOTIFICATION.equals(intent.getAction()) ||
                WeatherAlertNotificationService.ACTION_CANCELALLNOTIFICATIONS.equals(intent.getAction())) {
            intent.setClass(context, WeatherAlertNotificationService.class);
            WeatherAlertNotificationService.enqueueWork(context, intent);
        } else {
            intent.setClass(context, WeatherNotificationWorker.class);
            WeatherNotificationWorker.enqueueAction(context, intent);
        }
    }
}
