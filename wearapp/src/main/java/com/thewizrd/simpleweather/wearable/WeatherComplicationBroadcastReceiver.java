package com.thewizrd.simpleweather.wearable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WeatherComplicationBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Relay intent to WeatherComplicationIntentService
        intent.setClass(context, WeatherComplicationIntentService.class);
        WeatherComplicationIntentService.enqueueWork(context, intent);
    }
}
