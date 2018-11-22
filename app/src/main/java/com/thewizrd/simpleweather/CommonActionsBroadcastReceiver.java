package com.thewizrd.simpleweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.gson.stream.JsonReader;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.simpleweather.wearable.WearableDataListenerService;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;
import com.thewizrd.simpleweather.widgets.WidgetUtils;

import java.io.StringReader;
import java.util.concurrent.Callable;

public class CommonActionsBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "CommonActionsBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            if (CommonActions.ACTION_SETTINGS_UPDATEAPI.equals(intent.getAction())) {
                context.startService(new Intent(context, WearableDataListenerService.class)
                        .setAction(WearableDataListenerService.ACTION_SENDSETTINGSUPDATE));
                WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                        .setAction(WeatherWidgetService.ACTION_UPDATEWEATHER));
            } else if (CommonActions.ACTION_SETTINGS_UPDATEGPS.equals(intent.getAction())) {
                context.startService(new Intent(context, WearableDataListenerService.class)
                        .setAction(WearableDataListenerService.ACTION_SENDSETTINGSUPDATE));
                context.startService(new Intent(context, WearableDataListenerService.class)
                        .setAction(WearableDataListenerService.ACTION_SENDLOCATIONUPDATE));
            } else if (CommonActions.ACTION_SETTINGS_UPDATEUNIT.equals(intent.getAction())) {
                WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                        .setAction(WeatherWidgetService.ACTION_UPDATEWEATHER));
            } else if (CommonActions.ACTION_SETTINGS_UPDATEREFRESH.equals(intent.getAction())) {
                WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                        .setAction(WeatherWidgetService.ACTION_UPDATEALARM));
            } else if (CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE.equals(intent.getAction())) {
                WearableDataListenerService.enqueueWork(context,
                        new Intent(context, WearableDataListenerService.class)
                                .setAction(WearableDataListenerService.ACTION_SENDLOCATIONUPDATE));
            } else if (CommonActions.ACTION_WEATHER_UPDATEWIDGETLOCATION.equals(intent.getAction())) {
                String oldKey = intent.getStringExtra("oldKey");
                String locationJson = intent.getStringExtra("location");
                LocationData location = LocationData.fromJson(new JsonReader(new StringReader(locationJson)));

                if (WidgetUtils.exists(oldKey)) {
                    WidgetUtils.updateWidgetIds(oldKey, location);
                }
            } else if (CommonActions.ACTION_WEATHER_UPDATEWIDGETWEATHER.equals(intent.getAction())) {
                final String locationQuery = intent.getStringExtra("locationQuery");
                String weatherJson = intent.getStringExtra("weather");
                final Weather weather = Weather.fromJson(new JsonReader(new StringReader(weatherJson)));

                new AsyncTask<Void>().await(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        if (WidgetUtils.exists(locationQuery)) {
                            Integer[] ids = WidgetUtils.getWidgetIds(locationQuery);
                            for (int id : ids) {
                                WidgetUtils.saveWeatherData(id, weather);
                            }
                        }
                        return null;
                    }
                });
            } else if (CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE.equals(intent.getAction())) {
                WearableDataListenerService.enqueueWork(context,
                        new Intent(context, WearableDataListenerService.class)
                                .setAction(WearableDataListenerService.ACTION_SENDWEATHERUPDATE));
            }

            Logger.writeLine(Log.INFO, "%s: Intent Action = %s", TAG, intent.getAction());
        }
    }
}
