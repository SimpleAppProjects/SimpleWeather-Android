package com.thewizrd.simpleweather.widgets;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.AlarmClock;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.JobIntentService;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.common.util.ArrayUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.AsyncTaskEx;
import com.thewizrd.shared_resources.CallableEx;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.WearableHelper;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ConversionMethods;
import com.thewizrd.shared_resources.utils.ImageUtils;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.TransparentOverlay;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.helpers.ActivityUtils;
import com.thewizrd.simpleweather.main.MainActivity;
import com.thewizrd.simpleweather.notifications.WeatherNotificationBuilder;
import com.thewizrd.simpleweather.notifications.WeatherNotificationService;
import com.thewizrd.simpleweather.wearable.WearableDataListenerService;
import com.thewizrd.simpleweather.weatheralerts.WeatherAlertHandler;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.thewizrd.simpleweather.widgets.WidgetUtils.getBackgroundColor;
import static com.thewizrd.simpleweather.widgets.WidgetUtils.getPanelTextColor;
import static com.thewizrd.simpleweather.widgets.WidgetUtils.getTextColor;
import static com.thewizrd.simpleweather.widgets.WidgetUtils.getWidgetTypeFromID;
import static com.thewizrd.simpleweather.widgets.WidgetUtils.isBackgroundOptionalWidget;
import static com.thewizrd.simpleweather.widgets.WidgetUtils.isClockWidget;
import static com.thewizrd.simpleweather.widgets.WidgetUtils.isDateWidget;
import static com.thewizrd.simpleweather.widgets.WidgetUtils.isForecastWidget;

public class WeatherWidgetService extends JobIntentService {
    private static String TAG = "WeatherWidgetService";

    public static final String ACTION_REFRESHWIDGET = "SimpleWeather.Droid.action.REFRESH_WIDGET";
    public static final String ACTION_RESIZEWIDGET = "SimpleWeather.Droid.action.RESIZE_WIDGET";
    public static final String ACTION_UPDATECLOCK = "SimpleWeather.Droid.action.UPDATE_CLOCK";
    public static final String ACTION_UPDATEDATE = "SimpleWeather.Droid.action.UPDATE_DATE";
    public static final String ACTION_UPDATEWEATHER = "SimpleWeather.Droid.action.UPDATE_WEATHER";

    public static final String ACTION_STARTALARM = "SimpleWeather.Droid.action.START_ALARM";
    public static final String ACTION_CANCELALARM = "SimpleWeather.Droid.action.CANCEL_ALARM";
    public static final String ACTION_UPDATEALARM = "SimpleWeather.Droid.action.UPDATE_ALARM";

    public static final String ACTION_STARTCLOCK = "SimpleWeather.Droid.action.START_CLOCKALARM";
    public static final String ACTION_CANCELCLOCK = "SimpleWeather.Droid.action.CANCEL_CLOCKALARM";

    public static final String ACTION_SHOWALERTS = "SimpleWeather.Droid.action.SHOW_ALERTS";

    public static final String ACTION_RESETGPSWIDGETS = "SimpleWeather.Droid.action.RESET_GPSWIDGETS";
    public static final String ACTION_REFRESHGPSWIDGETS = "SimpleWeather.Droid.action.REFRESH_GPSWIDGETS";

    public static final String EXTRA_LOCATIONNAME = "SimpleWeather.Droid.extra.LOCATION_NAME";
    public static final String EXTRA_LOCATIONQUERY = "SimpleWeather.Droid.extra.LOCATION_QUERY";

    private static final int JOB_ID = 1000;

    private Context mContext;
    private AppWidgetManager mAppWidgetManager;
    private static boolean mAlarmStarted = false;
    private static BroadcastReceiver mTickReceiver;

    private WeatherManager wm;

    // Weather Widget Providers
    private WeatherWidgetProvider1x1 mAppWidget1x1 =
            WeatherWidgetProvider1x1.getInstance();
    private WeatherWidgetProvider2x2 mAppWidget2x2 =
            WeatherWidgetProvider2x2.getInstance();
    private WeatherWidgetProvider4x1 mAppWidget4x1 =
            WeatherWidgetProvider4x1.getInstance();
    private WeatherWidgetProvider4x2 mAppWidget4x2 =
            WeatherWidgetProvider4x2.getInstance();
    private WeatherWidgetProvider4x1Google mAppWidget4x1Google =
            WeatherWidgetProvider4x1Google.getInstance();

    private static boolean isNightMode = false;

    private static final int FORECAST_LENGTH = 3; // 3-day
    private static final int MEDIUM_FORECAST_LENGTH = 4; // 4-day
    private static final int WIDE_FORECAST_LENGTH = 5; // 5-day

