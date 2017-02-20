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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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

    private static boolean isNight(Weather weather)
    {
        Sunset1 sunsetInfo = weather.sun_phase.sunset;
        Sunrise1 sunriseInfo = weather.sun_phase.sunrise;

        String sunset_string =
                String.format("%s:%s %s", sunsetInfo.hour, sunsetInfo.minute, weather.location.tz_offset);
        String sunrise_string =
                String.format("%s:%s %s", sunriseInfo.hour, sunriseInfo.minute, weather.location.tz_offset);

        DateFormat sunset = new SimpleDateFormat("HH:mm Z");
        DateFormat sunrise = new SimpleDateFormat("HH:mm Z");
        try {
            sunset.parse(sunset_string);
            sunrise.parse(sunrise_string);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Date now = new Date();
        Calendar today = Calendar.getInstance();
        today.setTime(now);

        try {
            now = new SimpleDateFormat("HH:mm Z").parse(
                    String.format(Locale.getDefault(), "%d:%d %s", today.get(Calendar.HOUR_OF_DAY), today.get(Calendar.MINUTE), weather.location.tz_offset));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        // Determine whether its night using sunset/rise times
        if (now.getTime() < sunrise.getCalendar().getTimeInMillis() || now.getTime() > sunset.getCalendar().getTimeInMillis())
            return true;
        else
            return false;
    }

    public static String GetLastBuildDate(Weather weather)
    {
        String date;

        Date updateTime = weather.update_time;
        Calendar cal = Calendar.getInstance();
        cal.setTime(updateTime);

        if (cal.DAY_OF_WEEK == Calendar.getInstance().DAY_OF_WEEK)
        {
            date = "Updated at " + new SimpleDateFormat("hh:mm a").format(updateTime);
        }
        else
            date = "Updated on " + new SimpleDateFormat("EEE hh:mm a").format(updateTime);

        return date;
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
