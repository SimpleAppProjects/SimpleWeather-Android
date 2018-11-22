package com.thewizrd.simpleweather.weatheralerts;

import com.thewizrd.shared_resources.AppState;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.notifications.WeatherAlertNotificationBuilder;

import org.threeten.bp.ZonedDateTime;

import java.util.ArrayList;
import java.util.List;

public class WeatherAlertHandler {
    public static void postAlerts(LocationData location, List<WeatherAlert> alerts) {
        WeatherManager wm = WeatherManager.getInstance();

        if (wm.supportsAlerts() && alerts != null && alerts.size() > 0) {
            // Only alert if we're in the background
            if (App.getInstance().getAppState() != AppState.FOREGROUND) {
                // Check if any of these alerts have been posted before
                // or are past the expiration date
                List<WeatherAlert> unotifiedAlerts = new ArrayList<>();
                for (WeatherAlert alert : alerts) {
                    if (!alert.isNotified() && alert.getExpiresDate().compareTo(ZonedDateTime.now()) > 0)
                        unotifiedAlerts.add(alert);
                }
                //var unotifiedAlerts = alerts.Where(alert => alert.Notified == false && alert.ExpiresDate > DateTimeOffset.Now);

                // Post any un-notified alerts
                WeatherAlertNotificationBuilder.createNotifications(location, unotifiedAlerts);

                setAsNotified(location, alerts);
            }
        }
    }

    public static void setAsNotified(LocationData location, List<WeatherAlert> alerts) {
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
