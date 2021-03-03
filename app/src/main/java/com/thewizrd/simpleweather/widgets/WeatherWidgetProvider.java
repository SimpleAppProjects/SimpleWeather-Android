package com.thewizrd.simpleweather.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.services.UpdaterUtils;

public abstract class WeatherWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "WeatherWidgetProvider";

    // Actions
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
    public final boolean hasInstances(Context context) {
        return instancesCount(context) > 0;
    }

    public final int instancesCount(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, getClassName()));
        return appWidgetIds.length;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            Logger.writeLine(Log.INFO, "%s: WidgetType: %s; onReceive: %s", TAG, getWidgetType().name(), intent.getAction());
        }
        super.onReceive(context, intent);
    }

    protected void updateWidgets(Context context, final int[] appWidgetIds) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        RemoteViews updateViews = new RemoteViews(context.getPackageName(), getWidgetLayoutId());
        appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, updateViews);

        AsyncTask.run(new Runnable() {
            @Override
            public void run() {
                Context context = App.getInstance().getAppContext();

                if (appWidgetIds != null && appWidgetIds.length > 0) {
                    for (int appWidgetId : appWidgetIds) {
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
        UpdaterUtils.startAlarm(context);
    }

    @Override
    public void onDisabled(Context context) {
        // Remove alarms/updates
        UpdaterUtils.cancelAlarm(context);
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