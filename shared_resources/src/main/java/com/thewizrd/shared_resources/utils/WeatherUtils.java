package com.thewizrd.shared_resources.utils;

import android.content.Context;
import android.location.Location;
import android.text.format.DateFormat;
import android.util.SparseArray;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.thewizrd.shared_resources.DateTimeConstants;
import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.ImageDataViewModel;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.preferences.FeatureSettings;
import com.thewizrd.shared_resources.weatherdata.Beaufort;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAlertSeverity;
import com.thewizrd.shared_resources.weatherdata.WeatherAlertType;
import com.thewizrd.shared_resources.weatherdata.WeatherBackground;
import com.thewizrd.shared_resources.weatherdata.WeatherIcons;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.shared_resources.weatherdata.images.ImageDataHelper;
import com.thewizrd.shared_resources.weatherdata.images.ImageDataHelperImpl;
import com.thewizrd.shared_resources.weatherdata.images.model.ImageData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Locale;

public class WeatherUtils {

    public static String getLastBuildDate(Weather weather) {
        Context context = SimpleLibrary.getInstance().getApp().getAppContext();
        String date;
        String prefix;
        LocalDateTime update_time = weather.getUpdateTime().toLocalDateTime();
        String timeformat;

        if (DateFormat.is24HourFormat(context))
            timeformat = update_time.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_24HR));
        else
            timeformat = update_time.format(DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.CLOCK_FORMAT_12HR_AMPM));

        timeformat = String.format("%s %s", timeformat, weather.getLocation().getTzShort());

        if (update_time.getDayOfWeek() == ZonedDateTime.now().getDayOfWeek()) {
            prefix = context.getString(R.string.update_prefix_day);
            date = String.format("%s %s", prefix, timeformat);
        } else {
            prefix = context.getString(R.string.update_prefix);
            date = String.format("%s %s %s", prefix, update_time.format(
                    DateTimeUtils.ofPatternForUserLocale(DateTimeConstants.ABBREV_DAY_OF_THE_WEEK)), timeformat);
        }

        return date;
    }

    public static float getFeelsLikeTemp(float temp_f, float wind_mph, int humidity_percent) {
        float feelslikeTemp;

        if (temp_f < 50)
            feelslikeTemp = calculateWindChill(temp_f, wind_mph);
        else if (temp_f > 80)
            feelslikeTemp = calculateHeatIndex(temp_f, humidity_percent);
        else
            feelslikeTemp = temp_f;

        return feelslikeTemp;
    }

    public static float calculateWindChill(float temp_f, float wind_mph) {
        if (temp_f < 50)
            return (float) (35.74f + (0.6215f * temp_f) - (35.75f * Math.pow(wind_mph, 0.16f)) + (0.4275f * temp_f * Math.pow(wind_mph, 0.16f)));
        else
            return temp_f;
    }

    public static float calculateHeatIndex(float temp_f, int humidity) {
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
                return (float) HI;
            else
                return temp_f;
        } else
            return temp_f;
    }

    public static float calculateDewpointF(float temp_f, int humidity) {
        return ConversionMethods.CtoF(calculateDewpointC(ConversionMethods.FtoC(temp_f), humidity));
    }

    public static float calculateDewpointC(float temp_c, int humidity) {
        return (float) (243.04f * (Math.log(humidity / 100f) + ((17.625f * temp_c) / (243.04f + temp_c))) / (17.625f - Math.log(humidity / 100f) - ((17.625f * temp_c) / (243.04f + temp_c))));
    }

    public static String getWindDirection(float angle) {
        Context context = SimpleLibrary.getInstance().getApp().getAppContext();

        if (angle >= 348.75 && angle <= 11.25) {
            return context.getString(R.string.wind_dir_n);
        } else if (angle >= 11.25 && angle <= 33.75) {
            return context.getString(R.string.wind_dir_nne);
        } else if (angle >= 33.75 && angle <= 56.25) {
            return context.getString(R.string.wind_dir_ne);
        } else if (angle >= 56.25 && angle <= 78.75) {
            return context.getString(R.string.wind_dir_ene);
        } else if (angle >= 78.75 && angle <= 101.25) {
            return context.getString(R.string.wind_dir_e);
        } else if (angle >= 101.25 && angle <= 123.75) {
            return context.getString(R.string.wind_dir_ese);
        } else if (angle >= 123.75 && angle <= 146.25) {
            return context.getString(R.string.wind_dir_se);
        } else if (angle >= 146.25 && angle <= 168.75) {
            return context.getString(R.string.wind_dir_sse);
        } else if (angle >= 168.75 && angle <= 191.25) {
            return context.getString(R.string.wind_dir_s);
        } else if (angle >= 191.25 && angle <= 213.75) {
            return context.getString(R.string.wind_dir_ssw);
        } else if (angle >= 213.75 && angle <= 236.25) {
            return context.getString(R.string.wind_dir_sw);
        } else if (angle >= 236.25 && angle <= 258.75) {
            return context.getString(R.string.wind_dir_wsw);
        } else if (angle >= 258.75 && angle <= 281.25) {
            return context.getString(R.string.wind_dir_w);
        } else if (angle >= 281.25 && angle <= 303.75) {
            return context.getString(R.string.wind_dir_wnw);
        } else if (angle >= 303.75 && angle <= 326.25) {
            return context.getString(R.string.wind_dir_nw);
        } else/* if (angle >= 326.25 && angle <= 348.75)*/ {
            return context.getString(R.string.wind_dir_nnw);
        }
    }

    /* Used by NWS */
    public static int getWindDirection(String direction) {
        if ("N".equals(direction)) {
            return 0;
        } else if ("NNE".equals(direction)) {
            return 22;
        } else if ("NE".equals(direction)) {
            return 45;
        } else if ("ENE".equals(direction)) {
            return 67;
        } else if ("E".equals(direction)) {
            return 90;
        } else if ("ESE".equals(direction)) {
            return 112;
        } else if ("SE".equals(direction)) {
            return 135;
        } else if ("SSE".equals(direction)) {
            return 157;
        } else if ("S".equals(direction)) {
            return 180;
        } else if ("SSW".equals(direction)) {
            return 202;
        } else if ("SW".equals(direction)) {
            return 225;
        } else if ("WSW".equals(direction)) {
            return 247;
        } else if ("W".equals(direction)) {
            return 270;
        } else if ("WNW".equals(direction)) {
            return 292;
        } else if ("NW".equals(direction)) {
            return 315;
        } else {
            return 337;
        }
    }

    @DrawableRes
    public static int getDrawableFromAlertType(WeatherAlertType type) {
        int drawable = -1;

        switch (type) {
            case DENSEFOG:
                drawable = R.drawable.wi_fog;
                break;
            case FIRE:
                drawable = R.drawable.wi_fire;
                break;
            case FLOODWARNING:
            case FLOODWATCH:
                drawable = R.drawable.wi_flood;
                break;
            case HEAT:
                drawable = R.drawable.wi_hot;
                break;
            case HIGHWIND:
                drawable = R.drawable.wi_strong_wind;
                break;
            case HURRICANELOCALSTATEMENT:
            case HURRICANEWINDWARNING:
                drawable = R.drawable.wi_hurricane;
                break;
            case SEVERETHUNDERSTORMWARNING:
            case SEVERETHUNDERSTORMWATCH:
                drawable = R.drawable.wi_thunderstorm;
                break;
            case TORNADOWARNING:
            case TORNADOWATCH:
                drawable = R.drawable.wi_tornado;
                break;
            case VOLCANO:
                drawable = R.drawable.wi_volcano;
                break;
            case WINTERWEATHER:
                drawable = R.drawable.wi_snowflake_cold;
                break;
            case DENSESMOKE:
                drawable = R.drawable.wi_smoke;
                break;
            case DUSTADVISORY:
                drawable = R.drawable.wi_dust;
                break;
            case EARTHQUAKEWARNING:
                drawable = R.drawable.wi_earthquake;
                break;
            case GALEWARNING:
                drawable = R.drawable.wi_gale_warning;
                break;
            case SMALLCRAFT:
                drawable = R.drawable.wi_small_craft_advisory;
                break;
            case STORMWARNING:
                drawable = R.drawable.wi_storm_warning;
                break;
            case TSUNAMIWARNING:
            case TSUNAMIWATCH:
                drawable = R.drawable.wi_tsunami;
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
        return getColorFromTempF(temp_f, Colors.SIMPLEBLUE);
    }

    @ColorInt
    public static int getColorFromTempF(float temp_f, @ColorInt int defaultColor) {
        int color;

        if (temp_f <= 47.5) {
            color = Colors.LIGHTBLUE;
        } else if (temp_f >= 85) {
            color = Colors.RED;
        } else if (temp_f >= 70) {
            color = Colors.ORANGE;
        } else {
            color = defaultColor;
        }

        return color;
    }

    @DrawableRes
    public static int getWeatherIconResource(String icon) {
        int weatherIcon = -1;

        switch (icon) {
            // Day
            case WeatherIcons.DAY_SUNNY:
                weatherIcon = R.drawable.wi_day_sunny;
                break;
            case WeatherIcons.DAY_CLOUDY:
                weatherIcon = R.drawable.wi_day_cloudy;
                break;
            case WeatherIcons.DAY_CLOUDY_GUSTS:
                weatherIcon = R.drawable.wi_day_cloudy_gusts;
                break;
            case WeatherIcons.DAY_CLOUDY_WINDY:
                weatherIcon = R.drawable.wi_day_cloudy_windy;
                break;
            case WeatherIcons.DAY_FOG:
                weatherIcon = R.drawable.wi_day_fog;
                break;
            case WeatherIcons.DAY_HAIL:
                weatherIcon = R.drawable.wi_day_hail;
                break;
            case WeatherIcons.DAY_HAZE:
                weatherIcon = R.drawable.wi_day_haze;
                break;
            case WeatherIcons.DAY_LIGHTNING:
                weatherIcon = R.drawable.wi_day_lightning;
                break;
            case WeatherIcons.DAY_RAIN:
                weatherIcon = R.drawable.wi_day_rain;
                break;
            case WeatherIcons.DAY_RAIN_MIX:
                weatherIcon = R.drawable.wi_day_rain_mix;
                break;
            case WeatherIcons.DAY_RAIN_WIND:
                weatherIcon = R.drawable.wi_day_rain_wind;
                break;
            case WeatherIcons.DAY_SHOWERS:
                weatherIcon = R.drawable.wi_day_showers;
                break;
            case WeatherIcons.DAY_SLEET:
                weatherIcon = R.drawable.wi_day_sleet;
                break;
            case WeatherIcons.DAY_SLEET_STORM:
                weatherIcon = R.drawable.wi_day_sleet_storm;
                break;
            case WeatherIcons.DAY_SNOW:
                weatherIcon = R.drawable.wi_day_snow;
                break;
            case WeatherIcons.DAY_SNOW_THUNDERSTORM:
                weatherIcon = R.drawable.wi_day_snow_thunderstorm;
                break;
            case WeatherIcons.DAY_SNOW_WIND:
                weatherIcon = R.drawable.wi_day_snow_wind;
                break;
            case WeatherIcons.DAY_SPRINKLE:
                weatherIcon = R.drawable.wi_day_sprinkle;
                break;
            case WeatherIcons.DAY_STORM_SHOWERS:
                weatherIcon = R.drawable.wi_day_storm_showers;
                break;
            case WeatherIcons.DAY_SUNNY_OVERCAST:
                weatherIcon = R.drawable.wi_day_sunny_overcast;
                break;
            case WeatherIcons.DAY_THUNDERSTORM:
                weatherIcon = R.drawable.wi_day_thunderstorm;
                break;
            case WeatherIcons.DAY_WINDY:
                weatherIcon = R.drawable.wi_day_windy;
                break;
            case WeatherIcons.DAY_HOT:
                weatherIcon = R.drawable.wi_hot;
                break;
            case WeatherIcons.DAY_CLOUDY_HIGH:
                weatherIcon = R.drawable.wi_day_cloudy_high;
                break;
            case WeatherIcons.DAY_LIGHT_WIND:
                weatherIcon = R.drawable.wi_day_light_wind;
                break;

            // Night
            case WeatherIcons.NIGHT_CLEAR:
                weatherIcon = R.drawable.wi_night_clear;
                break;
            case WeatherIcons.NIGHT_ALT_CLOUDY:
                weatherIcon = R.drawable.wi_night_alt_cloudy;
                break;
            case WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS:
                weatherIcon = R.drawable.wi_night_alt_cloudy_gusts;
                break;
            case WeatherIcons.NIGHT_ALT_CLOUDY_WINDY:
                weatherIcon = R.drawable.wi_night_alt_cloudy_windy;
                break;
            case WeatherIcons.NIGHT_ALT_HAIL:
                weatherIcon = R.drawable.wi_night_alt_hail;
                break;
            case WeatherIcons.NIGHT_ALT_LIGHTNING:
                weatherIcon = R.drawable.wi_night_alt_lightning;
                break;
            case WeatherIcons.NIGHT_ALT_RAIN:
                weatherIcon = R.drawable.wi_night_alt_rain;
                break;
            case WeatherIcons.NIGHT_ALT_RAIN_MIX:
                weatherIcon = R.drawable.wi_night_alt_rain_mix;
                break;
            case WeatherIcons.NIGHT_ALT_RAIN_WIND:
                weatherIcon = R.drawable.wi_night_alt_rain_wind;
                break;
            case WeatherIcons.NIGHT_ALT_SHOWERS:
                weatherIcon = R.drawable.wi_night_alt_showers;
                break;
            case WeatherIcons.NIGHT_ALT_SLEET:
                weatherIcon = R.drawable.wi_night_alt_sleet;
                break;
            case WeatherIcons.NIGHT_ALT_SLEET_STORM:
                weatherIcon = R.drawable.wi_night_alt_sleet_storm;
                break;
            case WeatherIcons.NIGHT_ALT_SNOW:
                weatherIcon = R.drawable.wi_night_alt_snow;
                break;
            case WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM:
                weatherIcon = R.drawable.wi_night_alt_snow_thunderstorm;
                break;
            case WeatherIcons.NIGHT_ALT_SNOW_WIND:
                weatherIcon = R.drawable.wi_night_alt_snow_wind;
                break;
            case WeatherIcons.NIGHT_ALT_SPRINKLE:
                weatherIcon = R.drawable.wi_night_alt_sprinkle;
                break;
            case WeatherIcons.NIGHT_ALT_STORM_SHOWERS:
                weatherIcon = R.drawable.wi_night_alt_storm_showers;
                break;
            case WeatherIcons.NIGHT_ALT_THUNDERSTORM:
                weatherIcon = R.drawable.wi_night_alt_thunderstorm;
                break;
            case WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY:
                weatherIcon = R.drawable.wi_night_alt_partly_cloudy;
                break;
            case WeatherIcons.NIGHT_ALT_CLOUDY_HIGH:
                weatherIcon = R.drawable.wi_night_alt_cloudy_high;
                break;

            case WeatherIcons.NIGHT_FOG:
                weatherIcon = R.drawable.wi_night_fog;
                break;

            // Neutral
            case WeatherIcons.CLOUD:
                weatherIcon = R.drawable.wi_cloud;
                break;
            case WeatherIcons.CLOUDY:
                weatherIcon = R.drawable.wi_cloudy;
                break;
            case WeatherIcons.CLOUDY_GUSTS:
                weatherIcon = R.drawable.wi_cloudy_gusts;
                break;
            case WeatherIcons.CLOUDY_WINDY:
                weatherIcon = R.drawable.wi_cloudy_windy;
                break;
            case WeatherIcons.FOG:
                weatherIcon = R.drawable.wi_fog;
                break;
            case WeatherIcons.HAIL:
                weatherIcon = R.drawable.wi_hail;
                break;
            case WeatherIcons.RAIN:
                weatherIcon = R.drawable.wi_rain;
                break;
            case WeatherIcons.RAIN_MIX:
                weatherIcon = R.drawable.wi_rain_mix;
                break;
            case WeatherIcons.RAIN_WIND:
                weatherIcon = R.drawable.wi_rain_wind;
                break;
            case WeatherIcons.SHOWERS:
                weatherIcon = R.drawable.wi_showers;
                break;
            case WeatherIcons.SLEET:
                weatherIcon = R.drawable.wi_sleet;
                break;
            case WeatherIcons.SNOW:
                weatherIcon = R.drawable.wi_snow;
                break;
            case WeatherIcons.SPRINKLE:
                weatherIcon = R.drawable.wi_sprinkle;
                break;
            case WeatherIcons.STORM_SHOWERS:
                weatherIcon = R.drawable.wi_storm_showers;
                break;
            case WeatherIcons.THUNDERSTORM:
                weatherIcon = R.drawable.wi_thunderstorm;
                break;
            case WeatherIcons.SNOW_WIND:
                weatherIcon = R.drawable.wi_snow_wind;
                break;
            case WeatherIcons.SMOG:
                weatherIcon = R.drawable.wi_smog;
                break;
            case WeatherIcons.SMOKE:
                weatherIcon = R.drawable.wi_smoke;
                break;
            case WeatherIcons.LIGHTNING:
                weatherIcon = R.drawable.wi_lightning;
                break;
            case WeatherIcons.DUST:
                weatherIcon = R.drawable.wi_dust;
                break;
            case WeatherIcons.SNOWFLAKE_COLD:
                weatherIcon = R.drawable.wi_snowflake_cold;
                break;
            case WeatherIcons.WINDY:
                weatherIcon = R.drawable.wi_windy;
                break;
            case WeatherIcons.STRONG_WIND:
                weatherIcon = R.drawable.wi_strong_wind;
                break;
            case WeatherIcons.SANDSTORM:
                weatherIcon = R.drawable.wi_sandstorm;
                break;
            case WeatherIcons.HURRICANE:
                weatherIcon = R.drawable.wi_hurricane;
                break;
            case WeatherIcons.TORNADO:
                weatherIcon = R.drawable.wi_tornado;
                break;

            case WeatherIcons.FIRE:
                weatherIcon = R.drawable.wi_fire;
                break;
            case WeatherIcons.FLOOD:
                weatherIcon = R.drawable.wi_flood;
                break;
            case WeatherIcons.VOLCANO:
                weatherIcon = R.drawable.wi_volcano;
                break;

            case WeatherIcons.BAROMETER:
                weatherIcon = R.drawable.wi_barometer;
                break;
            case WeatherIcons.HUMIDITY:
                weatherIcon = R.drawable.wi_humidity;
                break;
            case WeatherIcons.MOONRISE:
                weatherIcon = R.drawable.wi_moonrise;
                break;
            case WeatherIcons.MOONSET:
                weatherIcon = R.drawable.wi_moonset;
                break;
            case WeatherIcons.RAINDROP:
                weatherIcon = R.drawable.wi_raindrop;
                break;
            case WeatherIcons.RAINDROPS:
                weatherIcon = R.drawable.wi_raindrops;
                break;
            case WeatherIcons.SUNRISE:
                weatherIcon = R.drawable.wi_sunrise;
                break;
            case WeatherIcons.SUNSET:
                weatherIcon = R.drawable.wi_sunset;
                break;
            case WeatherIcons.THERMOMETER:
                weatherIcon = R.drawable.wi_thermometer;
                break;
            case WeatherIcons.UMBRELLA:
                weatherIcon = R.drawable.wi_umbrella;
                break;
            case WeatherIcons.WIND_DIRECTION:
                weatherIcon = R.drawable.wi_wind_direction;
                break;
            case WeatherIcons.DIRECTION_UP:
                weatherIcon = R.drawable.wi_direction_up;
                break;
            case WeatherIcons.DIRECTION_DOWN:
                weatherIcon = R.drawable.wi_direction_down;
                break;

            // Beaufort
            case WeatherIcons.WIND_BEAUFORT_0:
                weatherIcon = R.drawable.wi_wind_beaufort_0;
                break;
            case WeatherIcons.WIND_BEAUFORT_1:
                weatherIcon = R.drawable.wi_wind_beaufort_1;
                break;
            case WeatherIcons.WIND_BEAUFORT_2:
                weatherIcon = R.drawable.wi_wind_beaufort_2;
                break;
            case WeatherIcons.WIND_BEAUFORT_3:
                weatherIcon = R.drawable.wi_wind_beaufort_3;
                break;
            case WeatherIcons.WIND_BEAUFORT_4:
                weatherIcon = R.drawable.wi_wind_beaufort_4;
                break;
            case WeatherIcons.WIND_BEAUFORT_5:
                weatherIcon = R.drawable.wi_wind_beaufort_5;
                break;
            case WeatherIcons.WIND_BEAUFORT_6:
                weatherIcon = R.drawable.wi_wind_beaufort_6;
                break;
            case WeatherIcons.WIND_BEAUFORT_7:
                weatherIcon = R.drawable.wi_wind_beaufort_7;
                break;
            case WeatherIcons.WIND_BEAUFORT_8:
                weatherIcon = R.drawable.wi_wind_beaufort_8;
                break;
            case WeatherIcons.WIND_BEAUFORT_9:
                weatherIcon = R.drawable.wi_wind_beaufort_9;
                break;
            case WeatherIcons.WIND_BEAUFORT_10:
                weatherIcon = R.drawable.wi_wind_beaufort_10;
                break;
            case WeatherIcons.WIND_BEAUFORT_11:
                weatherIcon = R.drawable.wi_wind_beaufort_11;
                break;
            case WeatherIcons.WIND_BEAUFORT_12:
                weatherIcon = R.drawable.wi_wind_beaufort_12;
                break;

            // Moon Phase
            case WeatherIcons.MOON_NEW:
                weatherIcon = R.drawable.wi_moon_new;
                break;
            case WeatherIcons.MOON_WAXING_CRESCENT_3:
                weatherIcon = R.drawable.wi_moon_waxing_crescent_3;
                break;
            case WeatherIcons.MOON_FIRST_QUARTER:
                weatherIcon = R.drawable.wi_moon_first_quarter;
                break;
            case WeatherIcons.MOON_WAXING_GIBBOUS_3:
                weatherIcon = R.drawable.wi_moon_waxing_gibbous_3;
                break;
            case WeatherIcons.MOON_FULL:
                weatherIcon = R.drawable.wi_moon_full;
                break;
            case WeatherIcons.MOON_WANING_GIBBOUS_3:
                weatherIcon = R.drawable.wi_moon_waning_gibbous_3;
                break;
            case WeatherIcons.MOON_THIRD_QUARTER:
                weatherIcon = R.drawable.wi_moon_third_quarter;
                break;
            case WeatherIcons.MOON_WANING_CRESCENT_3:
                weatherIcon = R.drawable.wi_moon_waning_crescent_3;
                break;

            case WeatherIcons.MOON_ALT_NEW:
                weatherIcon = R.drawable.wi_moon_alt_new;
                break;
            case WeatherIcons.MOON_ALT_WAXING_CRESCENT_3:
                weatherIcon = R.drawable.wi_moon_alt_waxing_crescent_3;
                break;
            case WeatherIcons.MOON_ALT_FIRST_QUARTER:
                weatherIcon = R.drawable.wi_moon_alt_first_quarter;
                break;
            case WeatherIcons.MOON_ALT_WAXING_GIBBOUS_3:
                weatherIcon = R.drawable.wi_moon_alt_waxing_gibbous_3;
                break;
            case WeatherIcons.MOON_ALT_FULL:
                weatherIcon = R.drawable.wi_moon_alt_full;
                break;
            case WeatherIcons.MOON_ALT_WANING_GIBBOUS_3:
                weatherIcon = R.drawable.wi_moon_alt_waning_gibbous_3;
                break;
            case WeatherIcons.MOON_ALT_THIRD_QUARTER:
                weatherIcon = R.drawable.wi_moon_alt_third_quarter;
                break;
            case WeatherIcons.MOON_ALT_WANING_CRESCENT_3:
                weatherIcon = R.drawable.wi_moon_alt_waning_crescent_3;
                break;

            case WeatherIcons.FAHRENHEIT:
                weatherIcon = R.drawable.wi_fahrenheit;
                break;
            case WeatherIcons.CELSIUS:
                weatherIcon = R.drawable.wi_celsius;
                break;

            case WeatherIcons.NA:
                weatherIcon = R.drawable.wi_na;
                break;
        }

        if (weatherIcon == -1) {
            // Not Available
            weatherIcon = R.drawable.wi_na;
        }

        return weatherIcon;
    }

    @WorkerThread
    public static ImageDataViewModel getImageData(@NonNull Weather weather) {
        String icon = weather.getCondition().getIcon();
        String backgroundCode;
        WeatherManager wm = WeatherManager.getInstance();

        // Apply background based on weather condition
        switch (icon) {
            // Rain
            case WeatherIcons.DAY_RAIN:
            case WeatherIcons.DAY_RAIN_MIX:
            case WeatherIcons.DAY_RAIN_WIND:
            case WeatherIcons.DAY_SHOWERS:
            case WeatherIcons.DAY_SLEET:
            case WeatherIcons.DAY_SPRINKLE:
                backgroundCode = WeatherBackground.RAIN;
                break;

            case WeatherIcons.NIGHT_ALT_HAIL:
            case WeatherIcons.NIGHT_ALT_RAIN:
            case WeatherIcons.NIGHT_ALT_RAIN_MIX:
            case WeatherIcons.NIGHT_ALT_RAIN_WIND:
            case WeatherIcons.NIGHT_ALT_SHOWERS:
            case WeatherIcons.NIGHT_ALT_SLEET:
            case WeatherIcons.NIGHT_ALT_SPRINKLE:
            case WeatherIcons.RAIN:
            case WeatherIcons.RAIN_MIX:
            case WeatherIcons.RAIN_WIND:
            case WeatherIcons.SHOWERS:
            case WeatherIcons.SLEET:
            case WeatherIcons.SPRINKLE:
                backgroundCode = WeatherBackground.RAIN_NIGHT;
                break;

            // Tornado / Hurricane / Thunderstorm / Tropical Storm
            case WeatherIcons.DAY_LIGHTNING:
            case WeatherIcons.DAY_THUNDERSTORM:
            case WeatherIcons.NIGHT_ALT_LIGHTNING:
            case WeatherIcons.NIGHT_ALT_THUNDERSTORM:
            case WeatherIcons.LIGHTNING:
            case WeatherIcons.THUNDERSTORM:
                backgroundCode = WeatherBackground.TSTORMS_NIGHT;
                break;

            case WeatherIcons.DAY_STORM_SHOWERS:
            case WeatherIcons.DAY_SLEET_STORM:
            case WeatherIcons.STORM_SHOWERS:
            case WeatherIcons.NIGHT_ALT_STORM_SHOWERS:
            case WeatherIcons.NIGHT_ALT_SLEET_STORM:
            case WeatherIcons.HAIL:
            case WeatherIcons.HURRICANE:
            case WeatherIcons.TORNADO:
                backgroundCode = WeatherBackground.STORMS;
                break;

            // Dust
            case WeatherIcons.DUST:
            case WeatherIcons.SANDSTORM:
                backgroundCode = WeatherBackground.DUST;
                break;

            // Foggy / Haze
            case WeatherIcons.DAY_FOG:
            case WeatherIcons.DAY_HAZE:
            case WeatherIcons.FOG:
            case WeatherIcons.NIGHT_FOG:
            case WeatherIcons.SMOG:
            case WeatherIcons.SMOKE:
                backgroundCode = WeatherBackground.FOG;
                break;

            // Snow / Snow Showers/Storm
            case WeatherIcons.DAY_SNOW:
            case WeatherIcons.DAY_SNOW_THUNDERSTORM:
            case WeatherIcons.NIGHT_ALT_SNOW:
            case WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM:
            case WeatherIcons.SNOW:
            case WeatherIcons.SNOW_WIND:
            case WeatherIcons.DAY_SNOW_WIND:
            case WeatherIcons.NIGHT_ALT_SNOW_WIND:
                backgroundCode = WeatherBackground.SNOW;
                break;

            /* Ambigious weather conditions */
            // (Mostly) Cloudy
            case WeatherIcons.CLOUD:
            case WeatherIcons.CLOUDY:
            case WeatherIcons.CLOUDY_GUSTS:
            case WeatherIcons.CLOUDY_WINDY:
            case WeatherIcons.DAY_CLOUDY:
            case WeatherIcons.DAY_CLOUDY_GUSTS:
            case WeatherIcons.DAY_CLOUDY_HIGH:
            case WeatherIcons.DAY_CLOUDY_WINDY:
            case WeatherIcons.NIGHT_ALT_CLOUDY:
            case WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS:
            case WeatherIcons.NIGHT_ALT_CLOUDY_HIGH:
            case WeatherIcons.NIGHT_ALT_CLOUDY_WINDY:
                if (wm.isNight(weather))
                    backgroundCode = WeatherBackground.MOSTLYCLOUDY_NIGHT;
                else
                    backgroundCode = WeatherBackground.MOSTLYCLOUDY_DAY;
                break;
            // Partly Cloudy
            case WeatherIcons.DAY_SUNNY_OVERCAST:
            case WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY:
                if (wm.isNight(weather))
                    backgroundCode = WeatherBackground.PARTLYCLOUDY_NIGHT;
                else
                    backgroundCode = WeatherBackground.PARTLYCLOUDY_DAY;
                break;

            case WeatherIcons.DAY_SUNNY:
            case WeatherIcons.NA:
            case WeatherIcons.NIGHT_CLEAR:
            case WeatherIcons.SNOWFLAKE_COLD:
            case WeatherIcons.DAY_HOT:
            case WeatherIcons.WINDY:
            case WeatherIcons.STRONG_WIND:
            default:
                // Set background based using sunset/rise times
                if (wm.isNight(weather))
                    backgroundCode = WeatherBackground.NIGHT;
                else
                    backgroundCode = WeatherBackground.DAY;
                break;
        }

        // Check cache for image data
        ImageDataHelperImpl imageHelper = ImageDataHelper.getImageDataHelper();
        ImageData imageData = imageHelper.getCachedImageData(backgroundCode);
        // Check if cache is available and valid
        if (imageData != null && imageData.isValid()) {
            return new ImageDataViewModel(imageData);
        } else {
            if (!FeatureSettings.isUpdateAvailable()) {
                imageData = imageHelper.getRemoteImageData(backgroundCode);
                if (imageData != null && imageData.isValid()) {
                    return new ImageDataViewModel(imageData);
                } else {
                    imageData = imageHelper.getDefaultImageData(backgroundCode, weather);
                    if (imageData != null && imageData.isValid())
                        return new ImageDataViewModel(imageData);
                }
            } else {
                imageData = imageHelper.getDefaultImageData(backgroundCode, weather);
                if (imageData != null && imageData.isValid())
                    return new ImageDataViewModel(imageData);
            }
        }

        return null;
    }

    @ColorInt
    public static int getWeatherBackgroundColor(Weather weather) {
        int rgb = -1;
        String icon = weather.getCondition().getIcon();
        WeatherManager wm = WeatherManager.getInstance();

        // Apply background based on weather condition
        switch (icon) {
            // Rain
            case WeatherIcons.DAY_RAIN:
            case WeatherIcons.DAY_RAIN_MIX:
            case WeatherIcons.DAY_RAIN_WIND:
            case WeatherIcons.DAY_SHOWERS:
            case WeatherIcons.DAY_SLEET:
            case WeatherIcons.DAY_SPRINKLE:
                rgb = 0xFF475374;
                break;
            case WeatherIcons.NIGHT_ALT_HAIL:
            case WeatherIcons.NIGHT_ALT_RAIN:
            case WeatherIcons.NIGHT_ALT_RAIN_MIX:
            case WeatherIcons.NIGHT_ALT_RAIN_WIND:
            case WeatherIcons.NIGHT_ALT_SHOWERS:
            case WeatherIcons.NIGHT_ALT_SLEET:
            case WeatherIcons.NIGHT_ALT_SPRINKLE:
            case WeatherIcons.RAIN:
            case WeatherIcons.RAIN_MIX:
            case WeatherIcons.RAIN_WIND:
            case WeatherIcons.SHOWERS:
            case WeatherIcons.SLEET:
            case WeatherIcons.SPRINKLE:
                rgb = 0xFF181010;
                break;
            // Tornado / Hurricane / Thunderstorm / Tropical Storm
            case WeatherIcons.DAY_LIGHTNING:
            case WeatherIcons.DAY_THUNDERSTORM:
                rgb = 0xFF283848;
                break;
            case WeatherIcons.NIGHT_ALT_LIGHTNING:
            case WeatherIcons.NIGHT_ALT_THUNDERSTORM:
            case WeatherIcons.LIGHTNING:
            case WeatherIcons.THUNDERSTORM:
                rgb = 0xFF181830;
                break;
            case WeatherIcons.DAY_STORM_SHOWERS:
            case WeatherIcons.DAY_SLEET_STORM:
            case WeatherIcons.STORM_SHOWERS:
            case WeatherIcons.NIGHT_ALT_STORM_SHOWERS:
            case WeatherIcons.NIGHT_ALT_SLEET_STORM:
            case WeatherIcons.HAIL:
            case WeatherIcons.HURRICANE:
            case WeatherIcons.TORNADO:
                rgb = 0xFF182830;
                break;
            // Dust
            case WeatherIcons.DUST:
            case WeatherIcons.SANDSTORM:
                rgb = 0xFFB06810;
                break;
            // Foggy / Haze
            case WeatherIcons.DAY_FOG:
            case WeatherIcons.DAY_HAZE:
            case WeatherIcons.FOG:
            case WeatherIcons.NIGHT_FOG:
            case WeatherIcons.SMOG:
            case WeatherIcons.SMOKE:
                rgb = 0xFF252524;
                break;
            // Snow / Snow Showers/Storm
            case WeatherIcons.DAY_SNOW:
            case WeatherIcons.DAY_SNOW_THUNDERSTORM:
            case WeatherIcons.NIGHT_ALT_SNOW:
            case WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM:
            case WeatherIcons.SNOW:
                rgb = 0xFF646464;
                break;
            case WeatherIcons.SNOW_WIND:
            case WeatherIcons.DAY_SNOW_WIND:
            case WeatherIcons.NIGHT_ALT_SNOW_WIND:
                rgb = 0xFF545454;
                break;
            /* Ambigious weather conditions */
            // (Mostly) Cloudy
            case WeatherIcons.CLOUD:
            case WeatherIcons.CLOUDY:
            case WeatherIcons.CLOUDY_GUSTS:
            case WeatherIcons.CLOUDY_WINDY:
            case WeatherIcons.DAY_CLOUDY:
            case WeatherIcons.DAY_CLOUDY_GUSTS:
            case WeatherIcons.DAY_CLOUDY_HIGH:
            case WeatherIcons.DAY_CLOUDY_WINDY:
            case WeatherIcons.NIGHT_ALT_CLOUDY:
            case WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS:
            case WeatherIcons.NIGHT_ALT_CLOUDY_HIGH:
            case WeatherIcons.NIGHT_ALT_CLOUDY_WINDY:
                if (wm.isNight(weather))
                    rgb = 0xFF182020;
                else
                    rgb = 0xFF5080A8;
                break;
            // Partly Cloudy
            case WeatherIcons.DAY_SUNNY_OVERCAST:
            case WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY:
                if (wm.isNight(weather))
                    rgb = 0xFF181820;
                else
                    rgb = 0xFF256AAD;
                break;
            case WeatherIcons.DAY_SUNNY:
            case WeatherIcons.NA:
            case WeatherIcons.NIGHT_CLEAR:
            case WeatherIcons.SNOWFLAKE_COLD:
            case WeatherIcons.DAY_HOT:
            case WeatherIcons.WINDY:
            case WeatherIcons.STRONG_WIND:
            default:
                // Set background based using sunset/rise times
                if (wm.isNight(weather))
                    rgb = 0xFF181018;
                else
                    rgb = 0xFF20A8D8;
                break;
        }

        return rgb;
    }

    public static Beaufort.BeaufortScale getBeaufortScale(int mph) {
        if (mph >= 1 && mph <= 3) {
            return Beaufort.BeaufortScale.B1;
        } else if (mph >= 4 && mph <= 7) {
            return Beaufort.BeaufortScale.B2;
        } else if (mph >= 8 && mph <= 12) {
            return Beaufort.BeaufortScale.B3;
        } else if (mph >= 13 && mph <= 18) {
            return Beaufort.BeaufortScale.B4;
        } else if (mph >= 19 && mph <= 24) {
            return Beaufort.BeaufortScale.B5;
        } else if (mph >= 25 && mph <= 31) {
            return Beaufort.BeaufortScale.B6;
        } else if (mph >= 32 && mph <= 38) {
            return Beaufort.BeaufortScale.B7;
        } else if (mph >= 39 && mph <= 46) {
            return Beaufort.BeaufortScale.B8;
        } else if (mph >= 47 && mph <= 54) {
            return Beaufort.BeaufortScale.B9;
        } else if (mph >= 55 && mph <= 63) {
            return Beaufort.BeaufortScale.B10;
        } else if (mph >= 64 && mph <= 72) {
            return Beaufort.BeaufortScale.B11;
        } else if (mph >= 73) {
            return Beaufort.BeaufortScale.B12;
        } else {
            return Beaufort.BeaufortScale.B0;
        }
    }

    public static Beaufort.BeaufortScale getBeaufortScale(float mps) {
        mps = new BigDecimal(mps).setScale(1, RoundingMode.HALF_UP).floatValue();

        if (mps >= 0.5f && mps <= 1.5f) {
            return Beaufort.BeaufortScale.B1;
        } else if (mps >= 1.6f && mps <= 3.3f) {
            return Beaufort.BeaufortScale.B2;
        } else if (mps >= 3.4f && mps <= 5.5f) {
            return Beaufort.BeaufortScale.B3;
        } else if (mps >= 5.5f && mps <= 7.9f) {
            return Beaufort.BeaufortScale.B4;
        } else if (mps >= 8f && mps <= 10.7f) {
            return Beaufort.BeaufortScale.B5;
        } else if (mps >= 10.8f && mps <= 13.8f) {
            return Beaufort.BeaufortScale.B6;
        } else if (mps >= 13.9f && mps <= 17.1f) {
            return Beaufort.BeaufortScale.B7;
        } else if (mps >= 17.2f && mps <= 20.7f) {
            return Beaufort.BeaufortScale.B8;
        } else if (mps >= 20.8f && mps <= 24.4f) {
            return Beaufort.BeaufortScale.B9;
        } else if (mps >= 24.5 && mps <= 28.4f) {
            return Beaufort.BeaufortScale.B10;
        } else if (mps >= 28.5f && mps <= 32.6f) {
            return Beaufort.BeaufortScale.B11;
        } else if (mps >= 32.7f) {
            return Beaufort.BeaufortScale.B12;
        } else {
            return Beaufort.BeaufortScale.B0;
        }
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
            lat = Double.parseDouble(coord[0]);
            _long = Double.parseDouble(coord[1]);
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

        @NonNull
        @Override
        public String toString() {
            return String.format(Locale.ROOT, "%s,%s", Double.toString(lat), Double.toString(_long));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Coordinate that = (Coordinate) o;

            if (Double.compare(that.lat, lat) != 0) return false;
            return Double.compare(that._long, _long) == 0;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(lat);
            result = (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(_long);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
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