    private FusedLocationProviderClient mFusedLocationClient;
    private CancellationTokenSource cts;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, WeatherWidgetService.class,
                JOB_ID, work);
    }

    private boolean isCtsCancelRequested() {
        if (cts != null)
            return cts.getToken().isCancellationRequested();
        else
            return true;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();
        mAppWidgetManager = AppWidgetManager.getInstance(mContext);
        wm = WeatherManager.getInstance();

        // Check if alarm is already set
        mAlarmStarted = (PendingIntent.getBroadcast(mContext, 0,
                new Intent(mContext, WeatherWidgetBroadcastReceiver.class)
                        .setAction(ACTION_UPDATEWEATHER),
                PendingIntent.FLAG_NO_CREATE) != null);

        final int currentNightMode = mContext.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        isNightMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;

        final Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                Logger.writeLine(Log.ERROR, e, "%s: Unhandled Exception %s", TAG, e.getMessage());

                if (oldHandler != null) {
                    oldHandler.uncaughtException(t, e);
                } else {
                    System.exit(2);
                }
            }
        });

        cts = new CancellationTokenSource();

        if (WearableHelper.isGooglePlayServicesInstalled()) {
            mFusedLocationClient = new FusedLocationProviderClient(this);
        }
    }

    @Override
    public void onDestroy() {
        if (Settings.showOngoingNotification() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            WeatherNotificationBuilder.showRefresh(false);

        Logger.writeLine(Log.INFO, "%s: destroying service and cancelling tasks...", TAG);

        try {
            if (cts != null) cts.cancel();
        } catch (Exception ex) {
            Logger.writeLine(Log.ERROR, ex, "%s: Error cancelling task...", TAG);
        }

        super.onDestroy();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
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
                    // We don't know the widget type to update,
                    // so just update all
                    case Unknown:
                    default:
                        refreshWidgets();
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
                }
            } else if (ACTION_STARTALARM.equals(intent.getAction())) {
                // Start alarm if it hasn't started already
                startAlarm(mContext);
            } else if (ACTION_CANCELALARM.equals(intent.getAction())) {
                // Cancel all alarms if no widgets exist
                cancelAlarms(mContext);
            } else if (ACTION_UPDATEALARM.equals(intent.getAction())) {
                // Refresh interval was changed
                // Update alarm
                updateAlarm(mContext);
            } else if (ACTION_STARTCLOCK.equals(intent.getAction())) {
                // Schedule clock updates
                startTickReceiver(mContext);
            } else if (ACTION_CANCELCLOCK.equals(intent.getAction())) {
                // Cancel clock alarm
                cancelClockAlarm(mContext);
            } else if (ACTION_UPDATECLOCK.equals(intent.getAction())) {
                // Update clock widget instances
                int[] appWidgetIds = intent.getIntArrayExtra(WeatherWidgetProvider.EXTRA_WIDGET_IDS);
                refreshClock(appWidgetIds);
            } else if (ACTION_UPDATEDATE.equals(intent.getAction())) {
                // Update clock widget instances
                int[] appWidgetIds = intent.getIntArrayExtra(WeatherWidgetProvider.EXTRA_WIDGET_IDS);
                refreshDate(appWidgetIds);
            } else if (WeatherNotificationService.ACTION_REFRESHNOTIFICATION.equals(intent.getAction())) {
                final boolean forceRefresh = intent.getBooleanExtra(WeatherNotificationService.EXTRA_FORCEREFRESH, false);

                if (Settings.isWeatherLoaded()) {
                    Weather weather = new AsyncTask<Weather>().await(new Callable<Weather>() {
                        @Override
                        public Weather call() throws Exception {
                            return getWeather(forceRefresh);
                        }
                    });

                    if (Settings.showOngoingNotification() && weather != null)
                        WeatherNotificationBuilder.updateNotification(new WeatherNowViewModel(weather));
                }
            } else if (ACTION_UPDATEWEATHER.equals(intent.getAction())) {
                if (Settings.isWeatherLoaded()) {
                    // Send broadcast to signal update
                    if (widgetsExist(App.getInstance().getAppContext()))
                        sendBroadcast(new Intent(WeatherWidgetProvider.ACTION_SHOWREFRESH));
                    // NOTE: Don't try to show refresh for pre-M devices
                    // If app gets killed, instance of notif is lost & view is reset
                    // and might get stuck
                    if (Settings.showOngoingNotification() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        WeatherNotificationBuilder.showRefresh(true);

                    if (widgetsExist(App.getInstance().getAppContext()))
                        refreshWidgets();

                    // Update for home
                    final Weather weather = new AsyncTask<Weather>().await(new Callable<Weather>() {
                        @Override
                        public Weather call() throws Exception {
                            return getWeather();
                        }
                    });

                    if (weather != null) {
                        if (Settings.showOngoingNotification())
                            WeatherNotificationBuilder.updateNotification(new WeatherNowViewModel(weather));
                        if (Settings.useAlerts() && wm.supportsAlerts() && weather != null) {
                            AsyncTask.run(new Runnable() {
                                @Override
                                public void run() {
                                    WeatherAlertHandler.postAlerts(Settings.getHomeData(), weather.getWeatherAlerts());
                                }
                            });
                        }
                    }
                }
            } else if (ACTION_RESETGPSWIDGETS.equals(intent.getAction())) {
                // GPS feature disabled; reset widget
                resetGPSWidgets();
            } else if (ACTION_REFRESHGPSWIDGETS.equals(intent.getAction())) {
                refreshGPSWidgets();
            }

            Logger.writeLine(Log.INFO, "%s: Intent Action = %s", TAG, intent.getAction());
        } catch (Exception ex) {
            Logger.writeLine(Log.ERROR, ex, "%s: exception occurred...", TAG);
        }
    }

    private static PendingIntent getAlarmIntent(Context context) {
        Intent intent = new Intent(context, WeatherWidgetBroadcastReceiver.class)
                .setAction(ACTION_UPDATEWEATHER);

        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    private static void updateAlarm(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        int interval = Settings.getRefreshInterval();

        boolean startNow = !mAlarmStarted;
        long intervalMillis = Duration.ofMinutes(interval).toMillis();
        long triggerAtTime = SystemClock.elapsedRealtime() + intervalMillis;

        if (startNow) {
            enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                    .setAction(ACTION_UPDATEWEATHER));
        }

        PendingIntent pendingIntent = getAlarmIntent(context);
        am.cancel(pendingIntent);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, triggerAtTime, intervalMillis, pendingIntent);
        mAlarmStarted = true;

        Logger.writeLine(Log.INFO, "%s: Updated alarm", TAG);
    }

    private void cancelAlarms(Context context) {
        // Cancel alarm if dependent features are turned off
        if (!widgetsExist(context) && !Settings.showOngoingNotification() && !Settings.useAlerts()) {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.cancel(getAlarmIntent(context));
            mAlarmStarted = false;

            Logger.writeLine(Log.INFO, "%s: Canceled alarm", TAG);
        }
    }

    private void startAlarm(Context context) {
        // Start alarm if dependent features are enabled
        if (!mAlarmStarted && (widgetsExist(context) || Settings.showOngoingNotification() || Settings.useAlerts())) {
            updateAlarm(context);
            mAlarmStarted = true;
        }
    }

    private static PendingIntent getClockRefreshIntent(Context context) {
        Intent intent = new Intent(context, WeatherWidgetBroadcastReceiver.class)
                .setAction(ACTION_UPDATECLOCK);

        return PendingIntent.getBroadcast(context, 0, intent, 0);
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
                am.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), 60000, pendingIntent);

                stopTickReceiver(context);

                Logger.writeLine(Log.INFO, "%s: Receieved tick in receiver", TAG);
            }
        }
    }

    private boolean widgetsExist(Context context) {
        return mAppWidget1x1.hasInstances(context)
                || mAppWidget2x2.hasInstances(context)
                || mAppWidget4x1.hasInstances(context)
                || mAppWidget4x2.hasInstances(context)
                || mAppWidget4x1Google.hasInstances(context);
    }

    private void resizeWidget(WeatherWidgetProvider provider, int appWidgetId, Bundle newOptions) throws InterruptedException {
        if (Settings.isWeatherLoaded()) rebuildWidget(provider, appWidgetId, newOptions);
    }

    private void refreshWidget(final WeatherWidgetProvider provider, int[] appWidgetIds) throws InterruptedException {
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

    private void refreshWidgets() throws InterruptedException {
        if (Settings.isWeatherLoaded()) {
            // Build the widget update for available providers
            // Add widget providers here
            List<Task<Void>> tasks = new ArrayList<>();

            if (mAppWidget1x1.hasInstances(WeatherWidgetService.this)) {
                Task<Void> task = AsyncTask.create(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Logger.writeLine(Log.DEBUG, "%s: refreshWidgets: started 1x1", TAG);
                        int[] appWidgetIds = mAppWidgetManager.getAppWidgetIds(mAppWidget1x1.getComponentName());

                        for (int id : appWidgetIds) {
                            refreshWidget(mAppWidget1x1, id);
                        }
                        return null;
                    }
                });

                tasks.add(task);
            }

            if (mAppWidget2x2.hasInstances(WeatherWidgetService.this)) {
                Task<Void> task = AsyncTask.create(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Logger.writeLine(Log.DEBUG, "%s: refreshWidgets: started 2x2", TAG);
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

            if (mAppWidget4x1.hasInstances(WeatherWidgetService.this)) {
                Task<Void> task = AsyncTask.create(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Logger.writeLine(Log.DEBUG, "%s: refreshWidgets: started 4x1", TAG);
                        int[] appWidgetIds = mAppWidgetManager.getAppWidgetIds(mAppWidget4x1.getComponentName());

                        for (int id : appWidgetIds) {
                            refreshWidget(mAppWidget4x1, id);
                        }
                        return null;
                    }
                });

                tasks.add(task);
            }

            if (mAppWidget4x2.hasInstances(WeatherWidgetService.this)) {
                Task<Void> task = AsyncTask.create(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Logger.writeLine(Log.DEBUG, "%s: refreshWidgets: started 4x2", TAG);
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

            if (mAppWidget4x1Google.hasInstances(WeatherWidgetService.this)) {
                Task<Void> task = AsyncTask.create(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Logger.writeLine(Log.DEBUG, "%s: refreshWidgets: started 4x1Google", TAG);
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

            try {
                Tasks.await(Tasks.whenAll(tasks));
            } catch (ExecutionException | InterruptedException e) {
                Logger.writeLine(Log.ERROR, e);
            }
        }
    }

    private void refreshWidget(final WeatherWidgetProvider provider, int appWidgetId) throws InterruptedException {
        LocationData locData = null;

        if (WidgetUtils.isGPS(appWidgetId)) {
            if (!Settings.useFollowGPS())
                return;
            else
                locData = Settings.getLastGPSLocData();
        } else {
            locData = WidgetUtils.getLocationData(appWidgetId);
        }

        if (isCtsCancelRequested()) throw new InterruptedException();

        Weather weather = null;

        if (locData != null) {
            if (isCtsCancelRequested()) throw new InterruptedException();

            if (locData.equals(Settings.getHomeData())) {
                weather = getWeather();
            } else {
                WeatherDataLoader wloader = new WeatherDataLoader(locData);
                try {
                    wloader.loadWeatherData(false);
                    weather = wloader.getWeather();
                } catch (Exception e) {
                    weather = null;
                }
            }

            if (weather != null) {
                // Save weather data
                WidgetUtils.saveWeatherData(appWidgetId, weather);

                WeatherNowViewModel viewModel = new WeatherNowViewModel(weather);

                // Build the widget update for provider
                RemoteViews views = buildUpdate(mContext, provider, appWidgetId, locData, viewModel);
                // Push update for this widget to the home screen
                mAppWidgetManager.updateAppWidget(appWidgetId, views);
                buildForecast(provider, viewModel, appWidgetId);
            } else {
                Logger.writeLine(Log.DEBUG, "%s: provider: %s; widgetId: %d; Unable to find weather data", TAG, provider.getClassName(), appWidgetId);
            }
        } else {
            Logger.writeLine(Log.DEBUG, "%s: provider: %s; widgetId: %d; Unable to find location data", TAG, provider.getClassName(), appWidgetId);
        }
    }

    private void resetGPSWidgets() {
        final int[] appWidgetIds = WidgetUtils.getWidgetIds("GPS");

        List<Task<Void>> tasks = new ArrayList<>();

        for (final int appWidgetId : appWidgetIds) {
            Task<Void> task = AsyncTask.create(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.app_widget_configure_layout);

                    Intent configureIntent = new Intent(mContext, WeatherWidgetConfigActivity.class);
                    configureIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

                    PendingIntent clickPendingIntent =
                            PendingIntent.getActivity(mContext, 0, configureIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    views.setOnClickPendingIntent(R.id.widget, clickPendingIntent);

                    mAppWidgetManager.updateAppWidget(appWidgetIds, views);
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

    private void refreshGPSWidgets() {
        final int[] appWidgetIds = WidgetUtils.getWidgetIds("GPS");

        final int[] ids1x1 = mAppWidgetManager.getAppWidgetIds(mAppWidget1x1.getComponentName());
        final int[] ids2x2 = mAppWidgetManager.getAppWidgetIds(mAppWidget2x2.getComponentName());
        final int[] ids4x1 = mAppWidgetManager.getAppWidgetIds(mAppWidget4x1.getComponentName());
        final int[] ids4x2 = mAppWidgetManager.getAppWidgetIds(mAppWidget4x2.getComponentName());
        final int[] ids4x1G = mAppWidgetManager.getAppWidgetIds(mAppWidget4x1Google.getComponentName());
        final Weather weather = getWeather();
        final LocationData locationData = Settings.getLastGPSLocData();

        List<Task<Void>> tasks = new ArrayList<>();

        for (final int appWidgetId : appWidgetIds) {
            Task<Void> task = AsyncTask.create(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    if (weather != null) {
                        // Save weather data
                        WidgetUtils.saveWeatherData(appWidgetId, weather);

                        WeatherNowViewModel viewModel = new WeatherNowViewModel(weather);

                        if (ArrayUtils.contains(ids1x1, appWidgetId)) {
                            // Build the widget update for provider
                            RemoteViews views = buildUpdate(mContext, mAppWidget1x1, appWidgetId, locationData, viewModel);
                            // Push update for this widget to the home screen
                            mAppWidgetManager.updateAppWidget(appWidgetId, views);
                            buildForecast(mAppWidget1x1, viewModel, appWidgetId);
                        } else if (ArrayUtils.contains(ids2x2, appWidgetId)) {
                            // Build the widget update for provider
                            RemoteViews views = buildUpdate(mContext, mAppWidget2x2, appWidgetId, locationData, viewModel);
                            // Push update for this widget to the home screen
                            mAppWidgetManager.updateAppWidget(appWidgetId, views);
                            buildForecast(mAppWidget2x2, viewModel, appWidgetId);
                        } else if (ArrayUtils.contains(ids4x1, appWidgetId)) {
                            // Build the widget update for provider
                            RemoteViews views = buildUpdate(mContext, mAppWidget4x1, appWidgetId, locationData, viewModel);
                            // Push update for this widget to the home screen
                            mAppWidgetManager.updateAppWidget(appWidgetId, views);
                            buildForecast(mAppWidget4x1, viewModel, appWidgetId);
                        } else if (ArrayUtils.contains(ids4x2, appWidgetId)) {
                            // Build the widget update for provider
                            RemoteViews views = buildUpdate(mContext, mAppWidget4x2, appWidgetId, locationData, viewModel);
                            // Push update for this widget to the home screen
                            mAppWidgetManager.updateAppWidget(appWidgetId, views);
                            buildForecast(mAppWidget4x2, viewModel, appWidgetId);
                        } else if (ArrayUtils.contains(ids4x1G, appWidgetId)) {
                            // Build the widget update for provider
                            RemoteViews views = buildUpdate(mContext, mAppWidget4x1Google, appWidgetId, locationData, viewModel);
                            // Push update for this widget to the home screen
                            mAppWidgetManager.updateAppWidget(appWidgetId, views);
                            buildForecast(mAppWidget4x1Google, viewModel, appWidgetId);
                        } else {
                            Logger.writeLine(Log.DEBUG, "%s: refreshGPSWidgets: unable to find widget provider", TAG);
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
            appWidgetIds = new int[appWidget2x2Ids.length + appWidget4x2Ids.length];
            System.arraycopy(appWidget2x2Ids, 0, appWidgetIds, 0, appWidget2x2Ids.length);
            System.arraycopy(appWidget4x2Ids, 0, appWidgetIds, appWidget2x2Ids.length, appWidget4x2Ids.length);
        }

        List<Task<Void>> tasks = new ArrayList<>();

        for (final int appWidgetId : appWidgetIds) {
            Task<Void> task = AsyncTask.create(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    WidgetType widgetType = getWidgetTypeFromID(appWidgetId);

                    RemoteViews views;

                    if (widgetType == WidgetType.Widget2x2)
                        views = new RemoteViews(mContext.getPackageName(), mAppWidget2x2.getWidgetLayoutId());
                    else if (widgetType == WidgetType.Widget4x2)
                        views = new RemoteViews(mContext.getPackageName(), mAppWidget4x2.getWidgetLayoutId());
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

                    float clockTextSize = mContext.getResources().getDimensionPixelSize(R.dimen.clock_text_size); // 36sp

                    if ((isSmallHeight && cellHeight <= 2) || cellWidth < 4)
                        clockTextSize *= (8f / 9); // 32sp

                    views.setTextViewTextSize(R.id.clock_panel, TypedValue.COMPLEX_UNIT_PX, clockTextSize);

                    // Update clock widgets
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        // TextClock
                        SpannableString timeStr12hr = new SpannableString(mContext.getText(R.string.main_widget_12_hours_format));
                        int start12hr = timeStr12hr.length() - 2;
                        timeStr12hr.setSpan(new TextAppearanceSpan("sans-serif-light", Typeface.NORMAL, (int) (clockTextSize * 0.875f),
                                        ContextCompat.getColorStateList(mContext, android.R.color.white),
                                        ContextCompat.getColorStateList(mContext, android.R.color.white)),
                                start12hr, timeStr12hr.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                        views.setCharSequence(R.id.clock_panel, "setFormat12Hour",
                                timeStr12hr);

                        views.setCharSequence(R.id.clock_panel, "setFormat24Hour",
                                mContext.getText(R.string.clock_24_hours_format));
                    } else {
                        // TextView
                        LocalDateTime now = LocalDateTime.now();

                        SpannableString timeStr;
                        String timeformat = now.format(DateTimeFormatter.ofPattern("h:mma"));
                        int end = timeformat.length() - 2;

                        if (DateFormat.is24HourFormat(App.getInstance().getAppContext())) {
                            timeformat = now.format(DateTimeFormatter.ofPattern("HH:mm"));
                            end = timeformat.length() - 1;
                            timeStr = new SpannableString(timeformat);
                        } else {
                            timeStr = new SpannableString(timeformat);
                            timeStr.setSpan(new TextAppearanceSpan("sans-serif-light", Typeface.NORMAL, (int) (clockTextSize * 0.875f),
                                            ContextCompat.getColorStateList(mContext, android.R.color.white),
                                            ContextCompat.getColorStateList(mContext, android.R.color.white)),
                                    end, timeformat.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }

                        views.setTextViewText(R.id.clock_panel, timeStr);
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
            appWidgetIds = new int[appWidget2x2Ids.length + appWidget4x2Ids.length + appWidget4x1GIds.length];
            System.arraycopy(appWidget2x2Ids, 0, appWidgetIds, 0, appWidget2x2Ids.length);
            System.arraycopy(appWidget4x2Ids, 0, appWidgetIds, appWidget2x2Ids.length, appWidget4x2Ids.length);
            System.arraycopy(appWidget4x1GIds, 0, appWidgetIds, appWidget4x2Ids.length, appWidget4x1GIds.length);
        }

        List<Task<Void>> tasks = new ArrayList<>();

        for (final int appWidgetId : appWidgetIds) {
            Task<Void> task = AsyncTask.create(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    // Update clock widgets
                    WidgetType widgetType = getWidgetTypeFromID(appWidgetId);

                    RemoteViews views;

                    if (widgetType == WidgetType.Widget2x2)
                        views = new RemoteViews(mContext.getPackageName(), mAppWidget2x2.getWidgetLayoutId());
                    else if (widgetType == WidgetType.Widget4x2)
                        views = new RemoteViews(mContext.getPackageName(), mAppWidget4x2.getWidgetLayoutId());
                    else if (widgetType == WidgetType.Widget4x1Google)
                        views = new RemoteViews(mContext.getPackageName(), mAppWidget4x1Google.getWidgetLayoutId());
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

                    if (widgetType != WidgetType.Widget4x1Google) {
                        float dateTextSize = mContext.getResources().getDimensionPixelSize(R.dimen.date_text_size); // 16sp

                        if ((isSmallHeight && cellHeight <= 2) || cellWidth < 4)
                            dateTextSize *= 0.875f; // 14sp

                        views.setTextViewTextSize(R.id.date_panel, TypedValue.COMPLEX_UNIT_PX, dateTextSize);
                    }

                    DateTimeFormatter dtfm;
                    if (widgetType == WidgetType.Widget2x2 && cellWidth >= 3) {
                        dtfm = DateTimeFormatter.ofPattern("eeee, MMMM dd");
                    } else if (widgetType == WidgetType.Widget4x1Google) {
                        dtfm = DateTimeFormatter.ofPattern("eeee, MMM dd");
                    } else {
                        dtfm = DateTimeFormatter.ofPattern("eee, MMM dd");
                    }

                    views.setTextViewText(R.id.date_panel, LocalDateTime.now().format(dtfm));

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

    private static PendingIntent getDefaultCalendarIntent(Context context) {
        Intent onClickIntent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR);
        return PendingIntent.getActivity(context, 0, onClickIntent, 0);
    }

    private static PendingIntent getDefaultClockIntent(Context context) {
        String clockAction = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ?
                AlarmClock.ACTION_SHOW_ALARMS : AlarmClock.ACTION_SET_ALARM;
        Intent onClickIntent = new Intent(clockAction);
        return PendingIntent.getActivity(context, 0, onClickIntent, 0);
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
            int backgroundColor = getBackgroundColor(mContext, background);

            if (background == WidgetUtils.WidgetBackground.CURRENT_CONDITIONS) {
                style = WidgetUtils.getBackgroundStyle(appWidgetId);

                if (style == WidgetUtils.WidgetBackgroundStyle.PANDA) {
                    updateViews.setInt(R.id.panda_background, "setColorFilter", isNightMode ? Colors.BLACK : Colors.WHITE);
                    updateViews.setImageViewResource(R.id.panda_background, R.drawable.widget_background_bottom_corners);
                } else if (style == WidgetUtils.WidgetBackgroundStyle.PENDINGCOLOR) {
                    updateViews.setInt(R.id.panda_background, "setColorFilter", weather.getPendingBackground());
                    updateViews.setImageViewResource(R.id.panda_background, R.drawable.widget_background_bottom_corners);
                } else {
                    updateViews.setImageViewBitmap(R.id.panda_background, null);
                }

                updateViews.setInt(R.id.widgetBackground, "setColorFilter", backgroundColor);
                updateViews.setInt(R.id.widgetBackground, "setImageAlpha", 0xFF);
                loadBackgroundImage(updateViews, appWidgetId, weather.getBackground(), cellWidth, cellHeight);
            } else if (background == WidgetUtils.WidgetBackground.TRANSPARENT) {
                updateViews.setImageViewResource(R.id.widgetBackground, R.drawable.widget_background);
                updateViews.setInt(R.id.widgetBackground, "setColorFilter", Colors.BLACK);
                updateViews.setInt(R.id.widgetBackground, "setImageAlpha", 0x00);
                updateViews.setInt(R.id.panda_background, "setColorFilter", Colors.TRANSPARENT);
                updateViews.setImageViewBitmap(R.id.panda_background, null);
            } else {
                updateViews.setImageViewResource(R.id.widgetBackground, R.drawable.widget_background);
                updateViews.setInt(R.id.widgetBackground, "setColorFilter", backgroundColor);
                updateViews.setInt(R.id.widgetBackground, "setImageAlpha", 0xFF);
                updateViews.setInt(R.id.panda_background, "setColorFilter", Colors.TRANSPARENT);
                updateViews.setImageViewBitmap(R.id.panda_background, null);
            }
        }

        // Colors
        setTextColorDependents(updateViews, provider, weather, background, style);

        // Temperature
        CharSequence temp = weather.getCurTemp();

        // Location Name
        updateViews.setTextViewText(R.id.location_name, weather.getLocation());

        // Update Time
        if (provider.getWidgetType() != WidgetType.Widget4x1Google && provider.getWidgetType() != WidgetType.Widget1x1) {
            String updatetext = getUpdateTimeText(Settings.getUpdateTime(), false);
            updateViews.setTextViewText(R.id.update_time, updatetext);
        }

        // Set specific data for widgets
        if (provider.getWidgetType() == WidgetType.Widget4x1Google) {
            updateViews.setTextViewText(R.id.condition_temp,
                    temp.toString().replace(WeatherIcons.FAHRENHEIT, "ºF").replace(WeatherIcons.CELSIUS, "ºC"));
            updateViews.setViewVisibility(R.id.divider, View.VISIBLE);
        } else if (provider.getWidgetType() == WidgetType.Widget2x2) {
            // Condition text
            updateViews.setTextViewText(R.id.condition_weather,
                    String.format(Locale.ROOT, "%sº - %s", StringUtils.removeNonDigitChars(temp.toString()), weather.getCurCondition()));

            ForecastItemViewModel todaysForecast = weather.getForecasts().get(0);

            updateViews.setTextViewText(R.id.condition_details,
                    String.format(Locale.ROOT, "%s | %s", todaysForecast.getHiTemp(), todaysForecast.getLoTemp()));
        } else if (provider.getWidgetType() == WidgetType.Widget4x2) {
            // Condition text
            updateViews.setTextViewText(R.id.condition_weather, weather.getCurCondition());
        } else if (provider.getWidgetType() == WidgetType.Widget4x1) {
            updateViews.setViewVisibility(R.id.now_date, View.VISIBLE);
        }

        // Set sizes for views
        updateViewSizes(updateViews, provider, newOptions);

        if (isDateWidget(provider.getWidgetType())) {
            // Open default clock/calendar app
            updateViews.setOnClickPendingIntent(R.id.date_panel, getDefaultCalendarIntent(context));
        }

        if (isClockWidget(provider.getWidgetType())) {
            // Open default clock/calendar app
            updateViews.setOnClickPendingIntent(R.id.clock_panel, getDefaultClockIntent(context));
        }

        // Progress bar
        updateViews.setViewVisibility(R.id.refresh_button, View.VISIBLE);
        updateViews.setViewVisibility(R.id.refresh_progress, View.GONE);

        // Set on click refresh intent
        setOnRefreshIntent(context, provider, appWidgetId, updateViews);

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
            if (cellWidth > 1 && cellHeight > 1)
                updateViews.setTextViewTextSize(R.id.location_name, TypedValue.COMPLEX_UNIT_SP, 14);
            else
                updateViews.setTextViewTextSize(R.id.location_name, TypedValue.COMPLEX_UNIT_SP, 12);
        } else if (provider.getWidgetType() == WidgetType.Widget4x1Google) {
            boolean forceSmall = false;
            int textSize = mContext.getResources().getDimensionPixelSize(R.dimen.widget4x1G_text_size); // 24sp
            if (cellWidth <= 3) {
                textSize *= (2f / 3); // 16sp
            } else if (isSmallHeight && cellHeight == 1) {
                //textSize *= (7f / 12); // 14sp
                forceSmall = true;
            } else if (cellWidth == 4) {
                textSize *= 0.75f; // 18sp
            }

            int layoutPadding = (int) ActivityUtils.dpToPx(mContext, forceSmall ? 0 : 12);
            updateViews.setViewPadding(R.id.layout_container, layoutPadding, layoutPadding, layoutPadding, layoutPadding);

            updateViews.setTextViewTextSize(R.id.date_panel, TypedValue.COMPLEX_UNIT_PX, textSize);
            updateViews.setTextViewTextSize(R.id.divider, TypedValue.COMPLEX_UNIT_PX, textSize);
            updateViews.setTextViewTextSize(R.id.condition_temp, TypedValue.COMPLEX_UNIT_PX, textSize);
            updateViews.setTextViewTextSize(R.id.location_name, TypedValue.COMPLEX_UNIT_SP, forceSmall ? 12 : 14);
        } else if (provider.getWidgetType() == WidgetType.Widget4x2) {
            int maxHeightSize = (int) ActivityUtils.dpToPx(mContext, 60);
            if (isSmallHeight && cellHeight <= 2 || cellWidth < 4) {
                int tempWidth = (int) ActivityUtils.dpToPx(mContext, 50);
                int iconWidth = (int) ActivityUtils.dpToPx(mContext, 45);
                updateViews.setInt(R.id.condition_temp, "setMaxWidth", tempWidth);
                updateViews.setInt(R.id.weather_icon, "setMaxWidth", iconWidth);
                updateViews.setInt(R.id.weather_icon, "setMaxHeight", maxHeightSize);
            } else {
                int tempWidth = (int) ActivityUtils.dpToPx(mContext, 60);
                int iconWidth = (int) ActivityUtils.dpToPx(mContext, 55);
                updateViews.setInt(R.id.condition_temp, "setMaxWidth", tempWidth);
                updateViews.setInt(R.id.weather_icon, "setMaxWidth", iconWidth);
                updateViews.setInt(R.id.weather_icon, "setMaxHeight", (int) (maxHeightSize * 7f / 6)); // 70dp
            }
        } else if (provider.getWidgetType() == WidgetType.Widget4x1) {
            int textSize = 12;
            if (cellHeight > 1 && (!isSmallWidth || cellWidth > 4))
                textSize = 14;

            updateViews.setTextViewTextSize(R.id.now_date, TypedValue.COMPLEX_UNIT_SP, textSize);
            updateViews.setTextViewTextSize(R.id.location_name, TypedValue.COMPLEX_UNIT_SP, textSize);

            if (forceSmallHeight) {
                updateViews.setViewPadding(R.id.now_date, 0, 0, 0, 0);
            } else {
                int padding = (int) ActivityUtils.dpToPx(mContext, 2);
                updateViews.setViewPadding(R.id.now_date, padding, padding, padding, padding);
            }
        } else if (provider.getWidgetType() == WidgetType.Widget2x2) {
            updateViews.setTextViewTextSize(R.id.location_name, TypedValue.COMPLEX_UNIT_SP, cellHeight > 2 ? 16 : 14);
            updateViews.setTextViewTextSize(R.id.condition_weather, TypedValue.COMPLEX_UNIT_SP, cellHeight > 2 ? 15 : 13);
            updateViews.setTextViewTextSize(R.id.condition_details, TypedValue.COMPLEX_UNIT_SP, cellHeight > 2 ? 14 : 12);
        }
    }

    private void setTextColorDependents(final RemoteViews updateViews, WeatherWidgetProvider provider,
                                        WeatherNowViewModel weather, WidgetUtils.WidgetBackground background, @Nullable WidgetUtils.WidgetBackgroundStyle style) {
        int textColor = getTextColor(background);
        int panelTextColor = getPanelTextColor(background, style, isNightMode);

        int tempTextSize = 72;
        if (provider.getWidgetType() == WidgetType.Widget4x1Google)
            tempTextSize = 60;

        float shadowRadius = 2.75f;
        if (provider.getWidgetType() == WidgetType.Widget4x1Google)
            shadowRadius = 7.5f;

        if (provider.getWidgetType() != WidgetType.Widget2x2 && provider.getWidgetType() != WidgetType.Widget4x1Google) {
            updateViews.setImageViewBitmap(R.id.condition_temp,
                    ImageUtils.weatherIconToBitmap(mContext, weather.getCurTemp(), tempTextSize, textColor, shadowRadius));
        }

        if (provider.getWidgetType() == WidgetType.Widget4x1) {
            updateViews.setTextColor(R.id.now_date, textColor);
        }

        if (provider.getWidgetType() != WidgetType.Widget4x1Google && provider.getWidgetType() != WidgetType.Widget1x1) {
            updateViews.setTextColor(R.id.update_time, textColor);
        }

        boolean is4x2 = provider.getWidgetType() == WidgetType.Widget4x2;

        // WeatherIcon
        updateViews.setImageViewBitmap(R.id.weather_icon,
                ImageUtils.weatherIconToBitmap(mContext, weather.getWeatherIcon(), tempTextSize, is4x2 ? textColor : panelTextColor, shadowRadius));

        updateViews.setTextColor(R.id.location_name, is4x2 ? textColor : panelTextColor);

        if (provider.getWidgetType() != WidgetType.Widget4x1Google && provider.getWidgetType() != WidgetType.Widget4x1) {
            updateViews.setTextColor(R.id.condition_weather, is4x2 ? textColor : panelTextColor);
            updateViews.setTextColor(R.id.condition_details, is4x2 ? textColor : panelTextColor);
        }

        updateViews.setInt(R.id.refresh_button, "setColorFilter", textColor);
        updateViews.setInt(R.id.settings_button, "setColorFilter", textColor);
    }

    private void loadBackgroundImage(final RemoteViews updateViews, final int appWidgetId, final String backgroundURI, final int cellWidth, final int cellHeight) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                int imgWidth = 200 * cellWidth;
                int imgHeight = 200 * cellHeight;
                float radius = mContext.getResources().getDimensionPixelSize(R.dimen.widget_corner_radius);

                Glide.with(mContext)
                        .asBitmap()
                        .load(backgroundURI)
                        .apply(RequestOptions.formatOf(DecodeFormat.PREFER_RGB_565)
                                .transforms(new CenterCrop(), new TransparentOverlay(0x33), new RoundedCorners((int) radius))
                        )
                        .into(new SimpleTarget<Bitmap>(imgWidth, imgHeight) {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                updateViews.setInt(R.id.widgetBackground, "setColorFilter", Colors.TRANSPARENT);
                                updateViews.setInt(R.id.widgetBackground, "setImageAlpha", 0xFF);
                                updateViews.setImageViewBitmap(R.id.widgetBackground, resource);
                                mAppWidgetManager.partiallyUpdateAppWidget(appWidgetId, updateViews);
                            }
                        });
            }
        });
    }

    private static void setOnRefreshIntent(Context context, WeatherWidgetProvider provider, int appWidgetId, RemoteViews updateViews) {
        Intent refreshIntent = new Intent(context, provider.getClass())
                .setAction(WeatherWidgetProvider.ACTION_REFRESHWIDGETS)
                .putExtra(WeatherWidgetProvider.EXTRA_WIDGET_IDS, new int[]{appWidgetId})
                .putExtra(WeatherWidgetProvider.EXTRA_WIDGET_TYPE, provider.getWidgetType().getValue());
        PendingIntent refreshPendingIntent =
                PendingIntent.getBroadcast(context, appWidgetId, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (updateViews != null)
            updateViews.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent);
    }

    private static void setOnClickIntent(Context context, LocationData location, RemoteViews updateViews) {
        // When user clicks on widget, launch to WeatherNow page
        Intent onClickIntent = new Intent(context.getApplicationContext(), MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (!Settings.getHomeData().equals(location))
            onClickIntent.putExtra("shortcut-data", location == null ? null : location.toJson());

        PendingIntent clickPendingIntent =
                PendingIntent.getActivity(context, location.hashCode(), onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (updateViews != null)
            updateViews.setOnClickPendingIntent(R.id.widget, clickPendingIntent);
    }

    private static void setOnSettingsClickIntent(Context context, RemoteViews updateViews, LocationData location, int appWidgetId) {
        // When user clicks on widget, launch to Config activity
        Intent onClickIntent = new Intent(context.getApplicationContext(), WeatherWidgetConfigActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        if (Settings.getHomeData().equals(location)) {
            onClickIntent.putExtra(EXTRA_LOCATIONQUERY, "GPS");
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
        new AsyncTaskEx<Void, InterruptedException>().await(new CallableEx<Void, InterruptedException>() {
            @Override
            public Void call() throws InterruptedException {
                if (isCtsCancelRequested()) throw new InterruptedException();

                if (!Settings.useFollowGPS() && WidgetUtils.isGPS(appWidgetId))
                    return null;

                // Get weather data from cache
                Weather weather = WidgetUtils.getWeatherData(appWidgetId);

                if (isCtsCancelRequested()) throw new InterruptedException();

                if (weather == null) {
                    LocationData locData = null;
                    if (WidgetUtils.isGPS(appWidgetId))
                        locData = Settings.getLastGPSLocData();
                    else
                        locData = WidgetUtils.getLocationData(appWidgetId);

                    if (isCtsCancelRequested()) throw new InterruptedException();

                    if (locData != null) {
                        if (isCtsCancelRequested()) throw new InterruptedException();

                        WeatherDataLoader wloader = new WeatherDataLoader(locData);
                        try {
                            wloader.loadWeatherData(false);
                            weather = wloader.getWeather();
                        } catch (Exception e) {
                            weather = null;
                        }
                        if (weather != null)
                            WidgetUtils.saveWeatherData(appWidgetId, weather);
                        else
                            return null;
                    } else {
                        Logger.writeLine(Log.DEBUG, "%s: provider: %s; widgetId: %d; Unable to find location data", TAG, provider.getClassName(), appWidgetId);
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
                        final String backgroundURI = wm.getWeatherBackgroundURI(weather);
                        loadBackgroundImage(updateViews, appWidgetId, backgroundURI, cellWidth, cellHeight);
                    }
                }

                // Set sizes for views
                updateViewSizes(updateViews, provider, newOptions);

                if (isForecastWidget(provider.getWidgetType())) {
                    // Determine forecast size
                    int forecastLength = getForecastLength(provider.getWidgetType(), cellWidth);
                    if (weather.getForecast().length < forecastLength)
                        forecastLength = weather.getForecast().length;

                    WeatherNowViewModel viewModel = new WeatherNowViewModel(weather);
                    updateViews.removeAllViews(R.id.forecast_layout);
                    buildForecastPanel(updateViews, provider, viewModel, appWidgetId, forecastLength, newOptions);
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

    private void buildForecast(WeatherWidgetProvider provider, WeatherNowViewModel weather, int appWidgetId) {
        buildForecast(provider, weather, new int[]{appWidgetId});
    }

    private void buildForecast(WeatherWidgetProvider provider, WeatherNowViewModel weather, int[] appWidgetIds) {
        if (weather == null)
            return;

        for (int i = 0; i < appWidgetIds.length; i++) {
            RemoteViews updateViews = new RemoteViews(mContext.getPackageName(), provider.getWidgetLayoutId());
            updateViews.removeAllViews(R.id.forecast_layout);

            Bundle newOptions = mAppWidgetManager.getAppWidgetOptions(appWidgetIds[i]);

            // Widget dimensions
            int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            int cellWidth = getCellsForSize(minWidth);

            // Determine forecast size
            int forecastLength = getForecastLength(provider.getWidgetType(), cellWidth);
            if (weather.getForecasts().size() < forecastLength)
                forecastLength = weather.getForecasts().size();

            if (provider.getWidgetType() == WidgetType.Widget4x2 || provider.getWidgetType() == WidgetType.Widget2x2 || provider.getWidgetType() == WidgetType.Widget4x1Google) {
                WidgetUtils.WidgetBackground background = WidgetUtils.getWidgetBackground(appWidgetIds[i]);
                int textColor = getTextColor(background);

                updateViews.setTextColor(R.id.clock_panel, textColor);
                updateViews.setTextColor(R.id.date_panel, textColor);
            }

            buildForecastPanel(updateViews, provider, weather, appWidgetIds[i], forecastLength, newOptions);
            mAppWidgetManager.partiallyUpdateAppWidget(appWidgetIds[i], updateViews);
        }
    }

    private void buildForecastPanel(
            RemoteViews updateViews, WeatherWidgetProvider provider, WeatherNowViewModel weather, int appWidgetId,
            int forecastLength, Bundle newOptions) {
        if (weather == null)
            return;

        if (provider.getWidgetType() == WidgetType.Widget4x1 || provider.getWidgetType() == WidgetType.Widget4x2) {
            // Background & Text Color
            WidgetUtils.WidgetBackground background = WidgetUtils.getWidgetBackground(appWidgetId);
            WidgetUtils.WidgetBackgroundStyle style = WidgetUtils.getBackgroundStyle(appWidgetId);
            int textColor = getPanelTextColor(background, style, isNightMode);
            int tempTextSize = 72;

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

            for (int i = 0; i < forecastLength; i++) {
                ForecastItemViewModel forecast = weather.getForecasts().get(i);

                RemoteViews forecastPanel;
                if (provider.getWidgetType() == WidgetType.Widget4x1) {
                    forecastPanel = new RemoteViews(mContext.getPackageName(), R.layout.app_widget_forecast_panel_4x1);

                    int size = (int) ActivityUtils.dpToPx(mContext, 40);
                    if (!(!isSmallWidth || cellWidth > 4)) {
                        size *= 0.875f; // 35dp
                    }
                    forecastPanel.setInt(R.id.forecast_icon, "setMaxWidth", size);
                    forecastPanel.setInt(R.id.forecast_icon, "setMaxHeight", size);
                } else {
                    forecastPanel = new RemoteViews(mContext.getPackageName(), R.layout.app_widget_forecast_panel_4x2);

                    int size = (int) ActivityUtils.dpToPx(mContext, 30);
                    if (cellHeight > 2 && (!isSmallWidth || cellWidth > 4)) {
                        size *= (5f / 3); // 50dp
                    }
                    forecastPanel.setInt(R.id.forecast_icon, "setMaxWidth", size);
                    forecastPanel.setInt(R.id.forecast_icon, "setMaxHeight", size);
                }

                forecastPanel.setTextViewText(R.id.forecast_date, forecast.getDate());
                forecastPanel.setTextViewText(R.id.forecast_hi, forecast.getHiTemp());
                forecastPanel.setTextViewText(R.id.forecast_lo, forecast.getLoTemp());

                forecastPanel.setTextColor(R.id.forecast_date, textColor);
                forecastPanel.setTextColor(R.id.forecast_hi, textColor);
                forecastPanel.setTextColor(R.id.divider, textColor);
                forecastPanel.setTextColor(R.id.forecast_lo, textColor);
                forecastPanel.setImageViewBitmap(R.id.forecast_icon,
                        ImageUtils.weatherIconToBitmap(this, forecast.getWeatherIcon(), tempTextSize, textColor, true));

                if (provider.getWidgetType() == WidgetType.Widget4x1) {
                    if (forceSmallHeight) {
                        forecastPanel.setViewPadding(R.id.forecast_date, 0, 0, 0, 0);
                    } else {
                        int padding = (int) ActivityUtils.dpToPx(mContext, 2);
                        forecastPanel.setViewPadding(R.id.forecast_date, padding, padding, padding, padding);
                    }

                    int textSize = 12;
                    if (cellHeight > 1 && (!isSmallWidth || cellWidth > 4))
                        textSize = 14;

                    forecastPanel.setTextViewTextSize(R.id.forecast_date, TypedValue.COMPLEX_UNIT_SP, textSize);
                    forecastPanel.setTextViewTextSize(R.id.forecast_hi, TypedValue.COMPLEX_UNIT_SP, textSize);
                    forecastPanel.setTextViewTextSize(R.id.forecast_lo, TypedValue.COMPLEX_UNIT_SP, textSize);
                } else {
                    int textSize = 12;
                    if ((!isSmallHeight && cellHeight > 2) && (!isSmallWidth || (cellWidth > 4)))
                        textSize = 14;
                    else if (forceSmallHeight || isSmallHeight && cellHeight <= 2)
                        textSize = 10;

                    forecastPanel.setTextViewTextSize(R.id.forecast_date, TypedValue.COMPLEX_UNIT_SP, textSize);
                    forecastPanel.setTextViewTextSize(R.id.forecast_hi, TypedValue.COMPLEX_UNIT_SP, textSize);
                    forecastPanel.setTextViewTextSize(R.id.divider, TypedValue.COMPLEX_UNIT_SP, textSize);
                    forecastPanel.setTextViewTextSize(R.id.forecast_lo, TypedValue.COMPLEX_UNIT_SP, textSize);
                }

                updateViews.addView(R.id.forecast_layout, forecastPanel);
            }
        }
    }

    /**
     * Returns number of cells needed for given size of the widget.
     *
     * @param size Widget size in dp.
     * @return Size in number of cells.
     */
    private static int getCellsForSize(int size) {
        // The hardwired sizes in this function come from the hardwired formula found in
        // Android's UI guidelines for widget design:
        // http://developer.android.com/guide/practices/ui_guidelines/widget_design.html
        return (size + 30) / 70;
    }

    private static int getForecastLength(WidgetType widgetType, int cellWidth) {
        if (widgetType != WidgetType.Widget4x1 && widgetType != WidgetType.Widget4x2)
            return 0;

        int forecastLength = (widgetType == WidgetType.Widget4x2) ? WIDE_FORECAST_LENGTH : FORECAST_LENGTH;

        if (cellWidth < 2) {
            if (widgetType == WidgetType.Widget4x1)
                forecastLength = 0;
            else // 4x2
                forecastLength = 1;
        } else if (cellWidth == 2) {
            if (widgetType == WidgetType.Widget4x1)
                forecastLength = 1;
            else // 4x2
                forecastLength = FORECAST_LENGTH;
        } else if (cellWidth == 3) {
            if (widgetType == WidgetType.Widget4x1)
                forecastLength = 2;
            else // 4x2
                forecastLength = MEDIUM_FORECAST_LENGTH;
        }

        return forecastLength;
    }

    private String getUpdateTimeText(LocalDateTime now, boolean shortFormat) {
        String timeformat = now.format(DateTimeFormatter.ofPattern("h:mm a")).toLowerCase();

        if (DateFormat.is24HourFormat(App.getInstance().getAppContext()))
            timeformat = now.format(DateTimeFormatter.ofPattern("HH:mm")).toLowerCase();

        String updatetime = String.format("%s %s", now.format(DateTimeFormatter.ofPattern("eee")), timeformat);

        if (shortFormat)
            return updatetime;
        else
            return String.format("%s %s", mContext.getString(R.string.widget_updateprefix), updatetime);
    }

    private Weather getWeather() {
        return getWeather(true);
    }

    private Weather getWeather(final boolean refreshWeather) {
        return new AsyncTask<Weather>().await(new Callable<Weather>() {
            @Override
            public Weather call() throws Exception {
                Weather weather = null;

                try {
                    if (Settings.useFollowGPS())
                        updateLocation();

                    if (isCtsCancelRequested()) throw new InterruptedException();

                    WeatherDataLoader wloader = new WeatherDataLoader(Settings.getHomeData());
                    if (refreshWeather)
                        wloader.loadWeatherData(false);
                    else
                        wloader.forceLoadSavedWeatherData();

                    weather = wloader.getWeather();

                    if (refreshWeather && weather != null) {
                        // Re-schedule alarm at selected interval from now
                        updateAlarm(App.getInstance().getAppContext());
                        Settings.setUpdateTime(LocalDateTime.now());
                    }
                } catch (InterruptedException cancelEx) {
                    Logger.writeLine(Log.ERROR, cancelEx, "%s: GetWeather cancelled", TAG);
                    return null;
                } catch (Exception ex) {
                    Logger.writeLine(Log.ERROR, ex, "%s: GetWeather error", TAG);
                    return null;
                }

                return weather;
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        final int currentNightMode = newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        isNightMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private boolean updateLocation() {
        return new AsyncTask<Boolean>().await(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                boolean locationChanged = false;

                if (Settings.useFollowGPS()) {
                    if (ContextCompat.checkSelfPermission(App.getInstance().getAppContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(App.getInstance().getAppContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return false;
                    }

                    Location location = null;

                    if (WearableHelper.isGooglePlayServicesInstalled()) {
                        location = new AsyncTask<Location>().await(new Callable<Location>() {
                            @SuppressLint("MissingPermission")
                            @Override
                            public Location call() throws Exception {
                                Location result = null;
                                try {
                                    result = Tasks.await(mFusedLocationClient.getLastLocation(), 5, TimeUnit.SECONDS);
                                } catch (TimeoutException e) {
                                    Logger.writeLine(Log.ERROR, e);
                                }
                                return result;
                            }
                        });
                    } else {
                        LocationManager locMan = (LocationManager) App.getInstance().getAppContext().getSystemService(Context.LOCATION_SERVICE);
                        boolean isGPSEnabled = false;
                        boolean isNetEnabled = false;
                        if (locMan != null) {
                            isGPSEnabled = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER);
                            isNetEnabled = locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                        }

                        if (isGPSEnabled || isNetEnabled && !isCtsCancelRequested()) {
                            Criteria locCriteria = new Criteria();
                            locCriteria.setAccuracy(Criteria.ACCURACY_COARSE);
                            locCriteria.setCostAllowed(false);
                            locCriteria.setPowerRequirement(Criteria.POWER_LOW);
                            String provider = locMan.getBestProvider(locCriteria, true);
                            location = locMan.getLastKnownLocation(provider);
                        }
                    }

                    if (location != null && !isCtsCancelRequested()) {
                        LocationData lastGPSLocData = Settings.getLastGPSLocData();

                        if (isCtsCancelRequested()) return locationChanged;

                        // Check previous location difference
                        if (lastGPSLocData.getQuery() != null &&
                                Math.abs(ConversionMethods.calculateHaversine(lastGPSLocData.getLatitude(), lastGPSLocData.getLongitude(),
                                        location.getLatitude(), location.getLongitude())) < 1600) {
                            return false;
                        }

                        LocationQueryViewModel query_vm = null;

                        TaskCompletionSource<LocationQueryViewModel> tcs = new TaskCompletionSource<>(cts.getToken());
                        tcs.setResult(wm.getLocation(location));
                        try {
                            query_vm = Tasks.await(tcs.getTask());
                        } catch (ExecutionException e) {
                            query_vm = new LocationQueryViewModel();
                            Logger.writeLine(Log.ERROR, e.getCause());
                        } catch (InterruptedException e) {
                            return locationChanged;
                        }

                        if (StringUtils.isNullOrEmpty(query_vm.getLocationQuery()))
                            query_vm = new LocationQueryViewModel();

                        if (StringUtils.isNullOrWhitespace(query_vm.getLocationQuery())) {
                            // Stop since there is no valid query
                            return false;
                        }

                        if (isCtsCancelRequested()) return locationChanged;

                        // Save oldkey
                        String oldkey = lastGPSLocData.getQuery();

                        // Save location as last known
                        lastGPSLocData.setData(query_vm, location);
                        Settings.saveLastGPSLocData(lastGPSLocData);

                        WearableDataListenerService.enqueueWork(App.getInstance().getAppContext(),
                                new Intent(App.getInstance().getAppContext(), WearableDataListenerService.class)
                                        .setAction(WearableDataListenerService.ACTION_SENDLOCATIONUPDATE));

                        locationChanged = true;
                    }
                }

                return locationChanged;
            }
        });
    }
}