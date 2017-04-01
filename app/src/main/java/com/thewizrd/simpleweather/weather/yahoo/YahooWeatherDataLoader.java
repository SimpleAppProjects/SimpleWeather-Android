package com.thewizrd.simpleweather.weather.yahoo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.thewizrd.simpleweather.WeatherLoadedListener;
import com.thewizrd.simpleweather.utils.FileUtils;
import com.thewizrd.simpleweather.utils.JSONParser;
import com.thewizrd.simpleweather.utils.Settings;
import com.thewizrd.simpleweather.utils.WeatherException;
import com.thewizrd.simpleweather.utils.WeatherUtils;
import com.thewizrd.simpleweather.weather.yahoo.data.Rootobject;
import com.thewizrd.simpleweather.weather.yahoo.data.YahooWeather;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class YahooWeatherDataLoader {

    private WeatherLoadedListener mCallBack;

    private String location = null;
    private YahooWeather weather = null;
    private int locationIdx = 0;
    private File filesDir = null;
    private File weatherFile = null;
    private Context mContext;

    public YahooWeatherDataLoader(Context context, WeatherLoadedListener listener, String Location, int idx) {
        location = Location;
        locationIdx = idx;

        mContext = context;
        filesDir = mContext.getFilesDir();
        mCallBack = listener;
    }

    public YahooWeatherDataLoader(Context context, WeatherLoadedListener listener, WeatherUtils.Coordinate coordinate, int idx) {
        location = coordinate.getCoordinatePair();
        locationIdx = idx;

        mContext = context;
        filesDir = mContext.getFilesDir();
        mCallBack = listener;
    }

    private void getWeatherData() throws WeatherException {
        String yahooAPI = "https://query.yahooapis.com/v1/public/yql?q=";
        String query = "select * from weather.forecast where woeid in (select woeid from geo.places(1) where text=\""
                + location + "\") and u='" + Settings.getTempUnit() + "'&format=json";
        WeatherException wEx = null;
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

                if (weather != null) {
                    saveTimeZone();
                    saveWeatherData();
                }
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
        } while (weather == null && counter < 10);

        // Load old data if available and we can't get new data
        if (weather == null)
        {
            loadSavedWeatherData(weatherFile, true);
        }

        // Throw error if still null
        if (weather == null && wEx != null) {
            throw wEx;
        } else if (weather == null && wEx == null) {
            throw new WeatherException(WeatherUtils.ErrorStatus.NOWEATHER);
        }
    }

    private YahooWeather parseWeather(String json)
    {
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

    public void loadWeatherData(final boolean forceRefresh) throws IOException {
        if (weatherFile == null) {
            weatherFile = new File(filesDir, "weather" + locationIdx + ".json");

            if (!weatherFile.exists() && !weatherFile.createNewFile())
                throw new IOException("Unable to load weather data");
        }

        new Thread() {
            @Override
            public void run() {
                if (forceRefresh) {
                    try {
                        getWeatherData();
                    } catch (final WeatherException e) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
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
                        mCallBack.onWeatherLoaded(locationIdx, weather);
                    }
                });
            }
        }.start();
    }

    private void loadWeatherData() throws IOException {
        if (weatherFile == null) {
            weatherFile = new File(filesDir, "weather" + locationIdx + ".json");

            if (!weatherFile.exists() && !weatherFile.createNewFile())
                throw new IOException("Unable to load weather data");
        }

        boolean gotData = loadSavedWeatherData(weatherFile);

        if (!gotData) {
            try {
                getWeatherData();
            } catch (final WeatherException e) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private boolean loadSavedWeatherData(File file, boolean _override) {
        if (_override) {
            if (!file.exists() || file.length() == 0)
                return false;

            try {
                String weatherJson = FileUtils.readFile(file);
                weather = (YahooWeather) JSONParser.deserializer(weatherJson, YahooWeather.class);
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
            weather = (YahooWeather) JSONParser.deserializer(weatherJson, YahooWeather.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (weather == null)
            return false;

        // Weather data expiration
        int ttl = Integer.valueOf(weather.ttl);

        Date updateTime = null;
        try {
            updateTime = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'Z'").parse(weather.created);
        } catch (ParseException e) {
            e.printStackTrace();
        }
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
                throw new IOException("Unable to save weather data");
        }

        JSONParser.serializer(weather, weatherFile);
    }

    public YahooWeather getWeather() { return weather; }
}