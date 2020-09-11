package com.thewizrd.simpleweather.shortcuts;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.utils.ImageUtils;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.main.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ShortcutCreatorWorker extends Worker {
    private static String TAG = "ShortcutCreatorWorker";

    private final Context mContext;

    public ShortcutCreatorWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context.getApplicationContext();
    }

    public static void requestUpdateShortcuts(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            context = context.getApplicationContext();

            Logger.writeLine(Log.INFO, "%s: Requesting work", TAG);

            // Set a delay of 1 minute to allow the loaders to refresh the weather before this starts
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(ShortcutCreatorWorker.class)
                    .setInitialDelay(1, TimeUnit.MINUTES)
                    .build();

            WorkManager.getInstance(context)
                    .enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, workRequest);
        }
    }

    public static void removeShortcuts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Context context = App.getInstance().getAppContext();
            ShortcutManager shortcutMan = context.getSystemService(ShortcutManager.class);
            shortcutMan.removeAllDynamicShortcuts();

            Logger.writeLine(Log.INFO, "%s: Shortcuts removed", TAG);
        }
    }

    @NonNull
    @Override
    @TargetApi(Build.VERSION_CODES.N_MR1)
    public Result doWork() {
        List<LocationData> locations = new ArrayList<>(Settings.getLocationData());
        if (Settings.useFollowGPS())
            locations.add(0, Settings.getHomeData());

        int MAX_SHORTCUTS = 4;
        if (locations.size() < MAX_SHORTCUTS)
            MAX_SHORTCUTS = locations.size();

        ShortcutManager shortcutMan = mContext.getSystemService(ShortcutManager.class);
        List<ShortcutInfo> shortcuts = new ArrayList<>(MAX_SHORTCUTS);

        shortcutMan.removeAllDynamicShortcuts();

        for (int i = 0; i < MAX_SHORTCUTS; i++) {
            LocationData location = locations.get(i);
            final Weather weather = Settings.getWeatherData(location.getQuery());
            boolean any = false;

            for (ShortcutInfo s : shortcuts) {
                if (s.getId().equals(location.getQuery())) {
                    any = true;
                    break;
                }
            }

            if (weather == null || !weather.isValid() || any) {
                locations.remove(i);
                i--;
                if (locations.size() < MAX_SHORTCUTS)
                    MAX_SHORTCUTS = locations.size();
                continue;
            }

            // Start WeatherNow Activity with weather data
            Intent intent = new Intent(mContext, MainActivity.class)
                    .setAction(Intent.ACTION_MAIN)
                    .putExtra(Constants.KEY_DATA, JSONParser.serializer(location, LocationData.class))
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);

            Bitmap bmp = AsyncTask.await(new Callable<Bitmap>() {
                @Override
                public Bitmap call() {
                    return ImageUtils.tintedBitmapFromDrawable(mContext, WeatherUtils.getWeatherIconResource(weather.getCondition().getIcon()),
                            mContext.getColor(R.color.colorPrimaryDark));
                }
            });
            ShortcutInfo shortcut = new ShortcutInfo.Builder(mContext, location.getQuery())
                    .setShortLabel(weather.getLocation().getName())
                    .setIcon(Icon.createWithBitmap(bmp))
                    .setIntent(intent)
                    .build();

            shortcuts.add(shortcut);
        }

        shortcutMan.setDynamicShortcuts(shortcuts);

        Logger.writeLine(Log.INFO, "%s: Shortcuts updated", TAG);

        return Result.success();
    }
}
