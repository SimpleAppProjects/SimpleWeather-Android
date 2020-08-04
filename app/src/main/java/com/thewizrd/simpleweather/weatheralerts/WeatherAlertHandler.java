package com.thewizrd.simpleweather.weatheralerts;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.thewizrd.shared_resources.AppState;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.BuildConfig;
import com.thewizrd.simpleweather.notifications.WeatherAlertNotificationBuilder;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import org.threeten.bp.ZonedDateTime;

import java.util.Collection;

public class WeatherAlertHandler {
    public static void postAlerts(LocationData location, Collection<WeatherAlert> alerts) {
        WeatherManager wm = WeatherManager.getInstance();

        if (wm.supportsAlerts() && alerts != null && alerts.size() > 0) {
            // Only alert if we're in the background
            if (BuildConfig.DEBUG || App.getInstance().getAppState() != AppState.FOREGROUND) {
                // Check if any of these alerts have been posted before
                // or are past the expiration date
                Collection<WeatherAlert> unotifiedAlerts = Collections2.filter(alerts, new Predicate<WeatherAlert>() {
                    @Override
                    public boolean apply(@NullableDecl WeatherAlert input) {
                        return input != null && (BuildConfig.DEBUG || !input.isNotified() && !input.getExpiresDate().isBefore(ZonedDateTime.now()));
                    }
                });

                // Post any un-notified alerts
                WeatherAlertNotificationBuilder.createNotifications(location, unotifiedAlerts);

                setAsNotified(location, alerts);
            }
        }
    }

    public static void setAsNotified(LocationData location, Collection<WeatherAlert> alerts) {
        if (alerts != null) {
            // Update all alerts
            for (WeatherAlert alert : alerts) {
                alert.setNotified(true);
            }

            // Save alert data
            Settings.saveWeatherAlerts(location, alerts);
        }
    }
}
