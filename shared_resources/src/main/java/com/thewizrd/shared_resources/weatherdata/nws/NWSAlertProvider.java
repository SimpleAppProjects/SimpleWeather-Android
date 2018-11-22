package com.thewizrd.shared_resources.weatherdata.nws;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;

import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.shared_resources.weatherdata.WeatherAlertProviderInterface;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public final class NWSAlertProvider implements WeatherAlertProviderInterface {
    @Override
    public List<WeatherAlert> getAlerts(LocationData location) {
        List<WeatherAlert> alerts = null;

        String queryAPI = null;
        URL weatherURL = null;
        HttpURLConnection client = null;

        try {
            queryAPI = "https://api.weather.gov/alerts/active?point=%s,%s";
            weatherURL = new URL(String.format(queryAPI, location.getLatitude(), location.getLongitude()));

            InputStream stream = null;

            Context context = SimpleLibrary.getInstance().getApp().getAppContext();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("v%s", packageInfo.versionName);

            client = (HttpURLConnection) weatherURL.openConnection();
            client.setInstanceFollowRedirects(true);
            client.addRequestProperty("Accept", "application/ld+json");
            client.addRequestProperty("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version));
            if ("gzip".equals(client.getContentEncoding())) {
                stream = new GZIPInputStream(client.getInputStream());
            } else {
                stream = client.getInputStream();
            }

            // Load data
            alerts = new ArrayList<>();

            AlertRootobject root = null;
            // TODO: put in async task?
            root = (AlertRootobject) JSONParser.deserializer(stream, AlertRootobject.class);

            for (GraphItem result : root.getGraph()) {
                alerts.add(new WeatherAlert(result));
            }

            // End Stream
            stream.close();
        } catch (Exception ex) {
            alerts = new ArrayList<>();
            Logger.writeLine(Log.ERROR, ex, "NWSAlertProvider: error getting weather alert data");
        } finally {
            if (client != null)
                client.disconnect();
        }

        if (alerts == null)
            alerts = new ArrayList<>();

        return alerts;
    }
}
