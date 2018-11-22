package com.thewizrd.shared_resources.utils;

import android.content.Context;
import android.location.Location;
import android.text.format.DateFormat;
import android.util.SparseArray;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.weatherdata.Weather;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.Locale;

public class WeatherUtils {

    public static String getLastBuildDate(Weather weather) {
        Context context = SimpleLibrary.getInstance().getApp().getAppContext();
        String date;
        String prefix;
        LocalDateTime update_time = weather.getUpdateTime().toLocalDateTime();
        String timeformat = update_time.format(DateTimeFormatter.ofPattern("a", Locale.getDefault()));

        if (DateFormat.is24HourFormat(context))
            timeformat = update_time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()));
        else
            timeformat = update_time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()));

        timeformat = String.format("%s %s", timeformat, weather.getLocation().getTzShort());

        if (update_time.getDayOfWeek() == ZonedDateTime.now().getDayOfWeek()) {
            prefix = context.getString(R.string.update_prefix_day);
            date = String.format("%s %s", prefix, timeformat);
        } else {
            prefix = context.getString(R.string.update_prefix);
            date = String.format("%s %s %s", prefix, update_time.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())), timeformat);
        }

        return date;
    }

    public static String getFeelsLikeTemp(String temp_f, String wind_mph, String humidity_percent) {
        String feelslikeTemp = temp_f;

        float temp = -99.00f;
        float windmph = -1f;
        int humidity = -1;

        try {
            temp = Float.parseFloat(temp_f);
        } catch (NumberFormatException e) {
        }
        try {
            windmph = Float.parseFloat(wind_mph);
        } catch (NumberFormatException e) {
        }
        try {
            humidity = Integer.parseInt(humidity_percent);
        } catch (NumberFormatException e) {
        }

        if (temp > -99.00f) {
            if (temp < 50 && windmph > -1f) {
                feelslikeTemp = Float.toString(calculateWindChill(temp, windmph));
            } else if (temp > 80 && humidity > -1) {
                feelslikeTemp = Double.toString(calculateHeatIndex(temp, humidity));
            } else
                feelslikeTemp = temp_f;
        }

        return feelslikeTemp;
    }

    public static float calculateWindChill(float temp_f, float wind_mph) {
        if (temp_f < 50)
            return (float) (35.74f + (0.6215f * temp_f) - (35.75f * Math.pow(wind_mph, 0.16f)) + (0.4275f * temp_f * Math.pow(wind_mph, 0.16f)));
        else
            return temp_f;
    }

    public static double calculateHeatIndex(float temp_f, int humidity) {
        if (temp_f > 80) {
            double HI = -42.379
                    + (2.04901523 * temp_f)
                    + (10.14333127 * humidity)
                    - (0.22475541 * temp_f * humidity)
                    - (0.00683783 * Math.pow(temp_f, 2))
                    - (0.05481717 * Math.pow(humidity, 2))
                    + (0.00122874 * Math.pow(temp_f, 2) * humidity)
                    + (0.00085282 * temp_f * Math.pow(humidity, 2))
                    - (0.00000199 * Math.pow(temp_f, 2) * Math.pow(humidity, 2));

            if (humidity < 13 && (temp_f > 80 && temp_f < 112)) {
                double adj = ((13 - humidity) / 4) * Math.sqrt((17 - Math.abs(temp_f - 95)) / 17);
                HI -= adj;
            } else if (humidity > 85 && (temp_f > 80 && temp_f < 87)) {
                double adj = ((humidity - 85) / 10) * ((87 - temp_f) / 5);
                HI += adj;
            }

            if (HI > 80 && HI > temp_f)
                return HI;
            else
                return temp_f;
        } else
            return temp_f;
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

        public Coordinate(Location location) {
            lat = location.getLatitude();
            _long = location.getLongitude();
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

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "%f,%f", lat, _long);
        }
    }

    public enum ErrorStatus {
        UNKNOWN(-1),
        SUCCESS(0),
        NOWEATHER(1),
        NETWORKERROR(2),
        INVALIDAPIKEY(3),
        QUERYNOTFOUND(4);

        private final int value;

        public int getValue() {
            return value;
        }

        private ErrorStatus(int value) {
            this.value = value;
        }

        private static SparseArray<ErrorStatus> map = new SparseArray<>();

        static {
            for (ErrorStatus errorStatus : values()) {
                map.put(errorStatus.value, errorStatus);
            }
        }

        public static ErrorStatus valueOf(int value) {
            return map.get(value);
        }
    }
}
