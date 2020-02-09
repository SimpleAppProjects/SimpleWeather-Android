package com.thewizrd.shared_resources.utils.here;

import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

class Token {
    private String access_token;
    private String expiration_date;

    public String getAccessToken() {
        return access_token;
    }

    public void setAccessToken(String token) {
        access_token = token;
    }

    public ZonedDateTime getExpirationDate() {
        ZonedDateTime dateTime = null;

        try {
            dateTime = ZonedDateTime.parse(expiration_date, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss ZZZZZ"));
        } catch (Exception ignored) {
        }

        if (dateTime == null)
            dateTime = ZonedDateTime.parse(expiration_date);

        return dateTime;
    }

    public void setExpirationDate(ZonedDateTime date) {
        expiration_date = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss ZZZZZ"));
    }
}
