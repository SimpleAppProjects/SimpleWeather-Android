package com.thewizrd.simpleweather.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WeatherAlertNotificationBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Relay intent to WeatherAlertNotificationService
        intent.setClass(context, WeatherAlertNotificationService.class);
        WeatherAlertNotificationService.enqueueWork(context, intent);
    }
}
