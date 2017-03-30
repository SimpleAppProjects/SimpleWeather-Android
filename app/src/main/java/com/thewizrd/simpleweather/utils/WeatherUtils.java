package com.thewizrd.simpleweather.utils;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.ThumbnailUtils;

import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.weather.weatherunderground.data.Sunrise1;
import com.thewizrd.simpleweather.weather.weatherunderground.data.Sunset1;
import com.thewizrd.simpleweather.weather.weatherunderground.data.Weather;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class WeatherUtils {
    private static Resources res = App.getAppContext().getResources();
    private static AssetManager am = App.getAppContext().getAssets();

    public static String GetWeatherIcon(String wundergrnd_icon) {
        String WeatherIcon;

        if (wundergrnd_icon.contains("nt_clear") || wundergrnd_icon.contains("nt_mostlysunny")
                || wundergrnd_icon.contains("nt_partlysunny") || wundergrnd_icon.contains("nt_sunny"))
            WeatherIcon = res.getString(R.string.wi_night_clear);
        else if (wundergrnd_icon.contains("nt_mostlycloudy") || wundergrnd_icon.contains("nt_partlycloudy"))
            WeatherIcon = res.getString(R.string.wi_night_cloudy);
        else if (wundergrnd_icon.contains("mostlysunny") || wundergrnd_icon.contains("partlysunny"))
            WeatherIcon = res.getString(R.string.wi_night_cloudy);
        else if (wundergrnd_icon.contains("mostlycloudy") || wundergrnd_icon.contains("partlycloudy"))
            WeatherIcon = res.getString(R.string.wi_wu_cloudy);
        else if (wundergrnd_icon.contains("flurries"))
            WeatherIcon = res.getString(R.string.wi_wu_flurries);
        else if (wundergrnd_icon.contains("hazy"))
            WeatherIcon = res.getString(R.string.wi_wu_hazy);
        else if (wundergrnd_icon.contains("rain"))
            WeatherIcon = res.getString(R.string.wi_wu_rain);
        else if (wundergrnd_icon.contains("sleat"))
            WeatherIcon = res.getString(R.string.wi_wu_sleat);
        else if (wundergrnd_icon.contains("snow"))
            WeatherIcon = res.getString(R.string.wi_wu_snow);
        else if (wundergrnd_icon.contains("tstorms"))
            WeatherIcon = res.getString(R.string.wi_wu_tstorms);
        else if (wundergrnd_icon.contains("cloudy"))
            WeatherIcon = res.getString(R.string.wi_wu_cloudy);
        else if (wundergrnd_icon.contains("clear") || wundergrnd_icon.contains("sunny"))
            WeatherIcon = res.getString(R.string.wi_wu_clear);
        else
            WeatherIcon = res.getString(R.string.wi_wu_unknown);

        return WeatherIcon;
    }

    public static BitmapDrawable GetBackground(Weather weather, int width, int height) throws IOException {
        InputStream imgStream = null;

        // Apply background based on weather condition
        switch (weather.condition.icon)
        {
            case "cloudy":
            case "mostlycloudy":
                if (isNight(weather))
                {
                    imgStream = am.open("backgrounds/MostlyCloudy-Night.jpg");
                }
                else
                {
                    imgStream = am.open("backgrounds/MostlyCloudy-Day.jpg");
                }
                break;
            case "mostlysunny":
            case "partlysunny":
            case "partlycloudy":
                if (isNight(weather))
                {
                    imgStream = am.open("backgrounds/PartlyCloudy-Night.jpg");
                }
                else
                {
                    imgStream = am.open("backgrounds/PartlyCloudy-Day.jpg");
                }
                break;
            case "chancerain":
            case "chancesleat":
            case "rain":
            case "sleat":
                imgStream = am.open("backgrounds/RainySky.jpg");
                break;
            case "chanceflurries":
            case "chancesnow":
            case "flurries":
            case "snow":
                imgStream = am.open("backgrounds/Snow.jpg");
                break;
            case "chancetstorms":
            case "tstorms":
                imgStream = am.open("backgrounds/StormySky.jpg");
                break;
            case "hazy":
                imgStream = am.open("backgrounds/FoggySky.jpg");
                break;
            case "sunny":
            case "clear":
            case "unknown":
            default:
                // Set background based using sunset/rise times
                if (isNight(weather))
                {
                    imgStream = am.open("backgrounds/NightSky.jpg");
                }
                else
                {
                    imgStream = am.open("backgrounds/DaySky.jpg");
                }
                break;
        }

        return new BitmapDrawable(res, ThumbnailUtils.extractThumbnail(BitmapFactory.decodeStream(imgStream), width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT));
    }

    public static boolean isNight(Weather weather)
    {
        Sunset1 sunsetInfo = weather.sun_phase.sunset;
        Sunrise1 sunriseInfo = weather.sun_phase.sunrise;

        Calendar now = Calendar.getInstance(TimeZone.getTimeZone(weather.location.tz_long));
        Calendar today = new GregorianCalendar();
        Calendar sunrise = new GregorianCalendar();
        Calendar sunset = new GregorianCalendar();

        today.set(0, 0, 0, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        sunrise.set(0, 0, 0, Integer.valueOf(sunriseInfo.hour), Integer.valueOf(sunriseInfo.minute));
        sunset.set(0, 0, 0, Integer.valueOf(sunsetInfo.hour), Integer.valueOf(sunsetInfo.minute));

        return today.getTime().getTime() < (sunrise.getTime().getTime()) || today.getTime().getTime() > sunset.getTime().getTime();
    }

    public static String GetLastBuildDate(Weather weather)
    {
        String date;

        Date updateTime = weather.update_time;
        Calendar cal = Calendar.getInstance();
        cal.setTime(updateTime);

        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        {
            date = "Updated at " + new SimpleDateFormat("hh:mm a").format(updateTime);
        }
        else
            date = "Updated on " + new SimpleDateFormat("EEE hh:mm a").format(updateTime);

        return date;
    }

    public static class Coordinate {
        private double lat = 0;
        private double _long = 0;

        public Coordinate(String coordinatePair) {
            setCoordinate(coordinatePair);
        }

        public Coordinate(double latitude, double longitude) {
            setCoordinate(latitude, longitude);
        }

        public void setCoordinate(String coordinatePair) {
            String[] coord = coordinatePair.split(",");
            lat = Double.valueOf(coord[0]);
            _long = Double.valueOf(coord[1]);
        }

        public void setCoordinate(double latitude, double longitude) {
            lat = latitude;
            _long = longitude;
        }

        public double getLatitude() {
            return lat;
        }

        public double getLongitude() {
            return _long;
        }

        public String getCoordinatePair() {
            return "(" + lat + ", " + _long + ")";
        }
    }

    public enum ErrorStatus
    {
        UNKNOWN,
        SUCCESS,
        NOWEATHER,
        NETWORKERROR,
        INVALIDAPIKEY,
        QUERYNOTFOUND,
    }
}
