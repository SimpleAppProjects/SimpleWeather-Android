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
import com.thewizrd.simpleweather.weather.weatherunderground.data.WUWeather;
import com.thewizrd.simpleweather.weather.yahoo.data.YahooWeather;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class WeatherUtils {
    private static Resources res = App.getAppContext().getResources();
    private static AssetManager am = App.getAppContext().getAssets();

    //region Yahoo Weather
    public static String GetWeatherIcon(int yahoo_weather_code) {
        String WeatherIcon;

        switch (yahoo_weather_code)
        {
            case 0: // Tornado
                WeatherIcon = res.getString(R.string.wi_yahoo_0);
                break;
            case 1: // Tropical Storm
            case 37:
            case 38: // Scattered Thunderstorms/showers
            case 39:
            case 45:
            case 47:
                WeatherIcon = res.getString(R.string.wi_yahoo_1);
                break;
            case 2: // Hurricane
                WeatherIcon = res.getString(R.string.wi_yahoo_2);
                break;
            case 3:
            case 4: // Scattered Thunderstorms
                WeatherIcon = res.getString(R.string.wi_yahoo_3);
                break;
            case 5: // Mixed Rain/Snow
            case 6: // Mixed Rain/Sleet
            case 7: // Mixed Snow/Sleet
            case 18: // Sleet
            case 35: // Mixed Rain/Hail
                WeatherIcon = res.getString(R.string.wi_yahoo_5);
                break;
            case 8: // Freezing Drizzle
            case 10: // Freezing Rain
            case 17: // Hail
                WeatherIcon = res.getString(R.string.wi_yahoo_8);
                break;
            case 9: // Drizzle
            case 11: // Showers
            case 12:
            case 40: // Scattered Showers
                WeatherIcon = res.getString(R.string.wi_yahoo_9);
                break;
            case 13: // Snow Flurries
            case 14: // Light Snow Showers
            case 16: // Snow
            case 42: // Scattered Snow Showers
            case 46: // Snow Showers
                WeatherIcon = res.getString(R.string.wi_yahoo_13);
                break;
            case 15: // Blowing Snow
            case 41: // Heavy Snow
            case 43:
                WeatherIcon = res.getString(R.string.wi_yahoo_15);
                break;
            case 19: // Dust
                WeatherIcon = res.getString(R.string.wi_yahoo_19);
                break;
            case 20: // Foggy
                WeatherIcon = res.getString(R.string.wi_yahoo_20);
                break;
            case 21: // Haze
                WeatherIcon = res.getString(R.string.wi_yahoo_21);
                break;
            case 22: // Smoky
                WeatherIcon = res.getString(R.string.wi_yahoo_22);
                break;
            case 23: // Blustery
            case 24: // Windy
                WeatherIcon = res.getString(R.string.wi_yahoo_23);
                break;
            case 25: // Cold
                WeatherIcon = res.getString(R.string.wi_yahoo_25);
                break;
            case 26: // Cloudy
                WeatherIcon = res.getString(R.string.wi_yahoo_26);
                break;
            case 27: // Mostly Cloudy (Night)
            case 29: // Partly Cloudy (Night)
                WeatherIcon = res.getString(R.string.wi_yahoo_27);
                break;
            case 28: // Mostly Cloudy (Day)
            case 30: // Partly Cloudy (Day)
                WeatherIcon = res.getString(R.string.wi_yahoo_28);
                break;
            case 31: // Clear (Night)
                WeatherIcon = res.getString(R.string.wi_yahoo_31);
                break;
            case 32: // Sunny
                WeatherIcon = res.getString(R.string.wi_yahoo_32);
                break;
            case 33: // Fair (Night)
                WeatherIcon = res.getString(R.string.wi_yahoo_33);
                break;
            case 34: // Fair (Day)
            case 44: // Partly Cloudy
                WeatherIcon = res.getString(R.string.wi_yahoo_34);
                break;
            case 36: // HOT
                WeatherIcon = res.getString(R.string.wi_yahoo_36);
                break;
            case 3200: // Not Available
            default:
                WeatherIcon = res.getString(R.string.wi_yahoo_3200);
                break;
        }

        return WeatherIcon;
    }

    public static BitmapDrawable GetBackground(YahooWeather weather, int width, int height) throws IOException {
        InputStream imgStream = null;

        // Apply background based on weather condition
        switch (Integer.valueOf(weather.condition.code))
        {
            // Night
            case 31:
            case 33:
                imgStream = am.open("backgrounds/NightSky.jpg");
                break;
            // Rain
            case 9:
            case 11:
            case 12:
            case 40:
                // (Mixed) Rain/Snow/Sleet
            case 5:
            case 6:
            case 7:
            case 18:
                // Hail / Freezing Rain
            case 8:
            case 10:
            case 17:
            case 35:
                imgStream = am.open("backgrounds/RainySky.jpg");
                break;
            // Tornado / Hurricane / Thunderstorm / Tropical Storm
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 37:
            case 38:
            case 39:
            case 45:
            case 47:
                imgStream = am.open("backgrounds/StormySky.jpg");
                break;
            // Dust
            case 19:
                imgStream = am.open("backgrounds/Dust.jpg");
                break;
            // Foggy / Haze
            case 20:
            case 21:
            case 22:
                imgStream = am.open("backgrounds/FoggySky.jpg");
                break;
            // Snow / Snow Showers/Storm
            case 13:
            case 14:
            case 15:
            case 16:
            case 41:
            case 42:
            case 43:
            case 46:
                imgStream = am.open("backgrounds/Snow.jpg");
                break;
            /* Ambigious weather conditions */
            // (Mostly) Cloudy
            case 28:
            case 26:
            case 27:
                if (isNight(weather))
                {
                    imgStream = am.open("backgrounds/MostlyCloudy-Night.jpg");
                }
                else
                {
                    imgStream = am.open("backgrounds/MostlyCloudy-Day.jpg");
                }
                break;
            // Partly Cloudy
            case 44:
            case 29:
            case 30:
                if (isNight(weather))
                {
                    imgStream = am.open("backgrounds/PartlyCloudy-Night.jpg");
                }
                else
                {
                    imgStream = am.open("backgrounds/PartlyCloudy-Day.jpg");
                }
                break;
        }

        if (imgStream == null)
        {
            // Set background based using sunset/rise times
            if (isNight(weather))
            {
                imgStream = am.open("backgrounds/NightSky.jpg");
            }
            else
            {
                imgStream = am.open("backgrounds/DaySky.jpg");
            }
        }

        return new BitmapDrawable(res, ThumbnailUtils.extractThumbnail(BitmapFactory.decodeStream(imgStream), width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT));
    }

    public static boolean isNight(YahooWeather weather)
    {
        Date sunriseTime = null;
        Date sunsetTime = null;
        try {
            sunriseTime = new SimpleDateFormat("hh:mm a").parse(weather.astronomy.getSunrise());
            sunsetTime = new SimpleDateFormat("hh:mm a").parse(weather.astronomy.getSunset());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Calendar now = Calendar.getInstance(TimeZone.getTimeZone(TimeZone.getAvailableIDs(weather.location.offset)[0]));
        Calendar today = new GregorianCalendar();
        Calendar sunrise = new GregorianCalendar();
        Calendar sunset = new GregorianCalendar();

        today.set(0, 0, 0, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        sunrise.set(0, 0, 0, Integer.valueOf(new SimpleDateFormat("H").format(sunriseTime)), Integer.valueOf(new SimpleDateFormat("m").format(sunriseTime)));
        sunset.set(0, 0, 0, Integer.valueOf(new SimpleDateFormat("H").format(sunsetTime)), Integer.valueOf(new SimpleDateFormat("m").format(sunriseTime)));

        return today.getTime().getTime() < (sunrise.getTime().getTime()) || today.getTime().getTime() > sunset.getTime().getTime();
    }

    public static String GetLastBuildDate(YahooWeather weather)
    {
        String date;

        Date updateTime = null;
        try {
            updateTime = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'Z'").parse(weather.created);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(updateTime.getTime() +
                TimeZone.getDefault().getOffset(System.currentTimeMillis()));

        if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
        {
            date = "Updated at " + new SimpleDateFormat("hh:mm a").format(cal.getTime());
        }
        else
            date = "Updated on " + new SimpleDateFormat("EEE hh:mm a").format(cal.getTime());

        return date;
    }
    //endregion

    //region WeatherUnderground
    public static String GetWeatherIcon(String wundergrnd_icon) {
        String WeatherIcon;

        if (wundergrnd_icon.contains("nt_clear") || wundergrnd_icon.contains("nt_mostlysunny")
                || wundergrnd_icon.contains("nt_partlysunny") || wundergrnd_icon.contains("nt_sunny"))
            WeatherIcon = res.getString(R.string.wi_night_clear);
        else if (wundergrnd_icon.contains("nt_mostlycloudy") || wundergrnd_icon.contains("nt_partlycloudy")
                || wundergrnd_icon.contains("nt_cloudy"))
            WeatherIcon = res.getString(R.string.wi_night_cloudy);
        else if (wundergrnd_icon.contains("mostlysunny") || wundergrnd_icon.contains("partlysunny"))
            WeatherIcon = res.getString(R.string.wi_night_cloudy);
        else if (wundergrnd_icon.contains("mostlycloudy") || wundergrnd_icon.contains("partlycloudy"))
            WeatherIcon = res.getString(R.string.wi_wu_cloudy);
        else if (wundergrnd_icon.contains("flurries"))
            WeatherIcon = res.getString(R.string.wi_wu_flurries);
        else if (wundergrnd_icon.contains("hazy"))
            WeatherIcon = res.getString(R.string.wi_wu_hazy);
        else if (wundergrnd_icon.contains("fog"))
            WeatherIcon = res.getString(R.string.wi_fog);
        else if (wundergrnd_icon.contains("rain"))
            WeatherIcon = res.getString(R.string.wi_wu_rain);
        else if (wundergrnd_icon.contains("sleet"))
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

    public static BitmapDrawable GetBackground(WUWeather weather, int width, int height) throws IOException {
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

    public static boolean isNight(WUWeather weather)
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

    public static String GetLastBuildDate(WUWeather weather)
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
    //endregion

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
        NOWEATHER,
        NETWORKERROR,
        INVALIDAPIKEY,
        QUERYNOTFOUND,
    }
}
