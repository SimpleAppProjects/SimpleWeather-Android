package com.thewizrd.simpleweather.weather.yahoo;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.thewizrd.simpleweather.utils.JSONParser;
import com.thewizrd.simpleweather.utils.Settings;
import com.thewizrd.simpleweather.utils.WeatherException;
import com.thewizrd.simpleweather.utils.WeatherUtils;
import com.thewizrd.simpleweather.weather.yahoo.data.Rootobject;
import com.thewizrd.simpleweather.weather.yahoo.data.YahooWeather;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class YahooWeatherLoaderTask extends AsyncTask<String, Void, YahooWeather> {

    private YahooWeather weather = null;
    private WeatherException wEx = null;

    private Context context;

    public YahooWeatherLoaderTask(Context context) {
        this.context = context;
    }

    @Override
    protected void onPostExecute(YahooWeather weather) {
        if (weather == null && wEx != null) {
            Toast.makeText(context, wEx.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

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
                // Reset exception
                wEx = null;

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
            } catch (UnknownHostException uHEx) {
                weather = null;
                wEx = new WeatherException(WeatherUtils.ErrorStatus.NETWORKERROR);
            } catch (Exception e) {
                weather = null;
                e.printStackTrace();
            }

            // If we can't load data, delay and try again
            if (weather == null)
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            counter++;
        } while (weather == null && counter < 5);

        if (weather == null && wEx == null)
            wEx = new WeatherException(WeatherUtils.ErrorStatus.NOWEATHER);
        else {
            saveTimeZone();
        }

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

    private void saveTimeZone()
    {
        // Now
        Calendar utc = null;
        try {
            utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            utc.setTime(new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm:ss'Z'").parse(weather.created));
            utc.set(Calendar.SECOND, 0);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // There
        Calendar there = null;
        try {
            there = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            there.setTime(new SimpleDateFormat("EEE, dd MMM yyyy hh:mm aa").parse(weather.lastBuildDate.substring(0, weather.lastBuildDate.length() - 3)));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        long offset = there.getTimeInMillis() - utc.getTimeInMillis();
        weather.location.offset = (int) offset;
    }
}