package com.thewizrd.simpleweather.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.gson.stream.JsonReader;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker;
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
                WearableDataListenerService.enqueueWork(context, new Intent(context, WearableDataListenerService.class)
                        .setAction(WearableDataListenerService.ACTION_SENDSETTINGSUPDATE));
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEWEATHER);
            } else if (CommonActions.ACTION_SETTINGS_UPDATEGPS.equals(intent.getAction())) {
                WearableDataListenerService.enqueueWork(context, new Intent(context, WearableDataListenerService.class)
                        .setAction(WearableDataListenerService.ACTION_SENDSETTINGSUPDATE));
                WearableDataListenerService.enqueueWork(context, new Intent(context, WearableDataListenerService.class)
                        .setAction(WearableDataListenerService.ACTION_SENDLOCATIONUPDATE));
                WearableDataListenerService.enqueueWork(context, new Intent(context, WearableDataListenerService.class)
                        .setAction(WearableDataListenerService.ACTION_SENDWEATHERUPDATE));
            } else if (CommonActions.ACTION_SETTINGS_UPDATEUNIT.equals(intent.getAction())) {
                WearableDataListenerService.enqueueWork(context, new Intent(context, WearableDataListenerService.class)
                        .setAction(WearableDataListenerService.ACTION_SENDSETTINGSUPDATE));
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEWEATHER);
            } else if (CommonActions.ACTION_SETTINGS_UPDATEREFRESH.equals(intent.getAction())) {
                WearableDataListenerService.enqueueWork(context, new Intent(context, WearableDataListenerService.class)
                        .setAction(WearableDataListenerService.ACTION_SENDSETTINGSUPDATE));
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEWEATHER);
            } else if (CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE.equals(intent.getAction())) {
                WearableDataListenerService.enqueueWork(context,
                        new Intent(context, WearableDataListenerService.class)
                                .setAction(WearableDataListenerService.ACTION_SENDLOCATIONUPDATE));
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEWEATHER);
            } else if (CommonActions.ACTION_WEATHER_UPDATEWIDGETLOCATION.equals(intent.getAction())) {
                final String oldKey = intent.getStringExtra(Constants.WIDGETKEY_OLDKEY);
                final String locationJson = intent.getStringExtra(Constants.WIDGETKEY_LOCATION);

                new AsyncTask<Void>().await(new Callable<Void>() {
                    @Override
                    public Void call() {
                        LocationData location = LocationData.fromJson(new JsonReader(new StringReader(locationJson)));

                        if (WidgetUtils.exists(oldKey)) {
                            WidgetUtils.updateWidgetIds(oldKey, location);
                        }
                        return null;
                    }
                });
            } else if (CommonActions.ACTION_WEATHER_UPDATEWIDGETWEATHER.equals(intent.getAction())) {
                final String locationQuery = intent.getStringExtra(Constants.WIDGETKEY_LOCATIONQUERY);
                final String weatherJson = intent.getStringExtra(Constants.WIDGETKEY_WEATHER);

                new AsyncTask<Void>().await(new Callable<Void>() {
                    @Override
                    public Void call() {
                        Weather weather = Weather.fromJson(new JsonReader(new StringReader(weatherJson)));

                        if (WidgetUtils.exists(locationQuery)) {
                            int[] ids = WidgetUtils.getWidgetIds(locationQuery);
                            for (int id : ids) {
                                WidgetUtils.saveWeatherData(id, weather);
                            }
                        }
                        return null;
                    }
                });
            } else if (CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE.equals(intent.getAction())) {
                WearableDataListenerService.enqueueWork(context, new Intent(context, WearableDataListenerService.class)
                        .setAction(WearableDataListenerService.ACTION_SENDWEATHERUPDATE));
            } else if (CommonActions.ACTION_WIDGET_RESETWIDGETS.equals(intent.getAction())) {
                WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                        .setAction(WeatherWidgetService.ACTION_RESETGPSWIDGETS));
            } else if (CommonActions.ACTION_WIDGET_REFRESHWIDGETS.equals(intent.getAction())) {
                WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                        .setAction(WeatherWidgetService.ACTION_REFRESHGPSWIDGETS));
            }

            Logger.writeLine(Log.INFO, "%s: Intent Action = %s", TAG, intent.getAction());
        }
    }
}
