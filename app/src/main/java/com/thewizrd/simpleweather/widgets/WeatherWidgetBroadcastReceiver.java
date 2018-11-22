package com.thewizrd.simpleweather.widgets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WeatherWidgetBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Relay intent to WeatherWidgetService
        intent.setClass(context, WeatherWidgetService.class);
        WeatherWidgetService.enqueueWork(context, intent);
    }
}
