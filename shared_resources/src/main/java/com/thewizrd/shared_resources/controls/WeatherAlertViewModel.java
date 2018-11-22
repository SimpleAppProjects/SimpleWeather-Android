package com.thewizrd.shared_resources.controls;

import android.content.Context;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.shared_resources.weatherdata.WeatherAlertSeverity;
import com.thewizrd.shared_resources.weatherdata.WeatherAlertType;

import org.threeten.bp.Duration;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.FormatStyle;

import java.util.Locale;

public class WeatherAlertViewModel {
    private WeatherAlertType alertType;
    private WeatherAlertSeverity alertSeverity;
    private String title;
    private String message;
    private String postDate;
    private String expireDate;
    private String attribution;

    public WeatherAlertViewModel(WeatherAlert weatherAlert) {
        Context context = SimpleLibrary.getInstance().getApp().getAppContext();

        alertType = weatherAlert.getType();
        alertSeverity = weatherAlert.getSeverity();
        title = weatherAlert.getTitle();
        message = weatherAlert.getMessage();

        Duration sincePost = Duration.between(ZonedDateTime.now(ZoneOffset.UTC), weatherAlert.getDate()).abs();

        if (sincePost.toDays() >= 1)
            postDate = context.getString(R.string.datetime_day_ago, (int) Math.floor(sincePost.toDays()));
        else if (sincePost.toHours() >= 1)
            postDate = context.getString(R.string.datetime_day_ago, (int) Math.floor(sincePost.toHours()));
        else if (sincePost.toMinutes() >= 1)
            postDate = context.getString(R.string.datetime_day_ago, (int) Math.floor(sincePost.toMinutes()));
        else
            postDate = context.getString(R.string.datetime_day_ago, (int) Math.floor(sincePost.getSeconds()));

        // Displays Thursday, April 10, 2008 6:30 AM
        expireDate = String.format("%s %s %s",
                context.getString(R.string.datetime_validuntil),
                weatherAlert.getExpiresDate().format(
                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)
                                .withLocale(Locale.getDefault())),
                weatherAlert.getExpiresDate().format(DateTimeFormatter.ofPattern("z")));

        attribution = weatherAlert.getAttribution();
    }

    public WeatherAlertType getAlertType() {
        return alertType;
    }

    public void setAlertType(WeatherAlertType alertType) {
        this.alertType = alertType;
    }

    public WeatherAlertSeverity getAlertSeverity() {
        return alertSeverity;
    }

    public void setAlertSeverity(WeatherAlertSeverity alertSeverity) {
        this.alertSeverity = alertSeverity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPostDate() {
        return postDate;
    }

    public void setPostDate(String postDate) {
        this.postDate = postDate;
    }

    public String getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(String expireDate) {
        this.expireDate = expireDate;
    }

    public String getAttribution() {
        return attribution;
    }

    public void setAttribution(String attribution) {
        this.attribution = attribution;
    }
}
