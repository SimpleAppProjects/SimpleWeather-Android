package com.thewizrd.simpleweather.weather.weatherunderground;

import android.os.AsyncTask;

import com.thewizrd.simpleweather.utils.JSONParser;
import com.thewizrd.simpleweather.utils.Settings;
import com.thewizrd.simpleweather.weather.weatherunderground.data.Rootobject;
import com.thewizrd.simpleweather.weather.weatherunderground.data.WUWeather;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class WUDataLoaderTask extends AsyncTask<String, Void, WUWeather> {

    private WUWeather weather = null;

    @Override
    protected WUWeather doInBackground(String... params) {
        String queryAPI = "http://api.wunderground.com/api/" + Settings.getAPIKEY()
                + "/astronomy/conditions/forecast10day";
        String options = ".json";
        int counter = 0;

        do {
            try {
                URL queryURL = new URL(queryAPI + params[0] + options);
                URLConnection client = queryURL.openConnection();
                InputStream stream = client.getInputStream();

                // Read to buffer
                ByteArrayOutputStream buffStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = stream.read(buffer)) != -1) {
                    buffStream.write(buffer, 0, length);
                }

                // Load data
                String response = buffStream.toString("UTF-8");
                weather = parseWeather(response);

                // Close
                buffStream.close();
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
            }

            if (weather == null)
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            counter++;
        } while (weather == null && counter < 5);

        return weather;
    }

    private WUWeather parseWeather(String json)
    {
        Rootobject root = null;

        try {
            root = (Rootobject) JSONParser.deserializer(json, Rootobject.class);

            // Load weather
            weather = new WUWeather(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return weather;
    }
}
