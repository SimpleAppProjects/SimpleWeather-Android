package com.thewizrd.simpleweather.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.R;

public abstract class WeatherWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "WeatherWidgetProvider";

    // Actions
    public static final String ACTION_SHOWREFRESH = "SimpleWeather.Droid.action.SHOWREFRESH";
    public static final String ACTION_REFRESHWIDGETS = "SimpleWeather.Droid.action.UPDATEWIDGETS";
    public static final String ACTION_SHOWPREVIOUSFORECAST = "SimpleWeather.Droid.action.SHOW_PREVIOUS_FORECAST";
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
                WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                        .setAction(WeatherWidgetService.ACTION_STARTALARM));
            } else if (Intent.ACTION_LOCALE_CHANGED.equals(intent.getAction())) {
                updateWidgets(context, null);
            } else if (ACTION_SHOWREFRESH.equals(intent.getAction())) {
                // Update widgets
                int[] appWidgetIds = intent.getIntArrayExtra(EXTRA_WIDGET_IDS);
                showRefresh(context, appWidgetIds);
            } else if (ACTION_REFRESHWIDGETS.equals(intent.getAction())) {
                // Update widgets
                int[] appWidgetIds = intent.getIntArrayExtra(EXTRA_WIDGET_IDS);
                updateWidgets(context, appWidgetIds);
            } else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
                // Update widgets
                int[] appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);
                updateWidgets(context, appWidgetIds);
            } else {
                Logger.writeLine(Log.INFO, "%s: Unhandled action: %s", TAG, intent.getAction());
                super.onReceive(context, intent);
            }
        }
    }

    protected void showRefresh(Context context, int[] appWidgetIds) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentname = new ComponentName(context.getPackageName(), getClassName());
        if (appWidgetIds == null || appWidgetIds.length == 0)
            appWidgetIds = appWidgetManager.getAppWidgetIds(componentname);

        RemoteViews views = new RemoteViews(context.getPackageName(), getWidgetLayoutId());
        views.setViewVisibility(R.id.refresh_button, View.GONE);
        views.setViewVisibility(R.id.refresh_progress, View.VISIBLE);
        appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, views);
    }

    protected void updateWidgets(Context context, int[] appWidgetIds) {
        showRefresh(context, appWidgetIds);

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
        WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                .setAction(WeatherWidgetService.ACTION_STARTALARM));
    }

    @Override
    public void onDisabled(Context context) {
        // Remove alarms/updates
        WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                .setAction(WeatherWidgetService.ACTION_CANCELALARM));
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
}