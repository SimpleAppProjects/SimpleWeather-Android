package com.thewizrd.simpleweather.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.thewizrd.simpleweather.services.AppUpdaterWorker;
import com.thewizrd.simpleweather.services.ImageDatabaseWorker;
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker;
import com.thewizrd.simpleweather.services.WidgetUpdaterWorker;

public class WeatherUpdateBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()) ||
                Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_STARTALARM);
            WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_STARTALARM);

            if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
                ImageDatabaseWorker.enqueueAction(context, ImageDatabaseWorker.ACTION_STARTALARM);

                AppUpdaterWorker.registerWorker(context);
            }
        }
    }
}
