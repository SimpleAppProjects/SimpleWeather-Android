package com.thewizrd.shared_resources.icons

import androidx.annotation.DrawableRes
import com.thewizrd.shared_resources.R

class WeatherIconsEFProvider : WeatherIconProvider() {
    companion object {
        const val KEY = "wi-erik-flowers"
    }

    override fun getKey(): String {
        return KEY
    }

    override fun getDisplayName(): String {
        return "Weather Icons"
    }

    override fun getAuthorName(): String {
        return "Erik Flowers"
    }

    override fun getAttributionLink(): String {
        return "https://erikflowers.github.io/weather-icons/"
    }

    override fun isFontIcon(): Boolean {
        return true
    }

    @DrawableRes
    override fun getWeatherIconResource(icon: String): Int {
        var weatherIcon = when (icon) {
            WeatherIcons.DAY_SUNNY -> R.drawable.wi_day_sunny
            WeatherIcons.DAY_CLOUDY -> R.drawable.wi_day_cloudy
            WeatherIcons.DAY_CLOUDY_GUSTS -> R.drawable.wi_day_cloudy_gusts
            WeatherIcons.DAY_CLOUDY_WINDY -> R.drawable.wi_day_cloudy_windy
            WeatherIcons.DAY_FOG -> R.drawable.wi_day_fog
            WeatherIcons.DAY_HAIL -> R.drawable.wi_day_hail
            WeatherIcons.DAY_HAZE -> R.drawable.wi_day_haze
            WeatherIcons.DAY_LIGHTNING -> R.drawable.wi_day_lightning
            WeatherIcons.DAY_RAIN -> R.drawable.wi_day_rain
            WeatherIcons.DAY_RAIN_MIX -> R.drawable.wi_day_rain_mix
            WeatherIcons.DAY_RAIN_WIND -> R.drawable.wi_day_rain_wind
            WeatherIcons.DAY_SHOWERS -> R.drawable.wi_day_showers
            WeatherIcons.DAY_SLEET -> R.drawable.wi_day_sleet
            WeatherIcons.DAY_SLEET_STORM -> R.drawable.wi_day_sleet_storm
            WeatherIcons.DAY_SNOW -> R.drawable.wi_day_snow
            WeatherIcons.DAY_SNOW_THUNDERSTORM -> R.drawable.wi_day_snow_thunderstorm
            WeatherIcons.DAY_SNOW_WIND -> R.drawable.wi_day_snow_wind
            WeatherIcons.DAY_SPRINKLE -> R.drawable.wi_day_sprinkle
            WeatherIcons.DAY_STORM_SHOWERS -> R.drawable.wi_day_storm_showers
            WeatherIcons.DAY_PARTLY_CLOUDY,
            WeatherIcons.DAY_SUNNY_OVERCAST -> R.drawable.wi_day_sunny_overcast
            WeatherIcons.DAY_THUNDERSTORM -> R.drawable.wi_day_thunderstorm
            WeatherIcons.DAY_WINDY -> R.drawable.wi_day_windy
            WeatherIcons.DAY_HOT -> R.drawable.wi_hot
            WeatherIcons.DAY_CLOUDY_HIGH -> R.drawable.wi_day_cloudy_high
            WeatherIcons.DAY_LIGHT_WIND -> R.drawable.wi_day_light_wind

            WeatherIcons.NIGHT_CLEAR -> R.drawable.wi_night_clear
            WeatherIcons.NIGHT_ALT_CLOUDY -> R.drawable.wi_night_alt_cloudy
            WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS -> R.drawable.wi_night_alt_cloudy_gusts
            WeatherIcons.NIGHT_ALT_CLOUDY_WINDY -> R.drawable.wi_night_alt_cloudy_windy
            WeatherIcons.NIGHT_ALT_HAIL -> R.drawable.wi_night_alt_hail
            WeatherIcons.NIGHT_ALT_LIGHTNING -> R.drawable.wi_night_alt_lightning
            WeatherIcons.NIGHT_ALT_RAIN -> R.drawable.wi_night_alt_rain
            WeatherIcons.NIGHT_ALT_RAIN_MIX -> R.drawable.wi_night_alt_rain_mix
            WeatherIcons.NIGHT_ALT_RAIN_WIND -> R.drawable.wi_night_alt_rain_wind
            WeatherIcons.NIGHT_ALT_SHOWERS -> R.drawable.wi_night_alt_showers
            WeatherIcons.NIGHT_ALT_SLEET -> R.drawable.wi_night_alt_sleet
            WeatherIcons.NIGHT_ALT_SLEET_STORM -> R.drawable.wi_night_alt_sleet_storm
            WeatherIcons.NIGHT_ALT_SNOW -> R.drawable.wi_night_alt_snow
            WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM -> R.drawable.wi_night_alt_snow_thunderstorm
            WeatherIcons.NIGHT_ALT_SNOW_WIND -> R.drawable.wi_night_alt_snow_wind
            WeatherIcons.NIGHT_ALT_SPRINKLE -> R.drawable.wi_night_alt_sprinkle
            WeatherIcons.NIGHT_ALT_STORM_SHOWERS -> R.drawable.wi_night_alt_storm_showers
            WeatherIcons.NIGHT_ALT_THUNDERSTORM -> R.drawable.wi_night_alt_thunderstorm
            WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY,
            WeatherIcons.NIGHT_OVERCAST -> R.drawable.wi_night_alt_partly_cloudy
            WeatherIcons.NIGHT_ALT_CLOUDY_HIGH -> R.drawable.wi_night_alt_cloudy_high
            WeatherIcons.NIGHT_FOG,
            WeatherIcons.NIGHT_HAZE -> R.drawable.wi_night_fog
            WeatherIcons.NIGHT_WINDY -> R.drawable.wi_windy
            WeatherIcons.NIGHT_HOT -> R.drawable.wi_thermometer_up
            WeatherIcons.NIGHT_LIGHT_WIND -> R.drawable.wi_windy

            WeatherIcons.CLOUD -> R.drawable.wi_cloud
            WeatherIcons.CLOUDY -> R.drawable.wi_cloudy
            WeatherIcons.CLOUDY_GUSTS -> R.drawable.wi_cloudy_gusts
            WeatherIcons.CLOUDY_WINDY -> R.drawable.wi_cloudy_windy
            WeatherIcons.FOG -> R.drawable.wi_fog
            WeatherIcons.HAIL -> R.drawable.wi_hail
            WeatherIcons.HAZE -> R.drawable.wi_windy
            WeatherIcons.HOT -> R.drawable.wi_thermometer_up
            WeatherIcons.LIGHT_WIND -> R.drawable.wi_windy
            WeatherIcons.OVERCAST -> R.drawable.wi_cloudy
            WeatherIcons.RAIN -> R.drawable.wi_rain
            WeatherIcons.RAIN_MIX -> R.drawable.wi_rain_mix
            WeatherIcons.RAIN_WIND -> R.drawable.wi_rain_wind
            WeatherIcons.SHOWERS -> R.drawable.wi_showers
            WeatherIcons.SLEET -> R.drawable.wi_sleet
            WeatherIcons.SLEET_STORM -> R.drawable.wi_sleet_storm
            WeatherIcons.SNOW -> R.drawable.wi_snow
            WeatherIcons.SNOW_THUNDERSTORM -> R.drawable.wi_snow_thunderstorm
            WeatherIcons.SPRINKLE -> R.drawable.wi_sprinkle
            WeatherIcons.STORM_SHOWERS -> R.drawable.wi_storm_showers
            WeatherIcons.THUNDERSTORM -> R.drawable.wi_thunderstorm
            WeatherIcons.SNOW_WIND -> R.drawable.wi_snow_wind
            WeatherIcons.SMOG -> R.drawable.wi_smog
            WeatherIcons.SMOKE -> R.drawable.wi_smoke
            WeatherIcons.LIGHTNING -> R.drawable.wi_lightning
            WeatherIcons.DUST -> R.drawable.wi_dust
            WeatherIcons.SNOWFLAKE_COLD -> R.drawable.wi_snowflake_cold
            WeatherIcons.WINDY -> R.drawable.wi_windy
            WeatherIcons.STRONG_WIND -> R.drawable.wi_strong_wind
            WeatherIcons.SANDSTORM -> R.drawable.wi_sandstorm
            WeatherIcons.HURRICANE -> R.drawable.wi_hurricane
            WeatherIcons.TORNADO -> R.drawable.wi_tornado
            WeatherIcons.FIRE -> R.drawable.wi_fire
            WeatherIcons.FLOOD -> R.drawable.wi_flood
            WeatherIcons.VOLCANO -> R.drawable.wi_volcano
            WeatherIcons.BAROMETER -> R.drawable.wi_barometer
            WeatherIcons.HUMIDITY -> R.drawable.wi_humidity
            WeatherIcons.MOONRISE -> R.drawable.wi_moonrise
            WeatherIcons.MOONSET -> R.drawable.wi_moonset
            WeatherIcons.RAINDROP -> R.drawable.wi_raindrop
            WeatherIcons.RAINDROPS -> R.drawable.wi_raindrops
            WeatherIcons.SUNRISE -> R.drawable.wi_sunrise
            WeatherIcons.SUNSET -> R.drawable.wi_sunset
            WeatherIcons.THERMOMETER -> R.drawable.wi_thermometer
            WeatherIcons.UMBRELLA -> R.drawable.wi_umbrella
            WeatherIcons.WIND_DIRECTION -> R.drawable.wi_wind_direction
            WeatherIcons.DIRECTION_UP -> R.drawable.wi_direction_up
            WeatherIcons.DIRECTION_DOWN -> R.drawable.wi_direction_down

            WeatherIcons.WIND_BEAUFORT_0 -> R.drawable.wi_wind_beaufort_0
            WeatherIcons.WIND_BEAUFORT_1 -> R.drawable.wi_wind_beaufort_1
            WeatherIcons.WIND_BEAUFORT_2 -> R.drawable.wi_wind_beaufort_2
            WeatherIcons.WIND_BEAUFORT_3 -> R.drawable.wi_wind_beaufort_3
            WeatherIcons.WIND_BEAUFORT_4 -> R.drawable.wi_wind_beaufort_4
            WeatherIcons.WIND_BEAUFORT_5 -> R.drawable.wi_wind_beaufort_5
            WeatherIcons.WIND_BEAUFORT_6 -> R.drawable.wi_wind_beaufort_6
            WeatherIcons.WIND_BEAUFORT_7 -> R.drawable.wi_wind_beaufort_7
            WeatherIcons.WIND_BEAUFORT_8 -> R.drawable.wi_wind_beaufort_8
            WeatherIcons.WIND_BEAUFORT_9 -> R.drawable.wi_wind_beaufort_9
            WeatherIcons.WIND_BEAUFORT_10 -> R.drawable.wi_wind_beaufort_10
            WeatherIcons.WIND_BEAUFORT_11 -> R.drawable.wi_wind_beaufort_11
            WeatherIcons.WIND_BEAUFORT_12 -> R.drawable.wi_wind_beaufort_12

            WeatherIcons.MOON_NEW -> R.drawable.wi_moon_new
            WeatherIcons.MOON_WAXING_CRESCENT_3 -> R.drawable.wi_moon_waxing_crescent_3
            WeatherIcons.MOON_FIRST_QUARTER -> R.drawable.wi_moon_first_quarter
            WeatherIcons.MOON_WAXING_GIBBOUS_3 -> R.drawable.wi_moon_waxing_gibbous_3
            WeatherIcons.MOON_FULL -> R.drawable.wi_moon_full
            WeatherIcons.MOON_WANING_GIBBOUS_3 -> R.drawable.wi_moon_waning_gibbous_3
            WeatherIcons.MOON_THIRD_QUARTER -> R.drawable.wi_moon_third_quarter
            WeatherIcons.MOON_WANING_CRESCENT_3 -> R.drawable.wi_moon_waning_crescent_3

            WeatherIcons.MOON_ALT_NEW -> R.drawable.wi_moon_alt_new
            WeatherIcons.MOON_ALT_WAXING_CRESCENT_3 -> R.drawable.wi_moon_alt_waxing_crescent_3
            WeatherIcons.MOON_ALT_FIRST_QUARTER -> R.drawable.wi_moon_alt_first_quarter
            WeatherIcons.MOON_ALT_WAXING_GIBBOUS_3 -> R.drawable.wi_moon_alt_waxing_gibbous_3
            WeatherIcons.MOON_ALT_FULL -> R.drawable.wi_moon_alt_full
            WeatherIcons.MOON_ALT_WANING_GIBBOUS_3 -> R.drawable.wi_moon_alt_waning_gibbous_3
            WeatherIcons.MOON_ALT_THIRD_QUARTER -> R.drawable.wi_moon_alt_third_quarter
            WeatherIcons.MOON_ALT_WANING_CRESCENT_3 -> R.drawable.wi_moon_alt_waning_crescent_3

            WeatherIcons.FAHRENHEIT -> R.drawable.wi_fahrenheit
            WeatherIcons.CELSIUS -> R.drawable.wi_celsius

            WeatherIcons.UV_INDEX,
            WeatherIcons.UV_INDEX_1,
            WeatherIcons.UV_INDEX_2,
            WeatherIcons.UV_INDEX_3,
            WeatherIcons.UV_INDEX_4,
            WeatherIcons.UV_INDEX_5,
            WeatherIcons.UV_INDEX_6,
            WeatherIcons.UV_INDEX_7,
            WeatherIcons.UV_INDEX_8,
            WeatherIcons.UV_INDEX_9,
            WeatherIcons.UV_INDEX_10,
            WeatherIcons.UV_INDEX_11 -> R.drawable.wi_day_sunny

            WeatherIcons.TREE_POLLEN -> R.drawable.ic_outline_tree
            WeatherIcons.GRASS_POLLEN -> R.drawable.ic_baseline_grass
            WeatherIcons.RAGWEED_POLLEN -> R.drawable.ic_ragweed_pollen

            WeatherIcons.NA -> R.drawable.wi_na

            else -> -1
        }

        if (weatherIcon == -1) {
            // Not Available
            weatherIcon = R.drawable.wi_na
        }

        return weatherIcon
    }
}