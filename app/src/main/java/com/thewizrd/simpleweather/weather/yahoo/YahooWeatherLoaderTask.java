package com.thewizrd.simpleweather.weather.yahoo;

import android.os.AsyncTask;

import com.thewizrd.simpleweather.utils.JSONParser;
import com.thewizrd.simpleweather.utils.Settings;
import com.thewizrd.simpleweather.weather.yahoo.data.Rootobject;
import com.thewizrd.simpleweather.weather.yahoo.data.YahooWeather;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class YahooWeatherLoaderTask extends AsyncTask<String, Void, YahooWeather> {

    private YahooWeather weather = null;

    @Override
    protected YahooWeather doInBackground(String... params) {

        String yahooAPI = "https://query.yahooapis.com/v1/public/yql?q=";
        String query = "select * from weather.forecast where woeid in (select woeid from geo.places(1) where text=\""
                + params[0] + "\") and u='" + Settings.getTempUnit() + "'&format=json";
        int counter = 0;

        do {
            try {
                URL queryURL = new URL(yahooAPI + query);
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

            // If we can't load data, delay and try again
            if (weather == null)
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            counter++;
        } while (weather == null && counter < 10);

        return weather;
    }

    private YahooWeather parseWeather(String json) {
        Rootobject root = null;

        try {
            root = (Rootobject) JSONParser.deserializer(json, Rootobject.class);

            // Load weather
            weather = new YahooWeather(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return weather;
    }
}