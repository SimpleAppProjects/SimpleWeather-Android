package com.thewizrd.simpleweather.weather.weatherunderground;

import android.content.Context;
import android.os.AsyncTask;

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

public class WeatherDataLoader extends AsyncTask<Boolean, Void, Weather> {

    OnWeatherLoadedListener mCallBack;
    //OnWeatherErrorListener mErrorBack;

    public interface OnWeatherLoadedListener {
        public void onWeatherLoaded(Weather weather);
    }

    /*
    public interface OnWeatherErrorListener {
        public void onWeatherError(WeatherUtils.ErrorStatus status);
    }
    */

    protected Weather doInBackground(Boolean... forceRefresh) {
        try {
            loadWeatherData(forceRefresh[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return weather;
    }

    protected void onPostExecute(Weather weather) {
        mCallBack.onWeatherLoaded(weather);
    }

    private String location_query = null;
    private Weather weather = null;
    private int locationIdx = 0;
    private File filesDir = null;
    private File weatherFile = null;
    private Context mContext;

    public WeatherDataLoader(Context context, String query, int idx) {
        location_query = query;
        locationIdx = idx;

        mContext = context;
        filesDir = mContext.getFilesDir();
        mCallBack = (OnWeatherLoadedListener) mContext;
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

    public void loadWeatherData(boolean forceRefresh) throws IOException {
        if (weatherFile == null) {
            weatherFile = new File(filesDir, "weather" + locationIdx + ".json");

            if (!weatherFile.exists() && !weatherFile.createNewFile())
                throw new IOException("Unable to create locations file");
        }

        if (forceRefresh) {
            getWeatherData();
        }
        else
            loadWeatherData();
    }

    public void loadWeatherData() throws IOException {
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

            if (weather == null)
                return false;

            return true;
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
        if (minSpan < ttl)
            return true;
        else
            return false;
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
