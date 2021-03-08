package com.thewizrd.simpleweather.widgets;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;

import static com.thewizrd.simpleweather.widgets.WidgetType.Widget2x2;

public class WeatherWidgetProvider2x2 extends WeatherWidgetProvider {
    private static WeatherWidgetProvider2x2 sInstance;

    public static synchronized WeatherWidgetProvider2x2 getInstance() {
        if (sInstance == null)
            sInstance = new WeatherWidgetProvider2x2();

        return sInstance;
    }

    // Overrides
    @Override
    public WidgetType getWidgetType() {
        return Widget2x2;
    }

    @Override
    public int getWidgetLayoutId() {
        return R.layout.app_widget_2x2;
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
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();

        if (Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            // Update clock widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName componentname = new ComponentName(context.getPackageName(), getClassName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentname);

            refreshClock(context, appWidgetIds);
            refreshDate(context, appWidgetIds);
        } else {
            super.onReceive(context, intent);
        }
    }
}
