package com.thewizrd.simpleweather.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.AlarmClock;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.SafeJobIntentService;

import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.FutureTarget;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.controls.BaseForecastItemViewModel;
import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.controls.ImageDataViewModel;
import com.thewizrd.shared_resources.controls.WeatherDetailsType;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.ContextUtils;
import com.thewizrd.shared_resources.icons.WeatherIcons;
import com.thewizrd.shared_resources.icons.WeatherIconsManager;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.tasks.CallableEx;
import com.thewizrd.shared_resources.tasks.TaskUtils;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.ImageUtils;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.TransparentOverlay;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.Forecast;
import com.thewizrd.shared_resources.weatherdata.Forecasts;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherRequest;
import com.thewizrd.simpleweather.GlideApp;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.main.MainActivity;
import com.thewizrd.simpleweather.utils.ArrayUtils;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static com.thewizrd.simpleweather.widgets.WidgetUtils.getBackgroundColor;
import static com.thewizrd.simpleweather.widgets.WidgetUtils.getCellsForSize;
import static com.thewizrd.simpleweather.widgets.WidgetUtils.getForecastLength;
import static com.thewizrd.simpleweather.widgets.WidgetUtils.getPanelTextColor;
import static com.thewizrd.simpleweather.widgets.WidgetUtils.getTextColor;
import static com.thewizrd.simpleweather.widgets.WidgetUtils.getWidgetTypeFromID;
import static com.thewizrd.simpleweather.widgets.WidgetUtils.isBackgroundOptionalWidget;
import static com.thewizrd.simpleweather.widgets.WidgetUtils.isClockWidget;
import static com.thewizrd.simpleweather.widgets.WidgetUtils.isDateWidget;
import static com.thewizrd.simpleweather.widgets.WidgetUtils.isForecastWidget;

public class WeatherWidgetService extends SafeJobIntentService {
    private static final String TAG = "WeatherWidgetService";

    public static final String ACTION_REFRESHWIDGET = "SimpleWeather.Droid.action.REFRESH_WIDGET";
    public static final String ACTION_RESIZEWIDGET = "SimpleWeather.Droid.action.RESIZE_WIDGET";
    public static final String ACTION_UPDATECLOCK = "SimpleWeather.Droid.action.UPDATE_CLOCK";
    public static final String ACTION_UPDATEDATE = "SimpleWeather.Droid.action.UPDATE_DATE";

    public static final String ACTION_STARTCLOCK = "SimpleWeather.Droid.action.START_CLOCKALARM";
    public static final String ACTION_CANCELCLOCK = "SimpleWeather.Droid.action.CANCEL_CLOCKALARM";

    public static final String ACTION_RESETGPSWIDGETS = "SimpleWeather.Droid.action.RESET_GPSWIDGETS";
    public static final String ACTION_REFRESHGPSWIDGETS = "SimpleWeather.Droid.action.REFRESH_GPSWIDGETS";
    public static final String ACTION_REFRESHWIDGETS = "SimpleWeather.Droid.action.REFRESH_WIDGETS";

    public static final String EXTRA_LOCATIONNAME = "SimpleWeather.Droid.extra.LOCATION_NAME";
    public static final String EXTRA_LOCATIONQUERY = "SimpleWeather.Droid.extra.LOCATION_QUERY";

    private static final int JOB_ID = 1000;

    private Context mContext;
    private AppWidgetManager mAppWidgetManager;
    private static BroadcastReceiver mTickReceiver;

    // Weather Widget Providers
    private final WeatherWidgetProvider1x1 mAppWidget1x1 =
            WeatherWidgetProvider1x1.getInstance();
    private final WeatherWidgetProvider2x2 mAppWidget2x2 =
            WeatherWidgetProvider2x2.getInstance();
    private final WeatherWidgetProvider4x1 mAppWidget4x1 =
            WeatherWidgetProvider4x1.getInstance();
    private final WeatherWidgetProvider4x2 mAppWidget4x2 =
            WeatherWidgetProvider4x2.getInstance();
    private final WeatherWidgetProvider4x1Google mAppWidget4x1Google =
            WeatherWidgetProvider4x1Google.getInstance();
    private final WeatherWidgetProvider4x1Notification mAppWidget4x1Notif =
            WeatherWidgetProvider4x1Notification.getInstance();
    private final WeatherWidgetProvider4x2Clock mAppWidget4x2Clock =
            WeatherWidgetProvider4x2Clock.getInstance();
    private final WeatherWidgetProvider4x2Huawei mAppWidget4x2Huawei =
            WeatherWidgetProvider4x2Huawei.getInstance();

    private boolean isNightMode = false;
    private float maxBitmapSize;

    private final CancellationTokenSource cts = new CancellationTokenSource();

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, WeatherWidgetService.class,
                JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();
        mAppWidgetManager = AppWidgetManager.getInstance(mContext);

