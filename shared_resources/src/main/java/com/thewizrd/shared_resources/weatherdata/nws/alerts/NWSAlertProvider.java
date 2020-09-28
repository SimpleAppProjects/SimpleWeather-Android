package com.thewizrd.shared_resources.weatherdata.nws.alerts;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.util.Log;

import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.okhttp3.OkHttp3Utils;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.weatherdata.WeatherAlert;
import com.thewizrd.shared_resources.weatherdata.WeatherAlertProviderInterface;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class NWSAlertProvider implements WeatherAlertProviderInterface {
    private static final String ALERT_QUERY_URL = "https://api.weather.gov/alerts/active?status=actual&message_type=alert&point=%s,%s";

    @Override
    public List<WeatherAlert> getAlerts(LocationData location) {
        List<WeatherAlert> alerts;

        OkHttpClient client = SimpleLibrary.getInstance().getHttpClient();
        Response response = null;

        try {
            Context context = SimpleLibrary.getInstance().getApp().getAppContext();
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = String.format("v%s", packageInfo.versionName);

            DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ROOT);
            df.applyPattern("0.####");

            Request request = new Request.Builder()
                    .url(String.format(Locale.ROOT, ALERT_QUERY_URL, df.format(location.getLatitude()), df.format(location.getLongitude())))
                    .addHeader("Accept", "application/ld+json")
                    .addHeader("User-Agent", String.format("SimpleWeather (thewizrd.dev@gmail.com) %s", version))
                    .build();

            // Connect to webstream
            response = client.newBuilder()
                    // Extend timeout to 15s
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()
                    .newCall(request).execute();
            final InputStream stream = OkHttp3Utils.getStream(response);

            // Load data
            AlertRootobject root = JSONParser.deserializer(stream, AlertRootobject.class);

            alerts = new ArrayList<>(root.getGraph().size());

            for (GraphItem result : root.getGraph()) {
                alerts.add(new WeatherAlert(result));
            }

            // End Stream
            stream.close();
        } catch (Exception ex) {
            alerts = new ArrayList<>();
            Logger.writeLine(Log.ERROR, ex, "NWSAlertProvider: error getting weather alert data");
        } finally {
            if (response != null)
                response.close();
        }

        return alerts;
    }
}
