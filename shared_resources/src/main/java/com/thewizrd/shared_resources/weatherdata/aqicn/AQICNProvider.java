package com.thewizrd.shared_resources.weatherdata.aqicn;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;

import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.keys.Keys;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.weatherdata.AirQuality;
import com.thewizrd.shared_resources.weatherdata.AirQualityProviderInterface;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AQICNProvider implements AirQualityProviderInterface {
    private static final String QUERY_URL = "https://api.waqi.info/feed/geo:%s;%s/?token=%s";

    @Override
    public AirQuality getAirQualityData(LocationData location) throws WeatherException {
        AirQuality aqiData = null;

        String key = Keys.getAQICNKey();
        if (StringUtils.isNullOrWhitespace(key))
            return null;

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response response = null;

        try {
            Context context = SimpleLibrary.getInstance().getApp().getAppContext();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("v%s", packageInfo.versionName);

            DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ROOT);
            df.applyPattern("0.####");

            Request request = new Request.Builder()
                    .url(String.format(Locale.ROOT, QUERY_URL, df.format(location.getLatitude()), df.format(location.getLongitude()), key))
                    .addHeader("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version))
                    .build();

            // Connect to webstream
            response = client.newCall(request).execute();
            final InputStream stream = response.body().byteStream();

            // Load data
            Rootobject root = JSONParser.deserializer(stream, Rootobject.class);

            aqiData = new AirQuality(root);

            // End Stream
            stream.close();
        } catch (Exception ex) {
            aqiData = null;
            Logger.writeLine(Log.ERROR, ex, "AQICNProvider: error getting air quality data");
        } finally {
            if (response != null)
                response.close();
        }

        return aqiData;
    }
}
