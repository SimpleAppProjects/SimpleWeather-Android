package com.thewizrd.simpleweather.widgets;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;

import static com.thewizrd.simpleweather.widgets.WidgetType.Widget1x1;

public class WeatherWidgetProvider1x1 extends WeatherWidgetProvider {
    private static WeatherWidgetProvider1x1 sInstance;

    public static synchronized WeatherWidgetProvider1x1 getInstance() {
        if (sInstance == null)
            sInstance = new WeatherWidgetProvider1x1();

        return sInstance;
    }

    // Overrides
    @Override
    public WidgetType getWidgetType() {
        return Widget1x1;
    }

    @Override
    public int getWidgetLayoutId() {
        return R.layout.app_widget_1x1;
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
}
