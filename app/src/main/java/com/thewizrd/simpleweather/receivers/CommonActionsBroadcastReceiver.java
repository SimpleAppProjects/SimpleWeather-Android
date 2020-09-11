package com.thewizrd.simpleweather.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.simpleweather.services.ImageDatabaseWorker;
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker;
import com.thewizrd.simpleweather.wearable.WearableWorker;
import com.thewizrd.simpleweather.widgets.WeatherWidgetService;
import com.thewizrd.simpleweather.widgets.WidgetUtils;

import java.util.List;
import java.util.concurrent.Callable;

public class CommonActionsBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "CommonActionsBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            if (CommonActions.ACTION_SETTINGS_UPDATEAPI.equals(intent.getAction())) {
                WearableWorker.enqueueAction(context, WearableWorker.ACTION_SENDSETTINGSUPDATE);
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEWEATHER);
            } else if (CommonActions.ACTION_SETTINGS_UPDATEGPS.equals(intent.getAction())) {
                WearableWorker.enqueueAction(context, WearableWorker.ACTION_SENDUPDATE);
            } else if (CommonActions.ACTION_SETTINGS_UPDATEUNIT.equals(intent.getAction())) {
                WearableWorker.enqueueAction(context, WearableWorker.ACTION_SENDSETTINGSUPDATE);
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEWEATHER);
            } else if (CommonActions.ACTION_SETTINGS_UPDATEREFRESH.equals(intent.getAction())) {
                WearableWorker.enqueueAction(context, WearableWorker.ACTION_SENDSETTINGSUPDATE);
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEWEATHER);
            } else if (CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE.equals(intent.getAction())) {
                WearableWorker.enqueueAction(context, WearableWorker.ACTION_SENDLOCATIONUPDATE);
                if (intent.getBooleanExtra(CommonActions.EXTRA_FORCEUPDATE, true)) {
                    WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEWEATHER);
                }
            } else if (CommonActions.ACTION_WEATHER_UPDATEWIDGETLOCATION.equals(intent.getAction())) {
                final String oldKey = intent.getStringExtra(Constants.WIDGETKEY_OLDKEY);
                final String locationJson = intent.getStringExtra(Constants.WIDGETKEY_LOCATION);

                AsyncTask.await(new Callable<Void>() {
                    @Override
                    public Void call() {
                        LocationData location = JSONParser.deserializer(locationJson, LocationData.class);

                        if (WidgetUtils.exists(oldKey)) {
                            WidgetUtils.updateWidgetIds(oldKey, location);
                        }
                        return null;
                    }
                });
            } else if (CommonActions.ACTION_WEATHER_UPDATEWIDGETWEATHER.equals(intent.getAction())) {
                final String locationQuery = intent.getStringExtra(Constants.WIDGETKEY_LOCATIONQUERY);
                final String weatherJson = intent.getStringExtra(Constants.WIDGETKEY_WEATHER);

                AsyncTask.await(new Callable<Void>() {
                    @Override
                    public Void call() {
                        Weather weather = JSONParser.deserializer(weatherJson, Weather.class);

                        if (WidgetUtils.exists(locationQuery)) {
                            List<Integer> ids = WidgetUtils.getWidgetIds(locationQuery);
                            for (int id : ids) {
                                WidgetUtils.saveWeatherData(id, weather);
                            }
                        }
                        return null;
                    }
                });
            } else if (CommonActions.ACTION_WEATHER_SENDWEATHERUPDATE.equals(intent.getAction())) {
                WearableWorker.enqueueAction(context, WearableWorker.ACTION_SENDWEATHERUPDATE);
            } else if (CommonActions.ACTION_WIDGET_RESETWIDGETS.equals(intent.getAction())) {
                WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                        .setAction(WeatherWidgetService.ACTION_RESETGPSWIDGETS));
            } else if (CommonActions.ACTION_WIDGET_REFRESHWIDGETS.equals(intent.getAction())) {
                WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                        .setAction(WeatherWidgetService.ACTION_REFRESHGPSWIDGETS));
            } else if (CommonActions.ACTION_IMAGES_UPDATEWORKER.equals(intent.getAction())) {
                ImageDatabaseWorker.enqueueAction(context, ImageDatabaseWorker.ACTION_UPDATEALARM);
            }

            Logger.writeLine(Log.INFO, "%s: Intent Action = %s", TAG, intent.getAction());
        }
    }
}
