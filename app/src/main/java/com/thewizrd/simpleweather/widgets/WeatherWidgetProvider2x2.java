package com.thewizrd.simpleweather.widgets;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;

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
    public boolean hasInstances(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
                new ComponentName(context, getClassName()));
        return (appWidgetIds.length > 0);
    }
}
