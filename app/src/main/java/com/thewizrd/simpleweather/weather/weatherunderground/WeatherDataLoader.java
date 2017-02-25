package com.thewizrd.simpleweather.weather.weatherunderground;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.thewizrd.simpleweather.utils.FileUtils;
import com.thewizrd.simpleweather.utils.JSONParser;
import com.thewizrd.simpleweather.utils.Settings;
import com.thewizrd.simpleweather.weather.weatherunderground.data.Rootobject;
import com.thewizrd.simpleweather.weather.weatherunderground.data.Weather;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class WeatherDataLoader {

    OnWeatherLoadedListener mCallBack;
    //OnWeatherErrorListener mErrorBack;

    public interface OnWeatherLoadedListener {
        void onWeatherLoaded(Weather weather);
    }

    /*
    public interface OnWeatherErrorListener {
        public void onWeatherError(WeatherUtils.ErrorStatus status);
    }
    */

    private String location_query = null;
    private Weather weather = null;
    private int locationIdx = 0;
    private File filesDir = null;
    private File weatherFile = null;
    private Context mContext;

    public WeatherDataLoader(Context context, OnWeatherLoadedListener listener, String query, int idx) {
        location_query = query;
        locationIdx = idx;

        mContext = context;
        filesDir = mContext.getFilesDir();
        mCallBack = listener;
        /*
        mErrorBack = (OnWeatherErrorListener) mContext;
        */
    }

    private void getWeatherData() {
        String queryAPI = "http://api.wunderground.com/api/" + Settings.getAPIKEY()
                + "/astronomy/conditions/forecast10day";
        String options = ".json";

        try {
            URL queryURL = new URL(queryAPI + location_query + options);
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

            if (weather != null)
                saveWeatherData();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }

    private Weather parseWeather(String json)
    {
        Rootobject root = null;

        try {
            root = (Rootobject) JSONParser.deserializer(json, Rootobject.class);

            // Load weather
            weather = new Weather(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return weather;
    }

    public void loadWeatherData(final boolean forceRefresh) throws IOException {
        if (weatherFile == null) {
            weatherFile = new File(filesDir, "weather" + locationIdx + ".json");

            if (!weatherFile.exists() && !weatherFile.createNewFile())
                throw new IOException("Unable to create locations file");
        }

        new Thread() {
            @Override
            public void run() {
                if (forceRefresh) {
                    getWeatherData();
                }
                else {
                    try {
                        loadWeatherData();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mCallBack.onWeatherLoaded(weather);
                    }
                });
            }
        }.start();
    }

    private void loadWeatherData() throws IOException {
        if (weatherFile == null) {
            weatherFile = new File(filesDir, "weather" + locationIdx + ".json");

            if (!weatherFile.exists() && !weatherFile.createNewFile())
                throw new IOException("Unable to create locations file");
        }

        boolean gotData = loadSavedWeatherData(weatherFile);

        if (!gotData) {
            getWeatherData();
        }
        else
            return;
    }

    private boolean loadSavedWeatherData(File file, boolean _override) {
        if (_override) {
            if (!file.exists() || file.length() == 0)
                return false;

            try {
                String weatherJson = FileUtils.readFile(file);
                weather = (Weather) JSONParser.deserializer(weatherJson, Weather.class);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return weather != null;

        }
        else
            return loadSavedWeatherData(file);
    }

    private boolean loadSavedWeatherData(File file) {
        if (!file.exists() || file.length() == 0)
            return false;

        try {
            String weatherJson = FileUtils.readFile(file);
            weather = (Weather) JSONParser.deserializer(weatherJson, Weather.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (weather == null)
            return false;

        // Weather data expiration
        int ttl = 60;

        Date updateTime = weather.update_time;
        Date now = Calendar.getInstance().getTime();
        long span = now.getTime() - updateTime.getTime();
        TimeUnit inMins = TimeUnit.MINUTES;
        long minSpan = inMins.convert(span, TimeUnit.MILLISECONDS);

        // Check file age
        return minSpan < ttl;
    }

    private void saveWeatherData() throws IOException {
        if (weatherFile == null) {
            weatherFile = new File(filesDir, "weather" + locationIdx + ".json");

            if (!weatherFile.exists() && !weatherFile.createNewFile())
                throw new IOException("Unable to create locations file");
        }

        JSONParser.serializer(weather, weatherFile);
    }

    public Weather getWeather() { return weather; }
}
