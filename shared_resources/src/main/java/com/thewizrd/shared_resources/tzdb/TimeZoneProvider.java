package com.thewizrd.shared_resources.tzdb;

import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.firebase.FirebaseHelper;
import com.thewizrd.shared_resources.keys.Keys;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;

import java.io.InputStream;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
        // Get Firebase token
        final String userToken = FirebaseHelper.getAccessToken();

        final String tzAPI = Keys.getTimeZoneAPI();
        if (StringUtils.isNullOrWhitespace(tzAPI) || StringUtils.isNullOrWhitespace(userToken))
            return null;

        String tzLong = null;
        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response response = null;

        try {
            Request request = new Request.Builder()
                    .url(String.format(Locale.ROOT, "%s?lat=%s&lon=%s", tzAPI, latitude, longitude))
                    .addHeader("Authorization", String.format(Locale.ROOT, "Bearer %s", userToken))
                    .build();

            // Connect to webstream
            response = client.newCall(request).execute();
            final InputStream stream = response.body().byteStream();

            // Load data
            TimeZoneData root = JSONParser.deserializer(stream, TimeZoneData.class);

            tzLong = root.getTzLong();

            // End Stream
            stream.close();
        } catch (Exception ex) {
            tzLong = null;
            Logger.writeLine(Log.ERROR, ex, "TimeZoneProvider: error time zone");
        } finally {
            if (response != null)
                response.close();
        }

        return tzLong;
    }
}
