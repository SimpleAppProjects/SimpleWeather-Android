package com.thewizrd.shared_resources.icons

import androidx.annotation.DrawableRes
import com.thewizrd.shared_resources.R

class WeatherIconicProvider : WeatherIconProvider() {
    override fun getKey(): String {
        return "w-iconic-jackd248"
    }

    override fun getDisplayName(): String {
        return "Weather Iconic"
    }

    override fun getAuthorName(): String {
        return "jackd248"
    }

    override fun getAttributionLink(): String {
        return "https://github.com/jackd248/weather-iconic"
    }

    override fun isFontIcon(): Boolean {
        return true
    }

    @DrawableRes
    override fun getWeatherIconResource(icon: String): Int {
        var weatherIcon = -1
        when (icon) {
            WeatherIcons.DAY_SUNNY -> weatherIcon = R.drawable.wic_sun
            WeatherIcons.DAY_CLOUDY -> weatherIcon = R.drawable.wic_sun_cloud
            WeatherIcons.DAY_CLOUDY_GUSTS -> weatherIcon = R.drawable.wic_sun_cloud_wind
            WeatherIcons.DAY_CLOUDY_WINDY -> weatherIcon = R.drawable.wic_sun_cloud_wind
            WeatherIcons.DAY_FOG -> weatherIcon = R.drawable.wic_sun_fog
            WeatherIcons.DAY_HAIL -> weatherIcon = R.drawable.wic_hail
            WeatherIcons.DAY_HAZE -> weatherIcon = R.drawable.wic_sun_fog
            WeatherIcons.DAY_LIGHTNING -> weatherIcon = R.drawable.wic_sun_cloud_lightning
            WeatherIcons.DAY_RAIN -> weatherIcon = R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_RAIN_MIX -> weatherIcon = R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_RAIN_WIND -> weatherIcon = R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_SHOWERS -> weatherIcon = R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_SLEET -> weatherIcon = R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_SLEET_STORM -> weatherIcon = R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_SNOW -> weatherIcon = R.drawable.wic_sun_cloud_snow
            WeatherIcons.DAY_SNOW_THUNDERSTORM -> weatherIcon = R.drawable.wic_sun_cloud_snow
            WeatherIcons.DAY_SNOW_WIND -> weatherIcon = R.drawable.wic_sun_cloud_snow
            WeatherIcons.DAY_SPRINKLE -> weatherIcon = R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_STORM_SHOWERS -> weatherIcon = R.drawable.wic_sun_cloud_lightning
            WeatherIcons.DAY_SUNNY_OVERCAST -> weatherIcon = R.drawable.wic_sun_cloud
            WeatherIcons.DAY_THUNDERSTORM -> weatherIcon = R.drawable.wic_sun_cloud_lightning
            WeatherIcons.DAY_WINDY -> weatherIcon = R.drawable.wic_sun_cloud_wind
            WeatherIcons.DAY_HOT -> weatherIcon = R.drawable.wic_sun
            WeatherIcons.DAY_CLOUDY_HIGH -> weatherIcon = R.drawable.wic_sun_cloud
            WeatherIcons.DAY_LIGHT_WIND -> weatherIcon = R.drawable.wic_sun_cloud_wind

            WeatherIcons.NIGHT_CLEAR -> weatherIcon = R.drawable.wic_moon
            WeatherIcons.NIGHT_ALT_CLOUDY -> weatherIcon = R.drawable.wic_moon_cloud
            WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS -> weatherIcon = R.drawable.wic_moon_cloud_wind
            WeatherIcons.NIGHT_ALT_CLOUDY_WINDY -> weatherIcon = R.drawable.wic_moon_cloud_wind
            WeatherIcons.NIGHT_ALT_HAIL -> weatherIcon = R.drawable.wic_hail
            WeatherIcons.NIGHT_ALT_LIGHTNING -> weatherIcon = R.drawable.wic_moon_cloud_lightning
            WeatherIcons.NIGHT_ALT_RAIN -> weatherIcon = R.drawable.wic_moon_cloud_rain
            WeatherIcons.NIGHT_ALT_RAIN_MIX -> weatherIcon = R.drawable.wic_moon_cloud_rain
            WeatherIcons.NIGHT_ALT_RAIN_WIND -> weatherIcon = R.drawable.wic_moon_cloud_rain
            WeatherIcons.NIGHT_ALT_SHOWERS -> weatherIcon = R.drawable.wic_moon_cloud_rain
            WeatherIcons.NIGHT_ALT_SLEET -> weatherIcon = R.drawable.wic_moon_cloud_rain
            WeatherIcons.NIGHT_ALT_SLEET_STORM -> weatherIcon = R.drawable.wic_moon_cloud_rain
            WeatherIcons.NIGHT_ALT_SNOW -> weatherIcon = R.drawable.wic_moon_cloud_snow
            WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM -> weatherIcon = R.drawable.wic_moon_cloud_snow
            WeatherIcons.NIGHT_ALT_SNOW_WIND -> weatherIcon = R.drawable.wic_moon_cloud_snow
            WeatherIcons.NIGHT_ALT_SPRINKLE -> weatherIcon = R.drawable.wic_moon_cloud_rain
            WeatherIcons.NIGHT_ALT_STORM_SHOWERS -> weatherIcon =
                R.drawable.wic_moon_cloud_lightning
            WeatherIcons.NIGHT_ALT_THUNDERSTORM -> weatherIcon = R.drawable.wic_moon_cloud_lightning
            WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY -> weatherIcon = R.drawable.wic_moon_cloud
            WeatherIcons.NIGHT_ALT_CLOUDY_HIGH -> weatherIcon = R.drawable.wic_moon_cloud
            WeatherIcons.NIGHT_FOG -> weatherIcon = R.drawable.wic_moon_fog

            WeatherIcons.CLOUD -> weatherIcon = R.drawable.wic_cloud
            WeatherIcons.CLOUDY -> weatherIcon = R.drawable.wic_clouds
            WeatherIcons.CLOUDY_GUSTS -> weatherIcon = R.drawable.wic_cloud_wind
            WeatherIcons.CLOUDY_WINDY -> weatherIcon = R.drawable.wic_cloud_wind
            WeatherIcons.FOG -> weatherIcon = R.drawable.wic_fog
            WeatherIcons.HAIL -> weatherIcon = R.drawable.wic_hail
            WeatherIcons.RAIN -> weatherIcon = R.drawable.wic_cloud_rain
            WeatherIcons.RAIN_MIX -> weatherIcon = R.drawable.wic_cloud_rain
            WeatherIcons.RAIN_WIND -> weatherIcon = R.drawable.wic_cloud_rain
            WeatherIcons.SHOWERS -> weatherIcon = R.drawable.wic_cloud_rain
            WeatherIcons.SLEET -> weatherIcon = R.drawable.wic_cloud_rain
            WeatherIcons.SNOW -> weatherIcon = R.drawable.wic_cloud_snow
            WeatherIcons.SPRINKLE -> weatherIcon = R.drawable.wic_cloud_rain_single
            WeatherIcons.STORM_SHOWERS -> weatherIcon = R.drawable.wic_lightning
            WeatherIcons.THUNDERSTORM -> weatherIcon = R.drawable.wic_lightning
            WeatherIcons.SNOW_WIND -> weatherIcon = R.drawable.wic_cloud_snow
            WeatherIcons.SMOG -> weatherIcon = R.drawable.wi_smog
            WeatherIcons.SMOKE -> weatherIcon = R.drawable.wi_smoke
            WeatherIcons.LIGHTNING -> weatherIcon = R.drawable.wic_lightning
            WeatherIcons.DUST -> weatherIcon = R.drawable.wi_dust
            WeatherIcons.SNOWFLAKE_COLD -> weatherIcon = R.drawable.wic_snowflake
            WeatherIcons.WINDY -> weatherIcon = R.drawable.wic_wind
            WeatherIcons.STRONG_WIND -> weatherIcon = R.drawable.wic_wind_high
            WeatherIcons.SANDSTORM -> weatherIcon = R.drawable.wi_sandstorm
            WeatherIcons.HURRICANE -> weatherIcon = R.drawable.wi_hurricane
            WeatherIcons.TORNADO -> weatherIcon = R.drawable.wic_tornado
            WeatherIcons.FIRE -> weatherIcon = R.drawable.wi_fire
            WeatherIcons.FLOOD -> weatherIcon = R.drawable.wi_flood
            WeatherIcons.VOLCANO -> weatherIcon = R.drawable.wi_volcano
            WeatherIcons.BAROMETER -> weatherIcon = R.drawable.wic_barometer
            WeatherIcons.HUMIDITY -> weatherIcon = R.drawable.wic_raindrop
            WeatherIcons.MOONRISE -> weatherIcon = R.drawable.wi_moonrise
            WeatherIcons.MOONSET -> weatherIcon = R.drawable.wi_moonset
            WeatherIcons.RAINDROP -> weatherIcon = R.drawable.wi_raindrop
            WeatherIcons.RAINDROPS -> weatherIcon = R.drawable.wi_raindrops
            WeatherIcons.SUNRISE -> weatherIcon = R.drawable.wic_sunrise
            WeatherIcons.SUNSET -> weatherIcon = R.drawable.wic_sunset
            WeatherIcons.THERMOMETER -> weatherIcon = R.drawable.wic_thermometer_medium
            WeatherIcons.UMBRELLA -> weatherIcon = R.drawable.wic_umbrella
            WeatherIcons.WIND_DIRECTION -> weatherIcon = R.drawable.wic_compass
            WeatherIcons.DIRECTION_UP -> weatherIcon = R.drawable.wi_direction_up
            WeatherIcons.DIRECTION_DOWN -> weatherIcon = R.drawable.wi_direction_down

            WeatherIcons.WIND_BEAUFORT_0 -> weatherIcon = R.drawable.wi_wind_beaufort_0
            WeatherIcons.WIND_BEAUFORT_1 -> weatherIcon = R.drawable.wi_wind_beaufort_1
            WeatherIcons.WIND_BEAUFORT_2 -> weatherIcon = R.drawable.wi_wind_beaufort_2
            WeatherIcons.WIND_BEAUFORT_3 -> weatherIcon = R.drawable.wi_wind_beaufort_3
            WeatherIcons.WIND_BEAUFORT_4 -> weatherIcon = R.drawable.wi_wind_beaufort_4
            WeatherIcons.WIND_BEAUFORT_5 -> weatherIcon = R.drawable.wi_wind_beaufort_5
            WeatherIcons.WIND_BEAUFORT_6 -> weatherIcon = R.drawable.wi_wind_beaufort_6
            WeatherIcons.WIND_BEAUFORT_7 -> weatherIcon = R.drawable.wi_wind_beaufort_7
            WeatherIcons.WIND_BEAUFORT_8 -> weatherIcon = R.drawable.wi_wind_beaufort_8
            WeatherIcons.WIND_BEAUFORT_9 -> weatherIcon = R.drawable.wi_wind_beaufort_9
            WeatherIcons.WIND_BEAUFORT_10 -> weatherIcon = R.drawable.wi_wind_beaufort_10
            WeatherIcons.WIND_BEAUFORT_11 -> weatherIcon = R.drawable.wi_wind_beaufort_11
            WeatherIcons.WIND_BEAUFORT_12 -> weatherIcon = R.drawable.wi_wind_beaufort_12

            WeatherIcons.MOON_NEW -> weatherIcon = R.drawable.wic_moon_fullmoon
            WeatherIcons.MOON_WAXING_CRESCENT_3 -> weatherIcon = R.drawable.wic_moon_waxing_crescent
            WeatherIcons.MOON_FIRST_QUARTER -> weatherIcon = R.drawable.wic_moon_first_quarter
            WeatherIcons.MOON_WAXING_GIBBOUS_3 -> weatherIcon = R.drawable.wic_moon_waxing_gibbous
            WeatherIcons.MOON_FULL -> weatherIcon = R.drawable.wic_moon_newmoon
            WeatherIcons.MOON_WANING_GIBBOUS_3 -> weatherIcon = R.drawable.wic_moon_waning_gibbous
            WeatherIcons.MOON_THIRD_QUARTER -> weatherIcon = R.drawable.wic_moon_last_quarter
            WeatherIcons.MOON_WANING_CRESCENT_3 -> weatherIcon = R.drawable.wic_moon_waning_crescent

            WeatherIcons.MOON_ALT_NEW -> weatherIcon = R.drawable.wic_moon_fullmoon
            WeatherIcons.MOON_ALT_WAXING_CRESCENT_3 -> weatherIcon =
                R.drawable.wic_moon_waxing_crescent
            WeatherIcons.MOON_ALT_FIRST_QUARTER -> weatherIcon = R.drawable.wic_moon_first_quarter
            WeatherIcons.MOON_ALT_WAXING_GIBBOUS_3 -> weatherIcon =
                R.drawable.wic_moon_waxing_gibbous
            WeatherIcons.MOON_ALT_FULL -> weatherIcon = R.drawable.wic_moon_newmoon
            WeatherIcons.MOON_ALT_WANING_GIBBOUS_3 -> weatherIcon =
                R.drawable.wic_moon_waning_gibbous
            WeatherIcons.MOON_ALT_THIRD_QUARTER -> weatherIcon = R.drawable.wic_moon_last_quarter
            WeatherIcons.MOON_ALT_WANING_CRESCENT_3 -> weatherIcon =
                R.drawable.wic_moon_waning_crescent

            WeatherIcons.FAHRENHEIT -> weatherIcon = R.drawable.wic_fahrenheit
            WeatherIcons.CELSIUS -> weatherIcon = R.drawable.wic_celsius

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
            WeatherIcons.UV_INDEX_11 -> weatherIcon = R.drawable.wic_sun

            WeatherIcons.TREE_POLLEN -> weatherIcon = R.drawable.ic_outline_tree
            WeatherIcons.GRASS_POLLEN -> weatherIcon = R.drawable.ic_baseline_grass
            WeatherIcons.RAGWEED_POLLEN -> weatherIcon = R.drawable.ic_ragweed_pollen

            WeatherIcons.NA -> weatherIcon = R.drawable.wui_unknown
        }

        if (weatherIcon == -1) {
            // Not Available
            weatherIcon = R.drawable.wui_unknown
        }

        return weatherIcon
    }
}