package com.thewizrd.simpleweather.shortcuts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;

import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.utils.ImageUtils;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.MainActivity;
import com.thewizrd.simpleweather.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ShortcutCreator {
    public static void updateShortcuts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    final Context context = App.getInstance().getAppContext();

                    final WeatherManager wm = WeatherManager.getInstance();

                    List<LocationData> locations = new ArrayList<>(Settings.getLocationData());
                    if (Settings.useFollowGPS())
                        locations.add(0, Settings.getHomeData());

                    ShortcutManager shortcutMan = context.getSystemService(ShortcutManager.class);
                    List<ShortcutInfo> shortcuts = new ArrayList<>();

                    shortcutMan.removeAllDynamicShortcuts();

                    int MAX_SHORTCUTS = 4;
                    if (locations.size() < MAX_SHORTCUTS)
                        MAX_SHORTCUTS = locations.size();

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
                        Intent intent = new Intent(context, MainActivity.class)
                                .setAction(Intent.ACTION_MAIN)
                                .putExtra("shortcut-data", location.toJson())
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);

                        Bitmap bmp = new AsyncTask<Bitmap>().await(new Callable<Bitmap>() {
                            @Override
                            public Bitmap call() throws Exception {
                                return ImageUtils.tintedBitmapFromDrawable(context, wm.getWeatherIconResource(weather.getCondition().getIcon()),
                                        context.getColor(R.color.colorPrimaryDark));
                            }
                        });
                        ShortcutInfo shortcut = new ShortcutInfo.Builder(context, location.getQuery())
                                .setShortLabel(weather.getLocation().getName())
                                .setIcon(Icon.createWithBitmap(bmp))
                                .setIntent(intent)
                                .build();

                        shortcuts.add(shortcut);
                    }

                    shortcutMan.setDynamicShortcuts(shortcuts);
                }
            });
        }
    }

    public static void removeShortcuts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Context context = App.getInstance().getAppContext();
            ShortcutManager shortcutMan = context.getSystemService(ShortcutManager.class);
            shortcutMan.removeAllDynamicShortcuts();
        }
    }
}
