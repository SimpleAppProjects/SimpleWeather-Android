package com.thewizrd.shared_resources.controls;

import android.content.Context;

import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.DateTimeUtils;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.shared_resources.weatherdata.WeatherAlertSeverity;
import com.thewizrd.shared_resources.weatherdata.WeatherAlertType;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

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
            postDate = context.getString(R.string.datetime_hr_ago, (int) Math.floor(sincePost.toHours()));
        else if (sincePost.toMinutes() >= 1)
            postDate = context.getString(R.string.datetime_min_ago, (int) Math.floor(sincePost.toMinutes()));
        else
            postDate = context.getString(R.string.datetime_sec_ago, (int) Math.floor(sincePost.getSeconds()));

        // Displays Thursday, April 10, 2008 6:30 AM
        expireDate = String.format("%s %s %s",
                context.getString(R.string.datetime_validuntil),
                weatherAlert.getExpiresDate().format(
                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT)
                                .withLocale(LocaleUtils.getLocale())),
                weatherAlert.getExpiresDate().format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.TIMEZONE_NAME)));

        attribution = weatherAlert.getAttribution();

        if (attribution != null) {
            attribution = String.format("%s %s", context.getString(R.string.credit_prefix), attribution);
        }
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

    public String getAlertBodyMessage() {
        return String.format("%s\n\n%s\n\n%s", getExpireDate(), getMessage(), getAttribution());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WeatherAlertViewModel that = (WeatherAlertViewModel) o;

        if (getAlertType() != that.getAlertType()) return false;
        if (getAlertSeverity() != that.getAlertSeverity()) return false;
        if (getTitle() != null ? !getTitle().equals(that.getTitle()) : that.getTitle() != null)
            return false;
        if (getMessage() != null ? !getMessage().equals(that.getMessage()) : that.getMessage() != null)
            return false;
        if (getPostDate() != null ? !getPostDate().equals(that.getPostDate()) : that.getPostDate() != null)
            return false;
        if (getExpireDate() != null ? !getExpireDate().equals(that.getExpireDate()) : that.getExpireDate() != null)
            return false;
        return getAttribution() != null ? getAttribution().equals(that.getAttribution()) : that.getAttribution() == null;
    }

    @Override
    public int hashCode() {
        int result = getAlertType() != null ? getAlertType().hashCode() : 0;
        result = 31 * result + (getAlertSeverity() != null ? getAlertSeverity().hashCode() : 0);
        result = 31 * result + (getTitle() != null ? getTitle().hashCode() : 0);
        result = 31 * result + (getMessage() != null ? getMessage().hashCode() : 0);
        result = 31 * result + (getPostDate() != null ? getPostDate().hashCode() : 0);
        result = 31 * result + (getExpireDate() != null ? getExpireDate().hashCode() : 0);
        result = 31 * result + (getAttribution() != null ? getAttribution().hashCode() : 0);
        return result;
    }
}
