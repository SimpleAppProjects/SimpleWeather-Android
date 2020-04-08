package com.thewizrd.shared_resources.tzdb;

import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.thewizrd.shared_resources.FirebaseHelper;
import com.thewizrd.shared_resources.keys.Keys;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;

import java.io.InputStream;
import java.net.URL;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

public class TimeZoneProvider implements TimeZoneProviderInterface {
    private class TimeZoneData {
        @SerializedName("tz_long")
        private String tzLong;

        public String getTzLong() {
            return tzLong;
        }

        public void setTzLong(String tzLong) {
            this.tzLong = tzLong;
        }
    }

    @Override
    public String getTimeZone(double latitude, final double longitude) {
        String tzLong = null;
        URL weatherURL = null;
        HttpsURLConnection client = null;

        // Get Firebase token
        final String userToken = FirebaseHelper.getAccessToken();

        final String tzAPI = Keys.getTimeZoneAPI();
        if (StringUtils.isNullOrWhitespace(tzAPI) || StringUtils.isNullOrWhitespace(userToken))
            return null;

        try {
            weatherURL = new URL(String.format(Locale.ROOT, "%s?lat=%s&lon=%s", tzAPI, latitude, longitude));

            InputStream stream = null;

            // Build request
            client = (HttpsURLConnection) weatherURL.openConnection();
            client.setConnectTimeout(Settings.CONNECTION_TIMEOUT);
            client.setReadTimeout(Settings.READ_TIMEOUT);
            client.addRequestProperty("Authorization", String.format(Locale.ROOT, "Bearer %s", userToken));
            stream = client.getInputStream();

            // Load data
            TimeZoneData root = JSONParser.deserializer(stream, TimeZoneData.class);

            tzLong = root.getTzLong();

            // End Stream
            stream.close();
        } catch (Exception ex) {
            tzLong = null;
            Logger.writeLine(Log.ERROR, ex, "TimeZoneProvider: error time zone");
        } finally {
            if (client != null)
                client.disconnect();
        }

        return tzLong;
    }
}