        /*
         * The total Bitmap memory used by the RemoteViews object cannot exceed
         * that required to fill the screen 1.5 times,
         * ie. (screen width x screen height x 4 x 1.5) bytes.
         */
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        maxBitmapSize = metrics.heightPixels * metrics.widthPixels * 4 * 1.5f;
    }

    @Override
    public void onDestroy() {
        Logger.writeLine(Log.INFO, "%s: destroying service and cancelling tasks...", TAG);

        try {
            cts.cancel();
        } catch (Exception ex) {
            Logger.writeLine(Log.ERROR, ex, "%s: Error cancelling task...", TAG);
        }

        super.onDestroy();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        final int currentNightMode = mContext.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        isNightMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;

        try {
            if (ACTION_REFRESHWIDGET.equals(intent.getAction())) {
                final int[] appWidgetIds = intent.getIntArrayExtra(WeatherWidgetProvider.EXTRA_WIDGET_IDS);
                WidgetType widgetType = WidgetType.valueOf(intent.getIntExtra(WeatherWidgetProvider.EXTRA_WIDGET_TYPE, -1));

                switch (widgetType) {
                    case Widget1x1:
                        refreshWidget(mAppWidget1x1, appWidgetIds);
                        break;
                    case Widget2x2:
                        refreshWidget(mAppWidget2x2, appWidgetIds);
                        break;
                    case Widget4x1:
                        refreshWidget(mAppWidget4x1, appWidgetIds);
                        break;
                    case Widget4x2:
                        refreshWidget(mAppWidget4x2, appWidgetIds);
                        break;
                    case Widget4x1Google:
                        refreshWidget(mAppWidget4x1Google, appWidgetIds);
                        break;
                    case Widget4x1Notification:
                        refreshWidget(mAppWidget4x1Notif, appWidgetIds);
                        break;
                    case Widget4x2Clock:
                        refreshWidget(mAppWidget4x2Clock, appWidgetIds);
                        break;
                    case Widget4x2Huawei:
                        refreshWidget(mAppWidget4x2Huawei, appWidgetIds);
                        break;
                    // We don't know the widget type to update,
                    // so just update all
                    case Unknown:
                    default:
                        refreshAllWidgets();
                        break;
                }
            } else if (ACTION_RESIZEWIDGET.equals(intent.getAction())) {
                final int appWidgetId = intent.getIntExtra(WeatherWidgetProvider.EXTRA_WIDGET_ID, -1);
                WidgetType widgetType = WidgetType.valueOf(intent.getIntExtra(WeatherWidgetProvider.EXTRA_WIDGET_TYPE, -1));
                final Bundle newOptions = intent.getBundleExtra(WeatherWidgetProvider.EXTRA_WIDGET_OPTIONS);

                switch (widgetType) {
                    case Widget1x1:
                        resizeWidget(mAppWidget1x1, appWidgetId, newOptions);
                        break;
                    case Widget2x2:
                        resizeWidget(mAppWidget2x2, appWidgetId, newOptions);
                        break;
                    case Widget4x1:
                        resizeWidget(mAppWidget4x1, appWidgetId, newOptions);
                        break;
                    case Widget4x2:
                        resizeWidget(mAppWidget4x2, appWidgetId, newOptions);
                        break;
                    case Widget4x1Google:
                        resizeWidget(mAppWidget4x1Google, appWidgetId, newOptions);
                        break;
                    case Widget4x1Notification:
                        resizeWidget(mAppWidget4x1Notif, appWidgetId, newOptions);
                        break;
                    case Widget4x2Clock:
                        resizeWidget(mAppWidget4x2Clock, appWidgetId, newOptions);
                        break;
                    case Widget4x2Huawei:
                        resizeWidget(mAppWidget4x2Huawei, appWidgetId, newOptions);
                        break;
                }
            } else if (ACTION_STARTCLOCK.equals(intent.getAction())) {
                // Schedule clock updates
                startTickReceiver(mContext);
            } else if (ACTION_CANCELCLOCK.equals(intent.getAction())) {
                if (!clockWidgetsExist(mContext)) {
                    // Cancel clock alarm
                    cancelClockAlarm(mContext);
                }
            } else if (ACTION_UPDATECLOCK.equals(intent.getAction())) {
                // Update clock widget instances
                int[] appWidgetIds = intent.getIntArrayExtra(WeatherWidgetProvider.EXTRA_WIDGET_IDS);
                refreshClock(appWidgetIds);
            } else if (ACTION_UPDATEDATE.equals(intent.getAction())) {
                // Update clock widget instances
                int[] appWidgetIds = intent.getIntArrayExtra(WeatherWidgetProvider.EXTRA_WIDGET_IDS);
                refreshDate(appWidgetIds);
            } else if (ACTION_RESETGPSWIDGETS.equals(intent.getAction())) {
                // GPS feature disabled; reset widget
                resetGPSWidgets();
            } else if (ACTION_REFRESHGPSWIDGETS.equals(intent.getAction())) {
                refreshWidgets(Constants.KEY_GPS);
            } else if (ACTION_REFRESHWIDGETS.equals(intent.getAction())) {
                refreshWidgets(intent.getStringExtra(EXTRA_LOCATIONQUERY));
            } else if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
                // no-op
            }

            Logger.writeLine(Log.INFO, "%s: Intent Action = %s", TAG, intent.getAction());
        } catch (Exception ex) {
            Logger.writeLine(Log.ERROR, ex, "%s: exception occurred...", TAG);
        }
    }

    private static PendingIntent getClockRefreshIntent(Context context) {
        Intent intent = new Intent(context, WeatherWidgetBroadcastReceiver.class)
                .setAction(ACTION_UPDATECLOCK);

        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static void startTickReceiver(Context context) {
        stopTickReceiver(context);

        mTickReceiver = new TickReceiver();
        context.registerReceiver(mTickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));

        Logger.writeLine(Log.INFO, "%s: Started tick receiver", TAG);
    }

    private static void stopTickReceiver(Context context) {
        if (mTickReceiver != null) {
            context.unregisterReceiver(mTickReceiver);
            mTickReceiver = null;

            Logger.writeLine(Log.INFO, "%s: Unregistered tick receiver", TAG);
        }
    }

    private static void cancelClockAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(getClockRefreshIntent(context));

        Logger.writeLine(Log.INFO, "%s: Canceled clock alarm", TAG);
    }

    static class TickReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && Intent.ACTION_TIME_TICK.equals(intent.getAction())) {
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                PendingIntent pendingIntent = getClockRefreshIntent(context);
                am.cancel(pendingIntent);

                long nowMillis = System.currentTimeMillis();
                long dueTime = nowMillis - (nowMillis % 60000) + 60000;
                am.setRepeating(AlarmManager.RTC, dueTime, 60000, pendingIntent);

                // Request an update
                context.sendBroadcast(new Intent(context, WeatherWidgetBroadcastReceiver.class)
                        .setAction(ACTION_UPDATECLOCK));

                stopTickReceiver(context);

                Logger.writeLine(Log.INFO, "%s: Receieved tick in receiver", TAG);
            }
        }
    }

    public static boolean widgetsExist(Context context) {
        return WeatherWidgetProvider1x1.getInstance().hasInstances(context)
                || WeatherWidgetProvider2x2.getInstance().hasInstances(context)
                || WeatherWidgetProvider4x1.getInstance().hasInstances(context)
                || WeatherWidgetProvider4x2.getInstance().hasInstances(context)
                || WeatherWidgetProvider4x1Google.getInstance().hasInstances(context)
                || WeatherWidgetProvider4x1Notification.getInstance().hasInstances(context)
                || WeatherWidgetProvider4x2Clock.getInstance().hasInstances(context)
                || WeatherWidgetProvider4x2Huawei.getInstance().hasInstances(context);
    }

    public static boolean clockWidgetsExist(Context context) {
        return WeatherWidgetProvider2x2.getInstance().hasInstances(context)
                || WeatherWidgetProvider4x2.getInstance().hasInstances(context)
                || WeatherWidgetProvider4x2Clock.getInstance().hasInstances(context)
                || WeatherWidgetProvider4x2Huawei.getInstance().hasInstances(context);
    }

    private void resizeWidget(WeatherWidgetProvider provider, int appWidgetId, Bundle newOptions) throws InterruptedException {
        if (Settings.isWeatherLoaded()) rebuildWidget(provider, appWidgetId, newOptions);
    }

    private void refreshWidget(final WeatherWidgetProvider provider, int[] appWidgetIds) {
        if (appWidgetIds == null || appWidgetIds.length == 0)
            appWidgetIds = mAppWidgetManager.getAppWidgetIds(provider.getComponentName());

        if (Settings.isWeatherLoaded()) {
            List<Task<Void>> tasks = new ArrayList<>(appWidgetIds.length);
            int count = 0;
            for (final int id : appWidgetIds) {
                Logger.writeLine(Log.DEBUG, "%s: refreshWidget: provider: %s; index: %d", TAG, provider.getWidgetType().name(), count);
                Task<Void> task = AsyncTask.create(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        refreshWidget(provider, id);
                        return null;
                    }
                });

                count++;
                tasks.add(task);
            }

            try {
                Tasks.await(Tasks.whenAll(tasks));
            } catch (ExecutionException | InterruptedException e) {
                Logger.writeLine(Log.ERROR, e);
            }
        }

        if (isClockWidget(provider.getWidgetType())) {
            refreshClock(appWidgetIds);
        }
        if (isDateWidget(provider.getWidgetType())) {
            refreshDate(appWidgetIds);
        }
    }

    private void refreshAllWidgets() {
        if (Settings.isWeatherLoaded()) {
            // Build the widget update for available providers
            // Add widget providers here
            List<Task<Void>> tasks = new ArrayList<>(8);

            if (mAppWidget1x1.hasInstances(mContext)) {
                Task<Void> task = AsyncTask.create(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Logger.writeLine(Log.DEBUG, "%s: refreshAllWidgets: started 1x1", TAG);
                        int[] appWidgetIds = mAppWidgetManager.getAppWidgetIds(mAppWidget1x1.getComponentName());

                        for (int id : appWidgetIds) {
                            refreshWidget(mAppWidget1x1, id);
                        }
                        return null;
                    }
                });

                tasks.add(task);
            }

            if (mAppWidget2x2.hasInstances(mContext)) {
                Task<Void> task = AsyncTask.create(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Logger.writeLine(Log.DEBUG, "%s: refreshAllWidgets: started 2x2", TAG);
                        int[] appWidgetIds = mAppWidgetManager.getAppWidgetIds(mAppWidget2x2.getComponentName());

                        for (int id : appWidgetIds) {
                            refreshWidget(mAppWidget2x2, id);
                        }

                        refreshClock(appWidgetIds);
                        refreshDate(appWidgetIds);
                        return null;
                    }
                });

                tasks.add(task);
            }

            if (mAppWidget4x1.hasInstances(mContext)) {
                Task<Void> task = AsyncTask.create(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Logger.writeLine(Log.DEBUG, "%s: refreshAllWidgets: started 4x1", TAG);
                        int[] appWidgetIds = mAppWidgetManager.getAppWidgetIds(mAppWidget4x1.getComponentName());

                        for (int id : appWidgetIds) {
                            refreshWidget(mAppWidget4x1, id);
                        }
                        return null;
                    }
                });

                tasks.add(task);
            }

            if (mAppWidget4x2.hasInstances(mContext)) {
                Task<Void> task = AsyncTask.create(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Logger.writeLine(Log.DEBUG, "%s: refreshAllWidgets: started 4x2", TAG);
                        int[] appWidgetIds = mAppWidgetManager.getAppWidgetIds(mAppWidget4x2.getComponentName());

                        for (int id : appWidgetIds) {
                            refreshWidget(mAppWidget4x2, id);
                        }

                        refreshClock(appWidgetIds);
                        refreshDate(appWidgetIds);
                        return null;
                    }
                });

                tasks.add(task);
            }

            if (mAppWidget4x1Google.hasInstances(mContext)) {
                Task<Void> task = AsyncTask.create(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Logger.writeLine(Log.DEBUG, "%s: refreshAllWidgets: started 4x1Google", TAG);
                        int[] appWidgetIds = mAppWidgetManager.getAppWidgetIds(mAppWidget4x1Google.getComponentName());

                        for (int id : appWidgetIds) {
                            refreshWidget(mAppWidget4x1Google, id);
                        }

                        refreshDate(appWidgetIds);
                        return null;
                    }
                });

                tasks.add(task);
            }

            if (mAppWidget4x1Notif.hasInstances(mContext)) {
                Task<Void> task = AsyncTask.create(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Logger.writeLine(Log.DEBUG, "%s: refreshAllWidgets: started 4x1Notif", TAG);
                        int[] appWidgetIds = mAppWidgetManager.getAppWidgetIds(mAppWidget4x1Notif.getComponentName());

                        for (int id : appWidgetIds) {
                            refreshWidget(mAppWidget4x1Notif, id);
                        }
                        return null;
                    }
                });

                tasks.add(task);
            }

            if (mAppWidget4x2Clock.hasInstances(mContext)) {
                Task<Void> task = AsyncTask.create(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Logger.writeLine(Log.DEBUG, "%s: refreshAllWidgets: started 4x2Clock", TAG);
                        int[] appWidgetIds = mAppWidgetManager.getAppWidgetIds(mAppWidget4x2Clock.getComponentName());

                        for (int id : appWidgetIds) {
                            refreshWidget(mAppWidget4x2Clock, id);
                        }

                        refreshClock(appWidgetIds);
                        refreshDate(appWidgetIds);
                        return null;
                    }
                });

                tasks.add(task);
            }

            if (mAppWidget4x2Huawei.hasInstances(mContext)) {
                Task<Void> task = AsyncTask.create(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Logger.writeLine(Log.DEBUG, "%s: refreshAllWidgets: started 4x2Huawei", TAG);
                        int[] appWidgetIds = mAppWidgetManager.getAppWidgetIds(mAppWidget4x2Huawei.getComponentName());

                        for (int id : appWidgetIds) {
                            refreshWidget(mAppWidget4x2Huawei, id);
                        }

                        refreshClock(appWidgetIds);
                        refreshDate(appWidgetIds);
                        return null;
                    }
                });

                tasks.add(task);
            }

            try {
                Tasks.await(Tasks.whenAll(tasks));
            } catch (ExecutionException | InterruptedException e) {
                Logger.writeLine(Log.ERROR, e);
            }
        }
    }

    private void refreshWidget(final WeatherWidgetProvider provider, int appWidgetId) throws InterruptedException {
        LocationData locData;

        if (WidgetUtils.isGPS(appWidgetId)) {
            if (!Settings.useFollowGPS()) {
                resetGPSWidgets(Collections.singletonList(appWidgetId));
                return;
            } else {
                locData = Settings.getLastGPSLocData();
            }
        } else {
            locData = WidgetUtils.getLocationData(appWidgetId);
        }

        TaskUtils.throwIfCancellationRequested(cts.getToken());

        Weather weather;

        if (locData != null) {
            TaskUtils.throwIfCancellationRequested(cts.getToken());

            WeatherDataLoader wloader = new WeatherDataLoader(locData);
            try {
                WeatherRequest.Builder request =
                        new WeatherRequest.Builder()
                                .forceRefresh(false);

                if (WidgetUtils.isForecastWidget(provider.getWidgetType())) {
                    request.loadForecasts();
                }

                weather = Tasks.await(wloader.loadWeatherData(request.build()));
            } catch (Exception e) {
                weather = null;
            }

            if (weather != null) {
                // Save weather data
                WidgetUtils.saveWeatherData(appWidgetId, weather);

                WeatherNowViewModel viewModel = new WeatherNowViewModel(weather);

                // Build the widget update for provider
                RemoteViews views = buildUpdate(mContext, provider, appWidgetId, locData, viewModel);
                if (isForecastWidget(provider.getWidgetType())) {
                    buildForecast(views, provider, appWidgetId);
                }
                // Push update for this widget to the home screen
                mAppWidgetManager.updateAppWidget(appWidgetId, views);
            } else {
                Logger.writeLine(Log.DEBUG, "%s: provider: %s; widgetId: %d; Unable to find weather data", TAG, provider.getClassName(), appWidgetId);
            }
        } else {
            Logger.writeLine(Log.DEBUG, "%s: provider: %s; widgetId: %d; Unable to find location data", TAG, provider.getClassName(), appWidgetId);
        }
    }

    private void resetGPSWidgets() {
        final List<Integer> appWidgetIds = WidgetUtils.getWidgetIds(Constants.KEY_GPS);
        resetGPSWidgets(appWidgetIds);
    }

    private void resetGPSWidgets(final List<Integer> appWidgetIds) {
        List<Task<Void>> tasks = new ArrayList<>(appWidgetIds.size());

        for (final int appWidgetId : appWidgetIds) {
            Task<Void> task = AsyncTask.create(new Callable<Void>() {
                @Override
                public Void call() {
                    RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.app_widget_configure_layout);

                    Intent configureIntent = new Intent(mContext, WeatherWidgetConfigActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    configureIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

                    PendingIntent clickPendingIntent =
                            PendingIntent.getActivity(mContext, appWidgetId, configureIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    views.setOnClickPendingIntent(R.id.widget, clickPendingIntent);

                    mAppWidgetManager.updateAppWidget(appWidgetId, views);
                    return null;
                }
            });

            tasks.add(task);
        }

        try {
            Tasks.await(Tasks.whenAll(tasks));
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }
    }

    private void refreshWidgets(String location_query) {
        final List<Integer> appWidgetIds = WidgetUtils.getWidgetIds(location_query);

        final int[] ids1x1 = mAppWidgetManager.getAppWidgetIds(mAppWidget1x1.getComponentName());
        final int[] ids2x2 = mAppWidgetManager.getAppWidgetIds(mAppWidget2x2.getComponentName());
        final int[] ids4x1 = mAppWidgetManager.getAppWidgetIds(mAppWidget4x1.getComponentName());
        final int[] ids4x2 = mAppWidgetManager.getAppWidgetIds(mAppWidget4x2.getComponentName());
        final int[] ids4x1G = mAppWidgetManager.getAppWidgetIds(mAppWidget4x1Google.getComponentName());
        final int[] ids4x1N = mAppWidgetManager.getAppWidgetIds(mAppWidget4x1Notif.getComponentName());
        final int[] ids4x2C = mAppWidgetManager.getAppWidgetIds(mAppWidget4x2Clock.getComponentName());
        final int[] ids4x2H = mAppWidgetManager.getAppWidgetIds(mAppWidget4x2Huawei.getComponentName());
        final LocationData locationData;
        if (Constants.KEY_GPS.equals(location_query)) {
            locationData = Settings.getLastGPSLocData();
        } else {
            locationData = Settings.getLocation(location_query);
        }

        if (locationData == null || !locationData.isValid())
            return;

        final WeatherDataLoader wLoader = new WeatherDataLoader(locationData);
        final Weather weather = AsyncTask.await(new Callable<Weather>() {
            @Override
            public Weather call() {
                try {
                    return Tasks.await(wLoader.loadWeatherData(new WeatherRequest.Builder()
                            .forceRefresh(false)
                            .loadForecasts()
                            .build()));
                } catch (ExecutionException | InterruptedException e) {
                    return null;
                }
            }
        });

        List<Task<Void>> tasks = new ArrayList<>(appWidgetIds.size());

        for (final int appWidgetId : appWidgetIds) {
            Task<Void> task = AsyncTask.create(new Callable<Void>() {
                @Override
                public Void call() {
                    if (weather != null) {
                        // Save weather data
                        WidgetUtils.saveWeatherData(appWidgetId, weather);

                        WeatherNowViewModel viewModel = new WeatherNowViewModel(weather);

                        if (ArrayUtils.contains(ids1x1, appWidgetId)) {
                            // Build the widget update for provider
                            RemoteViews views = buildUpdate(mContext, mAppWidget1x1, appWidgetId, locationData, viewModel);
                            // Push update for this widget to the home screen
                            mAppWidgetManager.updateAppWidget(appWidgetId, views);
                        } else if (ArrayUtils.contains(ids2x2, appWidgetId)) {
                            // Build the widget update for provider
                            RemoteViews views = buildUpdate(mContext, mAppWidget2x2, appWidgetId, locationData, viewModel);
                            // Push update for this widget to the home screen
                            mAppWidgetManager.updateAppWidget(appWidgetId, views);
                        } else if (ArrayUtils.contains(ids4x1, appWidgetId)) {
                            // Build the widget update for provider
                            RemoteViews views = buildUpdate(mContext, mAppWidget4x1, appWidgetId, locationData, viewModel);
                            buildForecast(views, mAppWidget4x1, appWidgetId);
                            // Push update for this widget to the home screen
                            mAppWidgetManager.updateAppWidget(appWidgetId, views);
                        } else if (ArrayUtils.contains(ids4x2, appWidgetId)) {
                            // Build the widget update for provider
                            RemoteViews views = buildUpdate(mContext, mAppWidget4x2, appWidgetId, locationData, viewModel);
                            buildForecast(views, mAppWidget4x2, appWidgetId);
                            // Push update for this widget to the home screen
                            mAppWidgetManager.updateAppWidget(appWidgetId, views);
                        } else if (ArrayUtils.contains(ids4x1G, appWidgetId)) {
                            // Build the widget update for provider
                            RemoteViews views = buildUpdate(mContext, mAppWidget4x1Google, appWidgetId, locationData, viewModel);
                            // Push update for this widget to the home screen
                            mAppWidgetManager.updateAppWidget(appWidgetId, views);
                        } else if (ArrayUtils.contains(ids4x1N, appWidgetId)) {
                            // Build the widget update for provider
                            RemoteViews views = buildUpdate(mContext, mAppWidget4x1Notif, appWidgetId, locationData, viewModel);
                            // Push update for this widget to the home screen
                            mAppWidgetManager.updateAppWidget(appWidgetId, views);
                        } else if (ArrayUtils.contains(ids4x2C, appWidgetId)) {
                            // Build the widget update for provider
                            RemoteViews views = buildUpdate(mContext, mAppWidget4x2Clock, appWidgetId, locationData, viewModel);
                            // Push update for this widget to the home screen
                            mAppWidgetManager.updateAppWidget(appWidgetId, views);
                        } else if (ArrayUtils.contains(ids4x2H, appWidgetId)) {
                            // Build the widget update for provider
                            RemoteViews views = buildUpdate(mContext, mAppWidget4x2Huawei, appWidgetId, locationData, viewModel);
                            // Push update for this widget to the home screen
                            mAppWidgetManager.updateAppWidget(appWidgetId, views);
                        } else {
                            Logger.writeLine(Log.DEBUG, "%s: refreshWidgets: unable to find widget provider", TAG);
                        }
                    }
                    return null;
                }
            });

            tasks.add(task);
        }

        try {
            Tasks.await(Tasks.whenAll(tasks));
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }
    }

    private void refreshClock(int[] appWidgetIds) {
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            int[] appWidget2x2Ids = mAppWidgetManager.getAppWidgetIds(mAppWidget2x2.getComponentName());
            int[] appWidget4x2Ids = mAppWidgetManager.getAppWidgetIds(mAppWidget4x2.getComponentName());
            int[] appWidget4x2CIds = mAppWidgetManager.getAppWidgetIds(mAppWidget4x2Clock.getComponentName());
            int[] appWidget4x2HIds = mAppWidgetManager.getAppWidgetIds(mAppWidget4x2Huawei.getComponentName());
            appWidgetIds = ArrayUtils.concat(appWidget2x2Ids, appWidget4x2Ids, appWidget4x2CIds, appWidget4x2HIds);
        }

        List<Task<Void>> tasks = new ArrayList<>(appWidgetIds.length);

        for (final int appWidgetId : appWidgetIds) {
            Task<Void> task = AsyncTask.create(new Callable<Void>() {
                @Override
                public Void call() {
                    WidgetType widgetType = getWidgetTypeFromID(appWidgetId);
                    LocationData locationData;
                    if (WidgetUtils.isGPS(appWidgetId))
                        locationData = Settings.getLastGPSLocData();
                    else
                        locationData = WidgetUtils.getLocationData(appWidgetId);

                    RemoteViews views;

                    if (widgetType == WidgetType.Widget2x2)
                        views = new RemoteViews(mContext.getPackageName(), mAppWidget2x2.getWidgetLayoutId());
                    else if (widgetType == WidgetType.Widget4x2)
                        views = new RemoteViews(mContext.getPackageName(), mAppWidget4x2.getWidgetLayoutId());
                    else if (widgetType == WidgetType.Widget4x2Clock)
                        views = new RemoteViews(mContext.getPackageName(), mAppWidget4x2Clock.getWidgetLayoutId());
                    else if (widgetType == WidgetType.Widget4x2Huawei)
                        views = new RemoteViews(mContext.getPackageName(), mAppWidget4x2Huawei.getWidgetLayoutId());
                    else
                        return null;

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
                        float clockTextSize = mContext.getResources().getDimensionPixelSize(R.dimen.clock_text_size); // 36sp

                        if ((isSmallHeight && cellHeight <= 2) || cellWidth < 4) {
                            clockTextSize *= (8f / 9); // 32sp
                            if (cellWidth < 4 && widgetType == WidgetType.Widget4x2) {
                                clockTextSize *= (7f / 8); // 28sp
                            }
                        }

                        views.setTextViewTextSize(R.id.clock_panel, TypedValue.COMPLEX_UNIT_PX, clockTextSize);
                    }

                    // Update clock widgets
                    boolean useAmPm = !(widgetType == WidgetType.Widget4x2Clock || widgetType == WidgetType.Widget4x2Huawei);
                    SpannableString timeStr12hr = new SpannableString(mContext.getText(useAmPm ? R.string.clock_12_hours_ampm_format : R.string.clock_12_hours_format));
                    if (useAmPm) {
                        int start12hr = timeStr12hr.length() - 2;
                        timeStr12hr.setSpan(new RelativeSizeSpan(0.875f), start12hr, timeStr12hr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }

                    views.setCharSequence(R.id.clock_panel, "setFormat12Hour",
                            timeStr12hr);

                    views.setCharSequence(R.id.clock_panel, "setFormat24Hour",
                            mContext.getText(R.string.clock_24_hours_format));

                    if (WidgetUtils.useTimeZone(appWidgetId) && locationData != null) {
                        views.setString(R.id.clock_panel, "setTimeZone", locationData.getTzLong());
                    } else {
                        views.setString(R.id.clock_panel, "setTimeZone", null);
                    }

                    if (!(!Settings.useFollowGPS() && WidgetUtils.isGPS(appWidgetId))) {
                        mAppWidgetManager.partiallyUpdateAppWidget(appWidgetId, views);
                    }
                    return null;
                }
            });

            tasks.add(task);
        }

        try {
            Tasks.await(Tasks.whenAll(tasks));
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }

        Logger.writeLine(Log.INFO, "%s: Refreshed clock", TAG);
    }

    private void refreshDate(int[] appWidgetIds) {
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            int[] appWidget2x2Ids = mAppWidgetManager.getAppWidgetIds(mAppWidget2x2.getComponentName());
            int[] appWidget4x2Ids = mAppWidgetManager.getAppWidgetIds(mAppWidget4x2.getComponentName());
            int[] appWidget4x1GIds = mAppWidgetManager.getAppWidgetIds(mAppWidget4x1Google.getComponentName());
            int[] appWidget4x2CIds = mAppWidgetManager.getAppWidgetIds(mAppWidget4x2Clock.getComponentName());
            int[] appWidget4x2HIds = mAppWidgetManager.getAppWidgetIds(mAppWidget4x2Huawei.getComponentName());
            appWidgetIds = ArrayUtils.concat(appWidget2x2Ids, appWidget4x2Ids, appWidget4x1GIds, appWidget4x2CIds, appWidget4x2HIds);
        }

        List<Task<Void>> tasks = new ArrayList<>(appWidgetIds.length);

        for (final int appWidgetId : appWidgetIds) {
            Task<Void> task = AsyncTask.create(new Callable<Void>() {
                @Override
                public Void call() {
                    // Update clock widgets
                    WidgetType widgetType = getWidgetTypeFromID(appWidgetId);
                    LocationData locationData;
                    if (WidgetUtils.isGPS(appWidgetId))
                        locationData = Settings.getLastGPSLocData();
                    else
                        locationData = WidgetUtils.getLocationData(appWidgetId);

                    RemoteViews views;

                    if (widgetType == WidgetType.Widget2x2)
                        views = new RemoteViews(mContext.getPackageName(), mAppWidget2x2.getWidgetLayoutId());
                    else if (widgetType == WidgetType.Widget4x2)
                        views = new RemoteViews(mContext.getPackageName(), mAppWidget4x2.getWidgetLayoutId());
                    else if (widgetType == WidgetType.Widget4x1Google)
                        views = new RemoteViews(mContext.getPackageName(), mAppWidget4x1Google.getWidgetLayoutId());
                    else if (widgetType == WidgetType.Widget4x2Clock)
                        views = new RemoteViews(mContext.getPackageName(), mAppWidget4x2Clock.getWidgetLayoutId());
                    else if (widgetType == WidgetType.Widget4x2Huawei)
                        views = new RemoteViews(mContext.getPackageName(), mAppWidget4x2Huawei.getWidgetLayoutId());
                    else
                        return null;

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
                        float dateTextSize = mContext.getResources().getDimensionPixelSize(R.dimen.date_text_size); // 16sp

                        if ((isSmallHeight && cellHeight <= 2) || cellWidth < 4)
                            dateTextSize *= 0.875f; // 14sp

                        views.setTextViewTextSize(R.id.date_panel, TypedValue.COMPLEX_UNIT_PX, dateTextSize);
                    } else if (widgetType == WidgetType.Widget4x1Google) {
                        float dateTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24f, mContext.getResources().getDisplayMetrics());

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
                    return null;
                }
            });

            tasks.add(task);
        }

        try {
            Tasks.await(Tasks.whenAll(tasks));
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }

        Logger.writeLine(Log.INFO, "%s: Refreshed date", TAG);
    }

    private static PendingIntent getCalendarAppIntent(Context context) {
        ComponentName componentName = WidgetUtils.getCalendarAppComponent(context);
        if (componentName != null) {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(componentName.getPackageName());
            return PendingIntent.getActivity(context, 0, launchIntent, 0);
        } else {
            Intent onClickIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR);
            return PendingIntent.getActivity(context, 0, onClickIntent, 0);
        }
    }

    private static PendingIntent getClockAppIntent(Context context) {
        ComponentName componentName = WidgetUtils.getClockAppComponent(context);
        if (componentName != null) {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(componentName.getPackageName());
            return PendingIntent.getActivity(context, 0, launchIntent, 0);
        } else {
            Intent onClickIntent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
            return PendingIntent.getActivity(context, 0, onClickIntent, 0);
        }
    }

    private RemoteViews buildUpdate(Context context, final WeatherWidgetProvider provider, final int appWidgetId, LocationData location, final WeatherNowViewModel weather) {
        // Build an update that holds the updated widget contents
        final RemoteViews updateViews = new RemoteViews(context.getPackageName(), provider.getWidgetLayoutId());

        Bundle newOptions = mAppWidgetManager.getAppWidgetOptions(appWidgetId);
        // Widget dimensions
        final int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        final int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        final int cellHeight = getCellsForSize(minHeight);
        final int cellWidth = getCellsForSize(minWidth);

        final WidgetUtils.WidgetBackground background = WidgetUtils.getWidgetBackground(appWidgetId);
        WidgetUtils.WidgetBackgroundStyle style = null;
        if (isBackgroundOptionalWidget(provider.getWidgetType())) {
            int backgroundColor = getBackgroundColor(appWidgetId, background);

            if (background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
                style = WidgetUtils.getBackgroundStyle(appWidgetId);

                if (weather.getImageData() == null)
                    weather.updateBackground();

                updateViews.removeAllViews(R.id.panda_container);
                updateViews.addView(R.id.panda_container, new RemoteViews(context.getPackageName(), R.layout.layout_panda_bg));

                if (style == WidgetUtils.WidgetBackgroundStyle.PANDA) {
                    // No-op
                } else if (style == WidgetUtils.WidgetBackgroundStyle.PENDINGCOLOR) {
                    updateViews.setImageViewResource(R.id.panda_background, R.drawable.widget_background);
                    updateViews.setInt(R.id.panda_background, "setColorFilter", weather.getPendingBackground());
                } else if (style == WidgetUtils.WidgetBackgroundStyle.LIGHT) {
                    updateViews.setImageViewResource(R.id.panda_background, R.drawable.widget_background);
                    updateViews.setInt(R.id.panda_background, "setColorFilter", Colors.WHITE);
                } else if (style == WidgetUtils.WidgetBackgroundStyle.DARK) {
                    updateViews.setImageViewResource(R.id.panda_background, R.drawable.widget_background);
                    updateViews.setInt(R.id.panda_background, "setColorFilter", Colors.BLACK);
                } else {
                    updateViews.removeAllViews(R.id.panda_container);
                }

                updateViews.setInt(R.id.widgetBackground, "setColorFilter", backgroundColor);
                updateViews.setInt(R.id.widgetBackground, "setImageAlpha", 0xFF);
                String backgroundUri = weather.getImageData() != null ? weather.getImageData().getImageURI() : null;
                loadBackgroundImage(updateViews, provider, appWidgetId, backgroundUri, cellWidth, cellHeight);
            } else if (background == WidgetUtils.WidgetBackground.TRANSPARENT) {
                updateViews.setImageViewResource(R.id.widgetBackground, R.drawable.widget_background);
                updateViews.setInt(R.id.widgetBackground, "setColorFilter", Colors.BLACK);
                updateViews.setInt(R.id.widgetBackground, "setImageAlpha", 0x00);
                updateViews.setInt(R.id.panda_background, "setColorFilter", Colors.TRANSPARENT);
                updateViews.setImageViewBitmap(R.id.panda_background, null);
            } else {
                updateViews.setImageViewBitmap(R.id.widgetBackground, ImageUtils.createColorBitmap(backgroundColor));
                updateViews.setInt(R.id.widgetBackground, "setColorFilter", Colors.TRANSPARENT);
                updateViews.setInt(R.id.widgetBackground, "setImageAlpha", 0xFF);
                updateViews.setInt(R.id.panda_background, "setColorFilter", Colors.TRANSPARENT);
                updateViews.setImageViewBitmap(R.id.panda_background, null);
            }
        }

        if (provider.getWidgetType() == WidgetType.Widget2x2) {
            updateViews.removeAllViews(R.id.weather_notif_layout);

            if (style != WidgetUtils.WidgetBackgroundStyle.PANDA)
                updateViews.addView(R.id.weather_notif_layout, new RemoteViews(context.getPackageName(), R.layout.app_widget_2x2_notif_layout));
            else
                updateViews.addView(R.id.weather_notif_layout, new RemoteViews(context.getPackageName(), R.layout.app_widget_2x2_notif_layout_themed));
        }

        // Colors
        setTextColorDependents(updateViews, provider, appWidgetId, weather, background, style);

        // Location Name
        updateViews.setTextViewText(R.id.location_name, weather.getLocation());

        // Set specific data for widgets
        if (provider.getWidgetType() == WidgetType.Widget2x2 || provider.getWidgetType() == WidgetType.Widget4x1Notification) {
            // Condition text
            updateViews.setTextViewText(R.id.condition_weather,
                    String.format(Locale.ROOT, "%s - %s",
                            !StringUtils.isNullOrWhitespace(weather.getCurTemp()) ? weather.getCurTemp() : WeatherIcons.PLACEHOLDER,
                            weather.getCurCondition()));

            updateViews.setTextViewText(R.id.condition_hi, !StringUtils.isNullOrWhitespace(weather.getHiTemp()) ? weather.getHiTemp() : WeatherIcons.PLACEHOLDER);
            updateViews.setTextViewText(R.id.condition_lo, !StringUtils.isNullOrWhitespace(weather.getLoTemp()) ? weather.getLoTemp() : WeatherIcons.PLACEHOLDER);
            updateViews.setViewVisibility(R.id.condition_hilo_layout, weather.isShowHiLo() ? View.VISIBLE : View.GONE);

            DetailItemViewModel chanceModel = null;
            DetailItemViewModel windModel = null;

            for (DetailItemViewModel input : weather.getWeatherDetails()) {
                if (input != null && (input.getDetailsType() == WeatherDetailsType.POPCHANCE || input.getDetailsType() == WeatherDetailsType.POPCLOUDINESS)) {
                    chanceModel = input;
                } else if (input != null && input.getDetailsType() == WeatherDetailsType.WINDSPEED) {
                    windModel = input;
                }

                if (chanceModel != null && windModel != null) {
                    break;
                }
            }

            if (chanceModel != null) {
                updateViews.setTextViewText(R.id.weather_pop, chanceModel.getValue());
                updateViews.setViewVisibility(R.id.weather_pop_layout, View.VISIBLE);
            } else {
                updateViews.setViewVisibility(R.id.weather_pop_layout, View.GONE);
            }

            if (windModel != null) {
                String speed = TextUtils.isEmpty(windModel.getValue()) ? "" : windModel.getValue().toString();
                speed = speed.split(",")[0];
                updateViews.setTextViewText(R.id.weather_windspeed, speed);
                updateViews.setViewVisibility(R.id.weather_wind_layout, View.VISIBLE);
            } else {
                updateViews.setViewVisibility(R.id.weather_wind_layout, View.GONE);
            }

            updateViews.setViewVisibility(R.id.extra_layout, chanceModel != null || windModel != null ? View.VISIBLE : View.GONE);
        } else if (provider.getWidgetType() == WidgetType.Widget4x2 || provider.getWidgetType() == WidgetType.Widget4x2Clock) {
            // Condition text
            updateViews.setTextViewText(R.id.condition_weather, weather.getCurCondition());
        } else if (provider.getWidgetType() == WidgetType.Widget4x2Huawei) {
            // Condition text
            updateViews.setTextViewText(R.id.condition_hilo,
                    String.format(Locale.ROOT, "%s | %s",
                            !StringUtils.isNullOrWhitespace(weather.getHiTemp()) ? weather.getHiTemp() : WeatherIcons.PLACEHOLDER,
                            !StringUtils.isNullOrWhitespace(weather.getLoTemp()) ? weather.getLoTemp() : WeatherIcons.PLACEHOLDER));
            updateViews.setViewVisibility(R.id.condition_hilo, weather.isShowHiLo() ? View.VISIBLE : View.GONE);
        }

        if (provider.getWidgetType() != WidgetType.Widget2x2 && provider.getWidgetType() != WidgetType.Widget4x1Notification) {
            updateViews.setTextViewText(R.id.condition_temp, weather.getCurTemp());
        }

        if (provider.getWidgetType() == WidgetType.Widget4x2) {
            if (!WeatherIconsManager.getInstance().isFontIcon() && background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
                updateViews.setViewVisibility(R.id.weather_icon_overlay, View.VISIBLE);
            } else {
                updateViews.setViewVisibility(R.id.weather_icon_overlay, View.GONE);
            }
        }

        // Set sizes for views
        updateViewSizes(updateViews, provider, newOptions);

        if (isDateWidget(provider.getWidgetType())) {
            // Open default clock/calendar app
            updateViews.setOnClickPendingIntent(R.id.date_panel, getCalendarAppIntent(context));
            refreshDate(new int[]{appWidgetId});
        }

        if (isClockWidget(provider.getWidgetType())) {
            // Open default clock/calendar app
            updateViews.setOnClickPendingIntent(R.id.clock_panel, getClockAppIntent(context));
            refreshClock(new int[]{appWidgetId});
        }

        updateViews.setViewVisibility(R.id.location_name, WidgetUtils.isLocationNameHidden(appWidgetId) ? View.GONE : View.VISIBLE);
        updateViews.setViewVisibility(R.id.settings_button, WidgetUtils.isSettingsButtonHidden(appWidgetId) ? View.GONE : View.VISIBLE);

        setOnClickIntent(context, location, updateViews);
        setOnSettingsClickIntent(context, updateViews, location, appWidgetId);

        return updateViews;
    }

    private void updateViewSizes(final RemoteViews updateViews, final WeatherWidgetProvider provider, final Bundle newOptions) {
        // Widget dimensions
        final int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        final int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        final int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        final int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        final int maxCellHeight = getCellsForSize(maxHeight);
        final int maxCellWidth = getCellsForSize(maxWidth);
        final int cellHeight = getCellsForSize(minHeight);
        final int cellWidth = getCellsForSize(minWidth);
        boolean forceSmallHeight = cellHeight == maxCellHeight;
        boolean isSmallHeight = ((float) maxCellHeight / cellHeight) <= 1.5f;
        boolean isSmallWidth = ((float) maxCellWidth / cellWidth) <= 1.5f;

        if (provider.getWidgetType() == WidgetType.Widget1x1) {
            if (cellWidth > 1 && cellHeight > 1) {
                updateViews.setTextViewTextSize(R.id.location_name, TypedValue.COMPLEX_UNIT_SP, 14);
            } else {
                updateViews.setTextViewTextSize(R.id.location_name, TypedValue.COMPLEX_UNIT_SP, 12);
            }

            if (cellWidth > 2 && cellHeight > 2) {
                updateViews.setTextViewTextSize(R.id.condition_temp, TypedValue.COMPLEX_UNIT_SP, 24);
            } else if (cellWidth > 1 && cellHeight > 1) {
                updateViews.setTextViewTextSize(R.id.condition_temp, TypedValue.COMPLEX_UNIT_SP, 18);
            } else {
                updateViews.setTextViewTextSize(R.id.condition_temp, TypedValue.COMPLEX_UNIT_SP, 16);
            }
        } else if (provider.getWidgetType() == WidgetType.Widget4x1Google) {
            boolean forceSmall = false;
            int textSize = mContext.getResources().getDimensionPixelSize(R.dimen.widget4x1G_text_size); // 24sp
            if (cellWidth <= 3) {
                textSize *= (2f / 3); // 16sp
            } else if (isSmallHeight && cellHeight == 1) {
                textSize *= (5 / 6f); // 20sp
                forceSmall = true;
            } else if (cellWidth == 4) {
                textSize *= 0.75f; // 18sp
            }

            int layoutPadding = (int) ActivityUtils.dpToPx(mContext, forceSmall ? 0 : 12);
            updateViews.setViewPadding(R.id.layout_container, layoutPadding, layoutPadding, layoutPadding, layoutPadding);

            updateViews.setTextViewTextSize(R.id.date_panel, TypedValue.COMPLEX_UNIT_PX, textSize);
            updateViews.setTextViewTextSize(R.id.condition_temp, TypedValue.COMPLEX_UNIT_PX, textSize);
            updateViews.setTextViewTextSize(R.id.location_name, TypedValue.COMPLEX_UNIT_SP, forceSmall ? 12 : 14);
        } else if (provider.getWidgetType() == WidgetType.Widget4x2) {
            int maxHeightSize = (int) ActivityUtils.dpToPx(mContext, 60);
            if (isSmallHeight && cellHeight <= 2 || cellWidth < 4) {
                int iconWidth = (int) ActivityUtils.dpToPx(mContext, 45);
                updateViews.setInt(R.id.weather_icon, "setMaxWidth", iconWidth);
                updateViews.setInt(R.id.weather_icon, "setMaxHeight", maxHeightSize);
            } else {
                int iconWidth = (int) ActivityUtils.dpToPx(mContext, 55);
                updateViews.setInt(R.id.weather_icon, "setMaxWidth", iconWidth);
                updateViews.setInt(R.id.weather_icon, "setMaxHeight", (int) (maxHeightSize * 7f / 6)); // 70dp
            }

            float textSize = ActivityUtils.dpToPx(mContext, 36f);

            if ((isSmallHeight && cellHeight <= 2) || cellWidth < 4)
                textSize = ActivityUtils.dpToPx(mContext, 28f);

            updateViews.setTextViewTextSize(R.id.condition_temp, TypedValue.COMPLEX_UNIT_PX, textSize);
            updateViews.setViewVisibility(R.id.condition_weather, forceSmallHeight && cellHeight <= 2 ? View.GONE : View.VISIBLE);
        } else if (provider.getWidgetType() == WidgetType.Widget4x1) {
            int locTextSize = 12;
            if (cellHeight > 1 && (!isSmallWidth || cellWidth > 4)) {
                locTextSize = 14;
            }

            updateViews.setTextViewTextSize(R.id.location_name, TypedValue.COMPLEX_UNIT_SP, locTextSize);

            if (isSmallHeight && cellHeight == 1) {
                int padding = (int) ActivityUtils.dpToPx(mContext, 0);
                updateViews.setViewPadding(R.id.layout_container, padding, padding, padding, padding);
            } else {
                int padding = (int) ActivityUtils.dpToPx(mContext, 8);
                updateViews.setViewPadding(R.id.layout_container, padding, padding, padding, padding);
            }
        } else if (provider.getWidgetType() == WidgetType.Widget2x2 || provider.getWidgetType() == WidgetType.Widget4x1Notification) {
            boolean largeText = cellHeight > 2 || provider.getWidgetType() == WidgetType.Widget4x1Notification;
            updateViews.setTextViewTextSize(R.id.location_name, TypedValue.COMPLEX_UNIT_SP, largeText ? 16 : 14);
            updateViews.setTextViewTextSize(R.id.condition_weather, TypedValue.COMPLEX_UNIT_SP, largeText ? 15 : 13);
            updateViews.setTextViewTextSize(R.id.condition_hi, TypedValue.COMPLEX_UNIT_SP, largeText ? 14 : 12);
            updateViews.setTextViewTextSize(R.id.divider, TypedValue.COMPLEX_UNIT_SP, largeText ? 14 : 12);
            updateViews.setTextViewTextSize(R.id.condition_lo, TypedValue.COMPLEX_UNIT_SP, largeText ? 14 : 12);
            updateViews.setTextViewTextSize(R.id.weather_pop, TypedValue.COMPLEX_UNIT_SP, largeText ? 14 : 12);
            updateViews.setTextViewTextSize(R.id.weather_windspeed, TypedValue.COMPLEX_UNIT_SP, largeText ? 14 : 12);

            updateViews.setViewVisibility(R.id.extra_layout, cellWidth <= 3 ? View.GONE : View.VISIBLE);
        } else if (provider.getWidgetType() == WidgetType.Widget4x2Clock) {
            updateViews.setViewVisibility(R.id.spacer_left, cellWidth <= 3 ? View.GONE : View.VISIBLE);
            updateViews.setViewVisibility(R.id.spacer_right, cellWidth <= 3 ? View.GONE : View.VISIBLE);
            updateViews.setTextViewTextSize(R.id.condition_temp, TypedValue.COMPLEX_UNIT_SP, cellWidth <= 3 ? 28f : 36f);
        }
    }

    private void setTextColorDependents(final RemoteViews updateViews, WeatherWidgetProvider provider, final int appWidgetId,
                                        WeatherNowViewModel weather, WidgetUtils.WidgetBackground background, @Nullable WidgetUtils.WidgetBackgroundStyle style) {
        int textColor = getTextColor(appWidgetId, background);
        int panelTextColor = getPanelTextColor(appWidgetId, background, style, isNightMode);

        int tempTextSize = 36;
        if (provider.getWidgetType() == WidgetType.Widget4x1Google)
            tempTextSize = 24;

        float shadowRadius = 1.75f;
        if (background != WidgetUtils.WidgetBackground.TRANSPARENT && background != WidgetUtils.WidgetBackground.CUSTOM &&
                (provider.getWidgetType() == WidgetType.Widget2x2 || provider.getWidgetType() == WidgetType.Widget4x2 && background != WidgetUtils.WidgetBackground.CURRENT_CONDITIONS)) {
            shadowRadius = 0f;
        }

        if (provider.getWidgetType() != WidgetType.Widget2x2 &&
                provider.getWidgetType() != WidgetType.Widget4x1Google &&
                provider.getWidgetType() != WidgetType.Widget4x1Notification &&
                provider.getWidgetType() != WidgetType.Widget4x2Clock &&
                provider.getWidgetType() != WidgetType.Widget4x2Huawei &&
                provider.getWidgetType() != WidgetType.Widget4x1) {
            updateViews.setTextColor(R.id.condition_temp, textColor);
        }

        boolean is4x2 = provider.getWidgetType() == WidgetType.Widget4x2;

        if (provider.getWidgetType() != WidgetType.Widget4x1) {
            // WeatherIcon
            if (!WidgetUtils.isBackgroundOptionalWidget(provider.getWidgetType()) || is4x2) {
                updateViews.setImageViewBitmap(R.id.weather_icon,
                        ImageUtils.bitmapFromDrawable(ContextUtils.getThemeContextOverride(mContext, false), weather.getWeatherIcon()));
            } else if (style == WidgetUtils.WidgetBackgroundStyle.PANDA) {
                updateViews.setImageViewResource(R.id.weather_icon, weather.getWeatherIcon());
            } else {
                updateViews.setImageViewBitmap(R.id.weather_icon,
                        ImageUtils.bitmapFromDrawable(ContextUtils.getThemeContextOverride(mContext, style == WidgetUtils.WidgetBackgroundStyle.LIGHT), weather.getWeatherIcon()));
            }
        }

        if (provider.getWidgetType() == WidgetType.Widget2x2) {
            if (style != WidgetUtils.WidgetBackgroundStyle.PANDA) {
                updateViews.setTextColor(R.id.location_name, panelTextColor);
            }
        } else {
            updateViews.setTextColor(R.id.location_name, is4x2 ? textColor : panelTextColor);
        }

        if (provider.getWidgetType() != WidgetType.Widget4x1Google && provider.getWidgetType() != WidgetType.Widget4x1) {
            if (provider.getWidgetType() == WidgetType.Widget2x2) {
                if (style != WidgetUtils.WidgetBackgroundStyle.PANDA) {
                    updateViews.setTextColor(R.id.condition_weather, panelTextColor);
                }
            } else {
                updateViews.setTextColor(R.id.condition_weather, is4x2 ? textColor : panelTextColor);
            }
        }

        if (provider.getWidgetType() == WidgetType.Widget2x2 || provider.getWidgetType() == WidgetType.Widget4x1Notification) {
            updateViews.setImageViewBitmap(R.id.hi_icon,
                    ImageUtils.tintedBitmapFromDrawable(mContext, R.drawable.wi_direction_up, Colors.WHITE)
            );
            updateViews.setImageViewBitmap(R.id.lo_icon,
                    ImageUtils.tintedBitmapFromDrawable(mContext, R.drawable.wi_direction_down, Colors.WHITE)
            );

            if (style != WidgetUtils.WidgetBackgroundStyle.PANDA) {
                updateViews.setTextColor(R.id.condition_hi, panelTextColor);
                updateViews.setTextColor(R.id.divider, panelTextColor);
                updateViews.setTextColor(R.id.condition_lo, panelTextColor);
                updateViews.setTextColor(R.id.weather_pop, panelTextColor);
                updateViews.setTextColor(R.id.weather_windspeed, panelTextColor);
                if (background != WidgetUtils.WidgetBackground.TRANSPARENT) {
                    updateViews.setInt(R.id.hi_icon, "setColorFilter", panelTextColor);
                    updateViews.setInt(R.id.lo_icon, "setColorFilter", panelTextColor);
                    updateViews.setInt(R.id.weather_popicon, "setColorFilter", panelTextColor);
                    updateViews.setInt(R.id.weather_windicon, "setColorFilter", panelTextColor);
                }
            }

            DetailItemViewModel chanceModel = null;
            DetailItemViewModel windModel = null;

            for (DetailItemViewModel input : weather.getWeatherDetails()) {
                if (input != null && (input.getDetailsType() == WeatherDetailsType.POPCHANCE || input.getDetailsType() == WeatherDetailsType.POPCLOUDINESS)) {
                    chanceModel = input;
                } else if (input != null && input.getDetailsType() == WeatherDetailsType.WINDSPEED) {
                    windModel = input;
                }

                if (chanceModel != null && windModel != null) {
                    break;
                }
            }

            if (chanceModel != null) {
                updateViews.setImageViewBitmap(R.id.weather_popicon,
                        ImageUtils.tintedBitmapFromDrawable(mContext, chanceModel.getIcon(), panelTextColor)
                );
            }

            if (windModel != null) {
                updateViews.setImageViewBitmap(R.id.weather_windicon,
                        ImageUtils.rotateBitmap(
                                ImageUtils.tintedBitmapFromDrawable(mContext, R.drawable.wi_direction_up, panelTextColor),
                                windModel.getIconRotation()
                        )
                );
            }
        }

        updateViews.setInt(R.id.settings_button, "setColorFilter", textColor);

        if (isDateWidget(provider.getWidgetType()) && provider.getWidgetType() != WidgetType.Widget4x1Google) {
            updateViews.setTextColor(R.id.date_panel, textColor);
        }

        if (isClockWidget(provider.getWidgetType())) {
            updateViews.setTextColor(R.id.clock_panel, textColor);
        }
    }

    private void loadBackgroundImage(final RemoteViews updateViews, final WeatherWidgetProvider provider, final int appWidgetId, final String backgroundURI, final int cellWidth, final int cellHeight) {
        int imgWidth = 200 * cellWidth;
        int imgHeight = 200 * cellHeight;

        /*
         * Ensure width and height are both > 0
         * To avoid IllegalArgumentException
         */
        if (imgWidth == 0 || imgHeight == 0) {
            switch (provider.getWidgetType()) {
                default:
                case Widget1x1:
                    imgWidth = imgHeight = 200;
                    break;
                case Widget2x2:
                    imgWidth = imgHeight = 200 * 2;
                    break;
                case Widget4x1:
                case Widget4x1Google:
                    imgWidth = 200 * 4;
                    imgHeight = 200;
                    break;
                case Widget4x2:
                    imgWidth = 200 * 4;
                    imgHeight = 200 * 2;
                    break;
            }
        }

        /*
         * The total Bitmap memory used by the RemoteViews object cannot exceed
         * that required to fill the screen 1.5 times,
         * ie. (screen width x screen height x 4 x 1.5) bytes.
         */
        if (maxBitmapSize < 3840000) { // (200 * 4) * (200 * 4) * 4 * 1.5f
            imgWidth = imgHeight = 200;
        } else if (imgHeight * imgWidth * 4 * 1.5f > maxBitmapSize) {
            switch (provider.getWidgetType()) {
                default:
                case Widget1x1:
                    imgWidth = imgHeight = 200;
                    break;
                case Widget2x2:
                    imgWidth = imgHeight = 200 * 2;
                    break;
                case Widget4x1:
                case Widget4x1Google:
                    imgWidth = 200 * 4;
                    imgHeight = 200;
                    break;
                case Widget4x2:
                    imgWidth = 200 * 4;
                    imgHeight = 200 * 2;
                    break;
            }
        }

        try {
            FutureTarget<Bitmap> imgTask = GlideApp.with(mContext)
                    .asBitmap()
                    .load(backgroundURI)
                    .format(DecodeFormat.PREFER_RGB_565)
                    .centerCrop()
                    .transform(new TransparentOverlay(0x33))
                    .thumbnail(0.75f)
                    .submit(imgWidth, imgHeight);

            Bitmap bmp = imgTask.get();

            updateViews.setInt(R.id.widgetBackground, "setColorFilter", Colors.TRANSPARENT);
            updateViews.setInt(R.id.widgetBackground, "setImageAlpha", 0xFF);
            updateViews.setImageViewBitmap(R.id.widgetBackground, bmp);
        } catch (ExecutionException | InterruptedException e) {
            Logger.writeLine(Log.ERROR, e);
        }
    }

    private static void setOnClickIntent(Context context, LocationData location, RemoteViews updateViews) {
        if (updateViews != null)
            updateViews.setOnClickPendingIntent(R.id.widget, getOnClickIntent(context, location));
    }

    private static PendingIntent getOnClickIntent(Context context, LocationData location) {
        // When user clicks on widget, launch to WeatherNow page
        Intent onClickIntent = new Intent(context.getApplicationContext(), MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (!Settings.getHomeData().equals(location)) {
            onClickIntent.putExtra(Constants.KEY_DATA, JSONParser.serializer(location, LocationData.class));
            onClickIntent.putExtra(Constants.FRAGTAG_HOME, false);
        }

        return PendingIntent.getActivity(context, location != null ? location.hashCode() : (int) SystemClock.uptimeMillis(), onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static void setOnSettingsClickIntent(Context context, RemoteViews updateViews, LocationData location, int appWidgetId) {
        // When user clicks on widget, launch to Config activity
        Intent onClickIntent = new Intent(context.getApplicationContext(), WeatherWidgetConfigActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (WidgetUtils.isGPS(appWidgetId)) {
            onClickIntent.putExtra(EXTRA_LOCATIONQUERY, Constants.KEY_GPS);
        } else {
            onClickIntent.putExtra(EXTRA_LOCATIONNAME, location == null ? null : location.getName());
            onClickIntent.putExtra(EXTRA_LOCATIONQUERY, location == null ? null : location.getQuery());
        }

        onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

        PendingIntent clickPendingIntent =
                PendingIntent.getActivity(context, appWidgetId, onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (updateViews != null)
            updateViews.setOnClickPendingIntent(R.id.settings_button, clickPendingIntent);
    }

    private void rebuildWidget(final WeatherWidgetProvider provider, final int appWidgetId, final Bundle newOptions) throws InterruptedException {
        AsyncTask.await(new CallableEx<Void, InterruptedException>() {
            @Override
            public Void call() throws InterruptedException {
                TaskUtils.throwIfCancellationRequested(cts.getToken());

                if (!Settings.useFollowGPS() && WidgetUtils.isGPS(appWidgetId))
                    return null;

                // Get weather data from cache
                Weather weather = WidgetUtils.getWeatherData(appWidgetId);

                TaskUtils.throwIfCancellationRequested(cts.getToken());

                if (weather == null) {
                    LocationData locData = null;
                    if (WidgetUtils.isGPS(appWidgetId))
                        locData = Settings.getLastGPSLocData();
                    else
                        locData = WidgetUtils.getLocationData(appWidgetId);

                    TaskUtils.throwIfCancellationRequested(cts.getToken());

                    if (locData != null) {
                        TaskUtils.throwIfCancellationRequested(cts.getToken());

                        WeatherDataLoader wloader = new WeatherDataLoader(locData);
                        try {
                            WeatherRequest.Builder request =
                                    new WeatherRequest.Builder().forceRefresh(false);

                            if (WidgetUtils.isForecastWidget(provider.getWidgetType())) {
                                request.loadForecasts();
                            }

                            weather = Tasks.await(wloader.loadWeatherData(request.build()));
                        } catch (Exception e) {
                            weather = null;
                        }
                        if (weather != null)
                            WidgetUtils.saveWeatherData(appWidgetId, weather);
                        else
                            return null;
                    } else {
                        Logger.writeLine(Log.DEBUG, "%s: provider: %s; widgetId: %d; Unable to find location data", TAG, provider.getClassName(), appWidgetId);
                        return null;
                    }
                }

                final RemoteViews updateViews = new RemoteViews(mContext.getPackageName(), provider.getWidgetLayoutId());

                // Widget dimensions
                final int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
                final int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
                final int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
                final int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
                final int maxCellHeight = getCellsForSize(maxHeight);
                final int maxCellWidth = getCellsForSize(maxWidth);
                final int cellHeight = getCellsForSize(minHeight);
                final int cellWidth = getCellsForSize(minWidth);
                boolean forceSmallHeight = cellHeight == maxCellHeight;
                boolean isSmallHeight = ((float) maxCellHeight / cellHeight) <= 1.5f;
                boolean isSmallWidth = ((float) maxCellWidth / cellWidth) <= 1.5f;

                if (isBackgroundOptionalWidget(provider.getWidgetType())) {
                    final WidgetUtils.WidgetBackground background = WidgetUtils.getWidgetBackground(appWidgetId);
                    if (background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
                        final ImageDataViewModel imageData = WeatherUtils.getImageData(weather);
                        String backgroundUri = imageData != null ? imageData.getImageURI() : null;
                        loadBackgroundImage(updateViews, provider, appWidgetId, backgroundUri, cellWidth, cellHeight);
                    }
                }

                // Set sizes for views
                updateViewSizes(updateViews, provider, newOptions);

                if (isForecastWidget(provider.getWidgetType())) {
                    // Determine forecast size
                    int forecastLength = getForecastLength(provider.getWidgetType(), cellWidth);
                    if (weather.getForecast().size() < forecastLength)
                        forecastLength = weather.getForecast().size();

                    updateViews.removeAllViews(R.id.forecast_layout);
                    buildForecastPanel(updateViews, provider, appWidgetId, forecastLength, newOptions);
                }

                if (isClockWidget(provider.getWidgetType())) {
                    refreshClock(new int[]{appWidgetId});
                }
                if (isDateWidget(provider.getWidgetType())) {
                    refreshDate(new int[]{appWidgetId});
                }

                mAppWidgetManager.partiallyUpdateAppWidget(appWidgetId, updateViews);
                return null;
            }
        });
    }

    private void buildForecast(RemoteViews updateViews, WeatherWidgetProvider provider, int appWidgetId) {
        updateViews.removeAllViews(R.id.forecast_layout);

        Bundle newOptions = mAppWidgetManager.getAppWidgetOptions(appWidgetId);

        // Widget dimensions
        int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int cellWidth = getCellsForSize(minWidth);

        // Determine forecast size
        int forecastLength = getForecastLength(provider.getWidgetType(), cellWidth);

        buildForecastPanel(updateViews, provider, appWidgetId, forecastLength, newOptions);
    }

    private void buildForecastPanel(
            RemoteViews updateViews, WeatherWidgetProvider provider, int appWidgetId,
            int forecastLength, Bundle newOptions) {
        if (provider.getWidgetType() == WidgetType.Widget4x1 || provider.getWidgetType() == WidgetType.Widget4x2) {
            // Background & Text Color
            WidgetUtils.WidgetBackground background = WidgetUtils.getWidgetBackground(appWidgetId);
            WidgetUtils.WidgetBackgroundStyle style = WidgetUtils.getBackgroundStyle(appWidgetId);
            int textColor = getPanelTextColor(appWidgetId, background, style, isNightMode);
            int tempTextSize = 36;

            RemoteViews forecastPanel = null;
            RemoteViews hrForecastPanel = null;

            List<ForecastItemViewModel> forecasts = getForecasts(appWidgetId, forecastLength);
            List<HourlyForecastItemViewModel> hourlyForecasts = getHourlyForecasts(appWidgetId, forecastLength);
            WidgetUtils.ForecastOption forecastOption = WidgetUtils.getForecastOption(appWidgetId);

            if (forecastOption == WidgetUtils.ForecastOption.DAILY) {
                forecastPanel = new RemoteViews(mContext.getPackageName(), R.layout.app_widget_forecast_layout_container);
            } else if (forecastOption == WidgetUtils.ForecastOption.HOURLY) {
                if (hourlyForecasts.size() > 0)
                    hrForecastPanel = new RemoteViews(mContext.getPackageName(), R.layout.app_widget_forecast_layout_container);
            } else {
                forecastPanel = new RemoteViews(mContext.getPackageName(), R.layout.app_widget_forecast_layout_container);
                if (hourlyForecasts.size() > 0)
                    hrForecastPanel = new RemoteViews(mContext.getPackageName(), R.layout.app_widget_forecast_layout_container);
            }

            for (int i = 0; i < Math.min(forecastLength, forecasts.size()); i++) {
                if (forecastPanel != null) {
                    addForecastItem(forecastPanel, provider, appWidgetId, forecasts.get(i), newOptions, textColor);
                }

                if (hrForecastPanel != null && i < hourlyForecasts.size()) {
                    addForecastItem(hrForecastPanel, provider, appWidgetId, hourlyForecasts.get(i), newOptions, textColor);
                }
            }

            if (forecastPanel != null) {
                updateViews.addView(R.id.forecast_layout, forecastPanel);
            }
            if (hrForecastPanel != null) {
                updateViews.addView(R.id.forecast_layout, hrForecastPanel);
            }

            if (forecastPanel != null && hrForecastPanel != null) {
                updateViews.setOnClickPendingIntent(R.id.forecast_layout, getShowNextIntent(mContext, provider, appWidgetId));
            } else {
                updateViews.setOnClickPendingIntent(R.id.forecast_layout, getOnClickIntent(mContext, WidgetUtils.getLocationData(appWidgetId)));
            }
        }
    }

    private List<ForecastItemViewModel> getForecasts(int appWidgetId, int forecastLength) {
        LocationData locData;
        if (WidgetUtils.isGPS(appWidgetId))
            locData = Settings.getLastGPSLocData();
        else
            locData = WidgetUtils.getLocationData(appWidgetId);

        if (locData != null && locData.isValid()) {
            Weather weather = WidgetUtils.getWeatherData(appWidgetId);
            List<Forecast> forecasts = null;

            if (weather != null && weather.getForecast() != null && !weather.getForecast().isEmpty()) {
                forecasts = weather.getForecast();
            } else {
                Forecasts fcasts = Settings.getWeatherForecastData(locData.getQuery());
                if (fcasts != null && fcasts.getForecast() != null && !fcasts.getForecast().isEmpty()) {
                    forecasts = fcasts.getForecast();
                }
            }

            if (forecasts != null && !forecasts.isEmpty()) {
                List<ForecastItemViewModel> fcasts = new ArrayList<>(forecastLength);

                for (int i = 0; i < Math.min(forecastLength, forecasts.size()); i++) {
                    fcasts.add(new ForecastItemViewModel(forecasts.get(i)));
                }

                return fcasts;
            }
        }

        return Collections.emptyList();
    }

    private List<HourlyForecastItemViewModel> getHourlyForecasts(int appWidgetId, int forecastLength) {
        LocationData locData;
        if (WidgetUtils.isGPS(appWidgetId))
            locData = Settings.getLastGPSLocData();
        else
            locData = WidgetUtils.getLocationData(appWidgetId);

        if (locData != null && locData.isValid()) {
            Weather weather = WidgetUtils.getWeatherData(appWidgetId);
            List<HourlyForecast> forecasts = null;
            ZonedDateTime now = ZonedDateTime.now(locData.getTzOffset());

            if (weather != null && weather.getHrForecast() != null && !weather.getHrForecast().isEmpty()) {
                forecasts = weather.getHrForecast();
            } else {
                forecasts = Settings.getHourlyForecastsByQueryOrderByDateByLimitFilterByDate(locData.getQuery(), forecastLength, now);
            }

            if (forecasts != null && !forecasts.isEmpty()) {
                List<HourlyForecastItemViewModel> fcasts = new ArrayList<>(forecastLength);

                int count = 0;
                for (HourlyForecast fcast : forecasts) {
                    if (!fcast.getDate().truncatedTo(ChronoUnit.HOURS).isBefore(now.truncatedTo(ChronoUnit.HOURS))) {
                        fcasts.add(new HourlyForecastItemViewModel(fcast));
                        count++;
                    }

                    if (count >= forecastLength) break;
                }

                return fcasts;
            }
        }

        return Collections.emptyList();
    }

    private static PendingIntent getShowNextIntent(Context context, WeatherWidgetProvider provider, int appWidgetId) {
        Intent showNext = new Intent(context, provider.getClass())
                .setAction(WeatherWidgetProvider.ACTION_SHOWNEXTFORECAST)
                .putExtra(WeatherWidgetProvider.EXTRA_WIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, appWidgetId, showNext, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void addForecastItem(RemoteViews forecastPanel, WeatherWidgetProvider provider, int appWidgetId, BaseForecastItemViewModel forecast, Bundle newOptions, int textColor) {
        // Widget dimensions
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int maxCellHeight = getCellsForSize(maxHeight);
        int maxCellWidth = getCellsForSize(maxWidth);
        int cellHeight = getCellsForSize(minHeight);
        int cellWidth = getCellsForSize(minWidth);
        boolean forceSmallWidth = cellWidth == maxCellWidth;
        boolean forceSmallHeight = cellHeight == maxCellHeight;
        boolean isSmallHeight = ((float) maxCellHeight / cellHeight) <= 1.5f;
        boolean isSmallWidth = ((float) maxCellWidth / cellWidth) <= 1.5f;
        WidgetUtils.WidgetBackground background = WidgetUtils.getWidgetBackground(appWidgetId);
        WidgetUtils.WidgetBackgroundStyle style = WidgetUtils.getBackgroundStyle(appWidgetId);

        RemoteViews forecastItem;
        if (provider.getWidgetType() == WidgetType.Widget4x1 || style != WidgetUtils.WidgetBackgroundStyle.PANDA) {
            forecastItem = new RemoteViews(mContext.getPackageName(), R.layout.app_widget_forecast_item);
        } else {
            forecastItem = new RemoteViews(mContext.getPackageName(), R.layout.app_widget_forecast_item_themed);
        }

        int maxIconSize;
        if (provider.getWidgetType() == WidgetType.Widget4x1) {
            maxIconSize = (int) ActivityUtils.dpToPx(mContext, 30);
            if ((!isSmallWidth || cellWidth > 4) && maxCellHeight > 0 && (maxHeight / maxCellHeight) >= 72) {
                maxIconSize *= (8 / 5f); // 48dp
            }
        } else {
            maxIconSize = (int) ActivityUtils.dpToPx(mContext, 48);
        }
        forecastItem.setInt(R.id.forecast_icon, "setMaxWidth", maxIconSize);
        forecastItem.setInt(R.id.forecast_icon, "setMaxHeight", maxIconSize);

        forecastItem.setTextViewText(R.id.forecast_date, forecast.getShortDate());
        forecastItem.setTextViewText(R.id.forecast_hi, forecast.getHiTemp());
        if (forecast instanceof ForecastItemViewModel) {
            forecastItem.setTextViewText(R.id.forecast_lo, ((ForecastItemViewModel) forecast).getLoTemp());
        }

        if (background != WidgetUtils.WidgetBackground.CURRENT_CONDITIONS || style != WidgetUtils.WidgetBackgroundStyle.PANDA) {
            forecastItem.setTextColor(R.id.forecast_date, textColor);
            forecastItem.setTextColor(R.id.forecast_hi, textColor);
            if (forecast instanceof ForecastItemViewModel) {
                forecastItem.setTextColor(R.id.divider, textColor);
                forecastItem.setTextColor(R.id.forecast_lo, textColor);
            }
        }

        // WeatherIcon
        if (!WidgetUtils.isBackgroundOptionalWidget(provider.getWidgetType())) {
            forecastItem.setImageViewBitmap(R.id.forecast_icon,
                    ImageUtils.bitmapFromDrawable(ContextUtils.getThemeContextOverride(mContext, false), forecast.getWeatherIcon()));
        } else if (background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS && style == WidgetUtils.WidgetBackgroundStyle.PANDA) {
            forecastItem.setImageViewResource(R.id.forecast_icon, forecast.getWeatherIcon());
        } else {
            forecastItem.setImageViewBitmap(R.id.forecast_icon,
                    ImageUtils.bitmapFromDrawable(ContextUtils.getThemeContextOverride(mContext, style == WidgetUtils.WidgetBackgroundStyle.LIGHT), forecast.getWeatherIcon()));
        }

        if (provider.getWidgetType() == WidgetType.Widget4x1) {
            if (cellHeight <= 1) {
                forecastItem.setViewPadding(R.id.forecast_date, 0, 0, 0, 0);
            } else {
                int padding = (int) ActivityUtils.dpToPx(mContext, 2);
                forecastItem.setViewPadding(R.id.forecast_date, padding, padding, padding, padding);
            }

            int textSize = 12;
            if (cellHeight > 1 && (!isSmallWidth || cellWidth > 4))
                textSize = 14;

            forecastItem.setTextViewTextSize(R.id.forecast_date, TypedValue.COMPLEX_UNIT_SP, textSize);
            forecastItem.setTextViewTextSize(R.id.forecast_hi, TypedValue.COMPLEX_UNIT_SP, textSize);
            if (forecast instanceof ForecastItemViewModel) {
                forecastItem.setTextViewTextSize(R.id.forecast_lo, TypedValue.COMPLEX_UNIT_SP, textSize);
            }
        } else {
            int textSize = 12;
            if ((!isSmallHeight && cellHeight > 2) && (!isSmallWidth || (cellWidth > 4)))
                textSize = 14;

            forecastItem.setTextViewTextSize(R.id.forecast_date, TypedValue.COMPLEX_UNIT_SP, textSize);
            forecastItem.setTextViewTextSize(R.id.forecast_hi, TypedValue.COMPLEX_UNIT_SP, textSize);
            if (forecast instanceof ForecastItemViewModel) {
                forecastItem.setTextViewTextSize(R.id.divider, TypedValue.COMPLEX_UNIT_SP, textSize);
                forecastItem.setTextViewTextSize(R.id.forecast_lo, TypedValue.COMPLEX_UNIT_SP, textSize);
            }
        }

        if (forecast instanceof HourlyForecastItemViewModel) {
            forecastItem.setViewVisibility(R.id.divider, View.GONE);
            forecastItem.setViewVisibility(R.id.forecast_lo, View.GONE);
        }

        forecastPanel.addView(R.id.forecast_container, forecastItem);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        final int currentNightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        isNightMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }
}