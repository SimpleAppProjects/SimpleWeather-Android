package com.thewizrd.shared_resources.weatherdata.aqicn;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;

import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.keys.Keys;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.weatherdata.AirQuality;
import com.thewizrd.shared_resources.weatherdata.AirQualityProviderInterface;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AQICNProvider implements AirQualityProviderInterface {
    @Override
    public AirQuality getAirQualityData(LocationData location) throws WeatherException {
        AirQuality aqiData = null;

        String queryAPI = null;
        URL weatherURL = null;
        HttpURLConnection client = null;

        String key = Keys.getAQICNKey();
        if (StringUtils.isNullOrWhitespace(key))
            return null;

        try {
            queryAPI = "https://api.waqi.info/feed/geo:%s;%s/?token=%s";
            weatherURL = new URL(String.format(queryAPI, location.getLatitude(), location.getLongitude(), key));

            InputStream stream = null;

            Context context = SimpleLibrary.getInstance().getApp().getAppContext();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("v%s", packageInfo.versionName);

            client = (HttpURLConnection) weatherURL.openConnection();
            client.setConnectTimeout(Settings.CONNECTION_TIMEOUT);
            client.setReadTimeout(Settings.READ_TIMEOUT);
            client.addRequestProperty("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version));
            stream = client.getInputStream();

            // Load data
            Rootobject root = JSONParser.deserializer(stream, Rootobject.class);

            aqiData = new AirQuality(root);

            // End Stream
            stream.close();
        } catch (Exception ex) {
            aqiData = null;
            Logger.writeLine(Log.ERROR, ex, "AQICNProvider: error getting air quality data");
        } finally {
            if (client != null)
                client.disconnect();
        }

        return aqiData;
    }
}
