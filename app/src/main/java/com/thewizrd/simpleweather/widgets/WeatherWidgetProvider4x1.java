package com.thewizrd.simpleweather.widgets;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;

import static com.thewizrd.simpleweather.widgets.WidgetType.Widget4x1;

public class WeatherWidgetProvider4x1 extends WeatherWidgetProvider {
    private static WeatherWidgetProvider4x1 sInstance;

    public static synchronized WeatherWidgetProvider4x1 getInstance() {
        if (sInstance == null)
            sInstance = new WeatherWidgetProvider4x1();

        return sInstance;
    }

    // Overrides
    @Override
    public WidgetType getWidgetType() {
        return Widget4x1;
    }

    @Override
    public int getWidgetLayoutId() {
        return R.layout.app_widget_4x1;
    }

    @Override
    protected String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public ComponentName getComponentName() {
        return new ComponentName(App.getInstance().getAppContext(), getClassName());
    }

    @Override
    public boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, getClassName()));
        return (appWidgetIds.length > 0);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();

        if (intent != null) {
            if (ACTION_SHOWNEXTFORECAST.equals(action)) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                RemoteViews views = new RemoteViews(context.getPackageName(), getWidgetLayoutId());
                views.showNext(R.id.forecast_layout);
                int appWidgetId = intent.getIntExtra(WeatherWidgetProvider.EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                appWidgetManager.updateAppWidget(appWidgetId, views);
            } else {
                super.onReceive(context, intent);
            }
        }
    }
}
