package com.thewizrd.weather_api.here.auth;

import com.squareup.moshi.JsonClass;
import com.thewizrd.shared_resources.utils.DateTimeUtils;

import java.time.ZonedDateTime;

@JsonClass(generateAdapter = true)
class Token {
    private String access_token;
    private String expiration_date;

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String token) {
        access_token = token;
    }

    public ZonedDateTime getExpirationDate() {
        ZonedDateTime dateTime = null;

        try {
            dateTime = ZonedDateTime.parse(expiration_date, DateTimeUtils.getZonedDateTimeFormatter());
        } catch (Exception ignored) {
        }

        if (dateTime == null)
            dateTime = ZonedDateTime.parse(expiration_date);

        return dateTime;
    }

    public void setExpirationDate(ZonedDateTime date) {
        expiration_date = date.format(DateTimeUtils.getZonedDateTimeFormatter());
    }

    String getExpiration_date() {
        return expiration_date;
    }

    void setExpiration_date(String expiration_date) {
        this.expiration_date = expiration_date;
    }
}
