package com.thewizrd.simpleweather.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.services.WeatherUpdaterWorker;

public abstract class WeatherWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "WeatherWidgetProvider";

    // Actions
    public static final String ACTION_REFRESHWIDGETS = "SimpleWeather.Droid.action.UPDATEWIDGETS";
    public static final String ACTION_SHOWNEXTFORECAST = "SimpleWeather.Droid.action.SHOW_NEXT_FORECAST";

    // Extras
    public static final String EXTRA_WIDGET_ID = "SimpleWeather.Droid.extra.WIDGET_ID";
    public static final String EXTRA_WIDGET_IDS = "SimpleWeather.Droid.extra.WIDGET_IDS";
    public static final String EXTRA_WIDGET_OPTIONS = "SimpleWeather.Droid.extra.WIDGET_OPTIONS";
    public static final String EXTRA_WIDGET_TYPE = "SimpleWeather.Droid.extra.WIDGET_TYPE";

    // Fields
    public abstract WidgetType getWidgetType();

    public abstract int getWidgetLayoutId();

    protected abstract String getClassName();

    public abstract ComponentName getComponentName();

    // Methods
    public abstract boolean hasInstances(Context context);

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                // Reset weather update time
                Settings.setUpdateTime(DateTimeUtils.getLocalDateTimeMIN());
                // Restart update alarm
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEALARM);
            } else if (Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction())) {
                updateWidgets(context, null);
            } else if (ACTION_REFRESHWIDGETS.equals(intent.getAction())) {
                // Update widgets
                int[] appWidgetIds = intent.getIntArrayExtra(EXTRA_WIDGET_IDS);
                updateWidgets(context, appWidgetIds);
            } else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
                // Update widgets
                int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                updateWidgets(context, appWidgetIds);
            } else {
                super.onReceive(context, intent);
            }

            Logger.writeLine(Log.INFO, "%s: WidgetType: %s; onReceive: %s", TAG, getWidgetType().name(), intent.getAction());
        }
    }

    protected void updateWidgets(Context context, final int[] appWidgetIds) {
        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                Context context = App.getInstance().getAppContext();

                if (appWidgetIds != null && appWidgetIds.length > 0) {
                    for (int appWidgetId : appWidgetIds) {
                        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                        LocationData location = WidgetUtils.getLocationData(appWidgetId);

                        RemoteViews updateViews = new RemoteViews(context.getPackageName(), getWidgetLayoutId());

                        if (location != null) {
                            WeatherWidgetService.setOnSettingsClickIntent(context, updateViews, location, appWidgetId);
                        }

                        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, updateViews);
                    }
                }
            }
        });

        WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                .setAction(WeatherWidgetService.ACTION_REFRESHWIDGET)
                .putExtra(EXTRA_WIDGET_IDS, appWidgetIds)
                .putExtra(EXTRA_WIDGET_TYPE, getWidgetType().getValue()));
    }

    @Override
    public void onUpdate(Context context,
                         AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateWidgets(context, appWidgetIds);
    }

    @Override
    public void onEnabled(Context context) {
        // Schedule alarms/updates
        WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEALARM);
    }

    @Override
    public void onDisabled(Context context) {
        // Remove alarms/updates
        WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_CANCELALARM);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                .setAction(WeatherWidgetService.ACTION_RESIZEWIDGET)
                .putExtra(EXTRA_WIDGET_ID, appWidgetId)
                .putExtra(EXTRA_WIDGET_OPTIONS, newOptions)
                .putExtra(EXTRA_WIDGET_TYPE, getWidgetType().getValue()));
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);

        for (int id : appWidgetIds) {
            // Remove id from list
            WidgetUtils.deleteWidget(id);
        }
    }

    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        super.onRestored(context, oldWidgetIds, newWidgetIds);

        for (int i = 0; i < oldWidgetIds.length; i++) {
            // Remap widget ids
            WidgetUtils.remapWidget(oldWidgetIds[i], newWidgetIds[i]);
        }
    }
}