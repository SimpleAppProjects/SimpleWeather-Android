package com.thewizrd.simpleweather.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WeatherNotificationBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Relay intent to Weather(Alert)NotificationService
        if (WeatherNotificationService.ACTION_REFRESHNOTIFICATION.equals(intent.getAction())) {
            intent.setClass(context, WeatherNotificationService.class);
            WeatherNotificationService.enqueueWork(context, intent);
        } else {
            intent.setClass(context, WeatherAlertNotificationService.class);
            WeatherAlertNotificationService.enqueueWork(context, intent);
        }
    }
}
