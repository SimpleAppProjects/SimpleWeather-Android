package com.thewizrd.simpleweather.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.services.UpdaterUtils;

import static com.thewizrd.simpleweather.widgets.WidgetUtils.getCellsForSize;

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

    protected void refreshClock(@NonNull Context context, int[] appWidgetIds) {
        final AppWidgetManager mAppWidgetManager = AppWidgetManager.getInstance(context);
        final WidgetType widgetType = getWidgetType();
        final RemoteViews views;

        if (widgetType == WidgetType.Widget2x2)
            views = new RemoteViews(context.getPackageName(), getWidgetLayoutId());
        else if (widgetType == WidgetType.Widget4x2)
            views = new RemoteViews(context.getPackageName(), getWidgetLayoutId());
        else if (widgetType == WidgetType.Widget4x2Clock)
            views = new RemoteViews(context.getPackageName(), getWidgetLayoutId());
        else if (widgetType == WidgetType.Widget4x2Huawei)
            views = new RemoteViews(context.getPackageName(), getWidgetLayoutId());
        else
            return;

        // Update clock widgets
        boolean useAmPm = !(widgetType == WidgetType.Widget4x2Clock || widgetType == WidgetType.Widget4x2Huawei);
        SpannableString timeStr12hr = new SpannableString(context.getText(useAmPm ? R.string.clock_12_hours_ampm_format : R.string.clock_12_hours_format));
        if (useAmPm) {
            int start12hr = timeStr12hr.length() - 2;
            timeStr12hr.setSpan(new RelativeSizeSpan(0.875f), start12hr, timeStr12hr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        views.setCharSequence(R.id.clock_panel, "setFormat12Hour",
                timeStr12hr);

        views.setCharSequence(R.id.clock_panel, "setFormat24Hour",
                context.getText(R.string.clock_24_hours_format));

        for (int appWidgetId : appWidgetIds) {
            LocationData locationData;
            if (WidgetUtils.isGPS(appWidgetId))
                locationData = Settings.getLastGPSLocData();
            else
                locationData = WidgetUtils.getLocationData(appWidgetId);

            // Widget dimensions
            Bundle newOptions = mAppWidgetManager.getAppWidgetOptions(appWidgetId);
            int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
            int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
            int maxCellHeight = getCellsForSize(maxHeight);
            int maxCellWidth = getCellsForSize(maxWidth);
            int cellHeight = getCellsForSize(minHeight);
            int cellWidth = getCellsForSize(minWidth);
            boolean forceSmallHeight = cellHeight == maxCellHeight;
            boolean isSmallHeight = ((float) maxCellHeight / cellHeight) <= 1.5f;
            boolean isSmallWidth = ((float) maxCellWidth / cellWidth) <= 1.5f;

            if (widgetType == WidgetType.Widget4x2Huawei) {
                views.setTextViewTextSize(R.id.clock_panel, TypedValue.COMPLEX_UNIT_SP, cellWidth <= 3 ? 48 : 60);
            } else if (widgetType == WidgetType.Widget4x2Clock) {
                views.setTextViewTextSize(R.id.clock_panel, TypedValue.COMPLEX_UNIT_SP, isSmallHeight && cellHeight <= 2 ? 60 : 66);
            } else {
                float clockTextSize = context.getResources().getDimensionPixelSize(R.dimen.clock_text_size); // 36sp

                if ((isSmallHeight && cellHeight <= 2) || cellWidth < 4) {
                    clockTextSize *= (8f / 9); // 32sp
                    if (cellWidth < 4 && widgetType == WidgetType.Widget4x2) {
                        clockTextSize *= (7f / 8); // 28sp
                    }
                }

                views.setTextViewTextSize(R.id.clock_panel, TypedValue.COMPLEX_UNIT_PX, clockTextSize);
            }

            if (WidgetUtils.useTimeZone(appWidgetId) && locationData != null) {
                views.setString(R.id.clock_panel, "setTimeZone", locationData.getTzLong());
            } else {
                views.setString(R.id.clock_panel, "setTimeZone", null);
            }

            if (!(!Settings.useFollowGPS() && WidgetUtils.isGPS(appWidgetId))) {
                mAppWidgetManager.partiallyUpdateAppWidget(appWidgetId, views);
            }
        }
    }

    protected void refreshDate(@NonNull Context context, int[] appWidgetIds) {
        final AppWidgetManager mAppWidgetManager = AppWidgetManager.getInstance(context);
        final WidgetType widgetType = getWidgetType();
        final RemoteViews views;

        if (widgetType == WidgetType.Widget2x2)
            views = new RemoteViews(context.getPackageName(), getWidgetLayoutId());
        else if (widgetType == WidgetType.Widget4x2)
            views = new RemoteViews(context.getPackageName(), getWidgetLayoutId());
        else if (widgetType == WidgetType.Widget4x1Google)
            views = new RemoteViews(context.getPackageName(), getWidgetLayoutId());
        else if (widgetType == WidgetType.Widget4x2Clock)
            views = new RemoteViews(context.getPackageName(), getWidgetLayoutId());
        else if (widgetType == WidgetType.Widget4x2Huawei)
            views = new RemoteViews(context.getPackageName(), getWidgetLayoutId());
        else
            return;

        for (int appWidgetId : appWidgetIds) {
            LocationData locationData;
            if (WidgetUtils.isGPS(appWidgetId))
                locationData = Settings.getLastGPSLocData();
            else
                locationData = WidgetUtils.getLocationData(appWidgetId);

            // Widget dimensions
            Bundle newOptions = mAppWidgetManager.getAppWidgetOptions(appWidgetId);
            int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
            int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
            int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
            int maxCellHeight = getCellsForSize(maxHeight);
            int maxCellWidth = getCellsForSize(maxWidth);
            int cellHeight = getCellsForSize(minHeight);
            int cellWidth = getCellsForSize(minWidth);
            boolean forceSmallHeight = cellHeight == maxCellHeight;
            boolean isSmallHeight = ((float) maxCellHeight / cellHeight) <= 1.5f;
            boolean isSmallWidth = ((float) maxCellWidth / cellWidth) <= 1.5f;

            if (widgetType == WidgetType.Widget2x2) {
                float dateTextSize = context.getResources().getDimensionPixelSize(R.dimen.date_text_size); // 16sp

                if ((isSmallHeight && cellHeight <= 2) || cellWidth < 4)
                    dateTextSize *= 0.875f; // 14sp

                views.setTextViewTextSize(R.id.date_panel, TypedValue.COMPLEX_UNIT_PX, dateTextSize);
            } else if (widgetType == WidgetType.Widget4x1Google) {
                float dateTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24f, context.getResources().getDisplayMetrics());

                if ((isSmallHeight && cellHeight <= 2)) {
                    dateTextSize *= (5 / 6f); // 20sp
                }

                views.setTextViewTextSize(R.id.date_panel, TypedValue.COMPLEX_UNIT_PX, dateTextSize);
            }

            String datePattern;
            if ((widgetType == WidgetType.Widget2x2 && cellWidth >= 3) ||
                    (widgetType == WidgetType.Widget4x2Clock && cellWidth >= 4) ||
                    (widgetType == WidgetType.Widget4x2Huawei && cellWidth >= 4)) {
                datePattern = DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_LONG_DATE_FORMAT);
            } else if (widgetType == WidgetType.Widget4x1Google || widgetType == WidgetType.Widget4x2Clock) {
                datePattern = DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_WDAY_ABBR_MONTH_FORMAT);
            } else if (widgetType == WidgetType.Widget4x2) {
                datePattern = DateTimeUtils.getBestPatternForSkeleton(cellWidth > 4 ? DateTimeConstants.SKELETON_ABBR_WDAY_MONTH_FORMAT : DateTimeConstants.SKELETON_SHORT_DATE_FORMAT);
            } else {
                datePattern = DateTimeUtils.getBestPatternForSkeleton(DateTimeConstants.SKELETON_SHORT_DATE_FORMAT);
            }

            views.setCharSequence(R.id.date_panel, "setFormat12Hour", datePattern);
            views.setCharSequence(R.id.date_panel, "setFormat24Hour", datePattern);

            if (WidgetUtils.useTimeZone(appWidgetId) && locationData != null) {
                views.setString(R.id.date_panel, "setTimeZone", locationData.getTzLong());
            } else {
                views.setString(R.id.date_panel, "setTimeZone", null);
            }

            if (!(!Settings.useFollowGPS() && WidgetUtils.isGPS(appWidgetId)))
                mAppWidgetManager.partiallyUpdateAppWidget(appWidgetId, views);
        }
    }
}