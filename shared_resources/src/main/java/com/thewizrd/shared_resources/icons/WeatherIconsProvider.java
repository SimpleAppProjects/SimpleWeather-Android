package com.thewizrd.shared_resources.icons;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.R;

public final class WeatherIconsProvider extends WeatherIconProvider {
    public static final String KEY = "wi-erik-flowers";

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    public String getDisplayName() {
        return "Weather Icons";
    }

    @Override
    public String getAuthorName() {
        return "Erik Flowers";
    }

    @Override
    public String getAttributionLink() {
        return "https://erikflowers.github.io/weather-icons/";
    }

    @Override
    public boolean isFontIcon() {
        return true;
    }

    @Override
    @DrawableRes
    public int getWeatherIconResource(@NonNull String icon) {
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

            case WeatherIcons.UV_INDEX:
            case WeatherIcons.UV_INDEX_1:
            case WeatherIcons.UV_INDEX_2:
            case WeatherIcons.UV_INDEX_3:
            case WeatherIcons.UV_INDEX_4:
            case WeatherIcons.UV_INDEX_5:
            case WeatherIcons.UV_INDEX_6:
            case WeatherIcons.UV_INDEX_7:
            case WeatherIcons.UV_INDEX_8:
            case WeatherIcons.UV_INDEX_9:
            case WeatherIcons.UV_INDEX_10:
            case WeatherIcons.UV_INDEX_11:
                weatherIcon = R.drawable.wi_day_sunny;
                break;

            case WeatherIcons.TREE_POLLEN:
                weatherIcon = R.drawable.ic_outline_tree;
                break;
            case WeatherIcons.GRASS_POLLEN:
                weatherIcon = R.drawable.ic_baseline_grass;
                break;
            case WeatherIcons.RAGWEED_POLLEN:
                weatherIcon = R.drawable.ic_ragweed_pollen;
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
}
