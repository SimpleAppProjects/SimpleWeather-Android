package com.thewizrd.simpleweather.widgets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WeatherWidgetBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            WeatherWidgetService.enqueueWork(context, new Intent(WeatherWidgetService.ACTION_UPDATEWEATHER));
            return;
        }

        // Relay intent to WeatherWidgetService
        intent.setClass(context, WeatherWidgetService.class);
        WeatherWidgetService.enqueueWork(context, intent);
    }
}
