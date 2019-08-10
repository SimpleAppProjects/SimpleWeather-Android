package com.thewizrd.shared_resources.utils;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.text.format.DateFormat;
import android.util.SparseArray;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAlertSeverity;
import com.thewizrd.shared_resources.weatherdata.WeatherAlertType;

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
                double adj = ((13 - humidity) / 4f) * Math.sqrt((17 - Math.abs(temp_f - 95)) / 17);
                HI -= adj;
            } else if (humidity > 85 && (temp_f > 80 && temp_f < 87)) {
                double adj = ((humidity - 85) / 10f) * ((87 - temp_f) / 5);
                HI += adj;
            }

            if (HI > 80 && HI > temp_f)
                return HI;
            else
                return temp_f;
        } else
            return temp_f;
    }

    public static String getWindDirection(float angle) {
        if (angle >= 348.75 && angle <= 11.25) {
            return "N";
        } else if (angle >= 11.25 && angle <= 33.75) {
            return "NNE";
        } else if (angle >= 33.75 && angle <= 56.25) {
            return "NE";
        } else if (angle >= 56.25 && angle <= 78.75) {
            return "ENE";
        } else if (angle >= 78.75 && angle <= 101.25) {
            return "E";
        } else if (angle >= 101.25 && angle <= 123.75) {
            return "ESE";
        } else if (angle >= 123.75 && angle <= 146.25) {
            return "SE";
        } else if (angle >= 146.25 && angle <= 168.75) {
            return "SSE";
        } else if (angle >= 168.75 && angle <= 191.25) {
            return "S";
        } else if (angle >= 191.25 && angle <= 213.75) {
            return "SSW";
        } else if (angle >= 213.75 && angle <= 236.25) {
            return "SW";
        } else if (angle >= 236.25 && angle <= 258.75) {
            return "WSW";
        } else if (angle >= 258.75 && angle <= 281.25) {
            return "W";
        } else if (angle >= 281.25 && angle <= 303.75) {
            return "WNW";
        } else if (angle >= 303.75 && angle <= 326.25) {
            return "NW";
        } else/* if (angle >= 326.25 && angle <= 348.75)*/ {
            return "NNW";
        }
    }

    public static int getDrawableFromAlertType(WeatherAlertType type) {
        int drawable = -1;

        switch (type) {
            case DENSEFOG:
                drawable = R.drawable.fog;
                break;
            case FIRE:
                drawable = R.drawable.fire;
                break;
            case FLOODWARNING:
            case FLOODWATCH:
                drawable = R.drawable.flood;
                break;
            case HEAT:
                drawable = R.drawable.hot;
                break;
            case HIGHWIND:
                drawable = R.drawable.strong_wind;
                break;
            case HURRICANELOCALSTATEMENT:
            case HURRICANEWINDWARNING:
                drawable = R.drawable.hurricane;
                break;
            case SEVERETHUNDERSTORMWARNING:
            case SEVERETHUNDERSTORMWATCH:
                drawable = R.drawable.thunderstorm;
                break;
            case TORNADOWARNING:
            case TORNADOWATCH:
                drawable = R.drawable.tornado;
                break;
            case VOLCANO:
                drawable = R.drawable.volcano;
                break;
            case WINTERWEATHER:
                drawable = R.drawable.snowflake_cold;
                break;
            case DENSESMOKE:
                drawable = R.drawable.smoke;
                break;
            case DUSTADVISORY:
                drawable = R.drawable.dust;
                break;
            case EARTHQUAKEWARNING:
                drawable = R.drawable.earthquake;
                break;
            case GALEWARNING:
                drawable = R.drawable.gale_warning;
                break;
            case SMALLCRAFT:
                drawable = R.drawable.small_craft_advisory;
                break;
            case STORMWARNING:
                drawable = R.drawable.storm_warning;
                break;
            case TSUNAMIWARNING:
            case TSUNAMIWATCH:
                drawable = R.drawable.tsunami;
                break;
            case SEVEREWEATHER:
            case SPECIALWEATHERALERT:
            default:
                drawable = R.drawable.ic_error_white;
                break;
        }

        return drawable;
    }

    public static int getColorFromAlertSeverity(WeatherAlertSeverity severity) {
        int color;

        switch (severity) {
            case SEVERE:
                color = Colors.ORANGERED;
                break;
            case EXTREME:
                color = Colors.RED;
                break;
            case MODERATE:
            default:
                color = Colors.ORANGE;
                break;
        }

        return color;
    }

    public static int getColorFromTempF(float temp_f) {
        int color;

        if (temp_f <= 47.5) {
            color = Colors.LIGHTBLUE;
        } else if (temp_f >= 85) {
            color = Colors.RED;
        } else if (temp_f >= 70) {
            color = Colors.ORANGE;
        } else {
            color = Colors.SIMPLEBLUE;
        }

        return color;
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

        public Coordinate(LocationData location) {
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
            return String.format(Locale.ROOT, "%s,%s", Double.toString(lat), Double.toString(_long));
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
