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
        var weatherIcon = when (icon) {
            WeatherIcons.DAY_SUNNY -> R.drawable.wic_sun
            WeatherIcons.DAY_CLOUDY -> R.drawable.wic_sun_cloud
            WeatherIcons.DAY_CLOUDY_GUSTS -> R.drawable.wic_sun_cloud_wind
            WeatherIcons.DAY_CLOUDY_WINDY -> R.drawable.wic_sun_cloud_wind
            WeatherIcons.DAY_FOG -> R.drawable.wic_sun_fog
            WeatherIcons.DAY_HAIL -> R.drawable.wic_hail
            WeatherIcons.DAY_HAZE -> R.drawable.wic_sun_fog
            WeatherIcons.DAY_LIGHTNING -> R.drawable.wic_sun_cloud_lightning
            WeatherIcons.DAY_PARTLY_CLOUDY -> R.drawable.wic_sun_cloud
            WeatherIcons.DAY_RAIN -> R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_RAIN_MIX -> R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_RAIN_WIND -> R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_SHOWERS -> R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_SLEET -> R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_SLEET_STORM -> R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_SNOW -> R.drawable.wic_sun_cloud_snow
            WeatherIcons.DAY_SNOW_THUNDERSTORM -> R.drawable.wic_sun_cloud_snow
            WeatherIcons.DAY_SNOW_WIND -> R.drawable.wic_sun_cloud_snow
            WeatherIcons.DAY_SPRINKLE -> R.drawable.wic_sun_cloud_rain
            WeatherIcons.DAY_STORM_SHOWERS -> R.drawable.wic_sun_cloud_lightning
            WeatherIcons.DAY_SUNNY_OVERCAST -> R.drawable.wic_sun_cloud
            WeatherIcons.DAY_THUNDERSTORM -> R.drawable.wic_sun_cloud_lightning
            WeatherIcons.DAY_WINDY -> R.drawable.wic_wind
            WeatherIcons.DAY_HOT -> R.drawable.wic_sun
            WeatherIcons.DAY_CLOUDY_HIGH -> R.drawable.wic_sun_cloud
            WeatherIcons.DAY_LIGHT_WIND -> R.drawable.wic_wind

            WeatherIcons.NIGHT_CLEAR -> R.drawable.wic_moon
            WeatherIcons.NIGHT_ALT_CLOUDY -> R.drawable.wic_moon_cloud
            WeatherIcons.NIGHT_ALT_CLOUDY_GUSTS -> R.drawable.wic_moon_cloud_wind
            WeatherIcons.NIGHT_ALT_CLOUDY_WINDY -> R.drawable.wic_moon_cloud_wind
            WeatherIcons.NIGHT_ALT_HAIL -> R.drawable.wic_hail
            WeatherIcons.NIGHT_ALT_LIGHTNING -> R.drawable.wic_moon_cloud_lightning
            WeatherIcons.NIGHT_ALT_RAIN -> R.drawable.wic_moon_cloud_rain
            WeatherIcons.NIGHT_ALT_RAIN_MIX -> R.drawable.wic_moon_cloud_rain
            WeatherIcons.NIGHT_ALT_RAIN_WIND -> R.drawable.wic_moon_cloud_rain
            WeatherIcons.NIGHT_ALT_SHOWERS -> R.drawable.wic_moon_cloud_rain
            WeatherIcons.NIGHT_ALT_SLEET -> R.drawable.wic_moon_cloud_rain
            WeatherIcons.NIGHT_ALT_SLEET_STORM -> R.drawable.wic_moon_cloud_rain
            WeatherIcons.NIGHT_ALT_SNOW -> R.drawable.wic_moon_cloud_snow
            WeatherIcons.NIGHT_ALT_SNOW_THUNDERSTORM -> R.drawable.wic_moon_cloud_snow
            WeatherIcons.NIGHT_ALT_SNOW_WIND -> R.drawable.wic_moon_cloud_snow
            WeatherIcons.NIGHT_ALT_SPRINKLE -> R.drawable.wic_moon_cloud_rain
            WeatherIcons.NIGHT_ALT_STORM_SHOWERS -> R.drawable.wic_moon_cloud_lightning
            WeatherIcons.NIGHT_ALT_THUNDERSTORM -> R.drawable.wic_moon_cloud_lightning
            WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY -> R.drawable.wic_moon_cloud
            WeatherIcons.NIGHT_ALT_CLOUDY_HIGH -> R.drawable.wic_moon_cloud
            WeatherIcons.NIGHT_FOG -> R.drawable.wic_moon_fog
            WeatherIcons.NIGHT_OVERCAST -> R.drawable.wic_moon_cloud
            WeatherIcons.NIGHT_HAZE -> R.drawable.wic_moon_fog
            WeatherIcons.NIGHT_WINDY -> R.drawable.wic_wind
            WeatherIcons.NIGHT_HOT -> R.drawable.wic_moon
            WeatherIcons.NIGHT_LIGHT_WIND -> R.drawable.wic_wind

            WeatherIcons.CLOUD -> R.drawable.wic_cloud
            WeatherIcons.CLOUDY -> R.drawable.wic_clouds
            WeatherIcons.CLOUDY_GUSTS -> R.drawable.wic_cloud_wind
            WeatherIcons.CLOUDY_WINDY -> R.drawable.wic_cloud_wind
            WeatherIcons.FOG -> R.drawable.wic_fog
            WeatherIcons.HAIL -> R.drawable.wic_hail
            WeatherIcons.HAZE -> R.drawable.wic_fog
            WeatherIcons.HOT -> R.drawable.wic_thermometer_hot
            WeatherIcons.LIGHT_WIND -> R.drawable.wic_wind
            WeatherIcons.RAIN -> R.drawable.wic_cloud_rain
            WeatherIcons.RAIN_MIX -> R.drawable.wic_cloud_rain
            WeatherIcons.RAIN_WIND -> R.drawable.wic_cloud_rain
            WeatherIcons.OVERCAST -> R.drawable.wic_clouds
            WeatherIcons.SHOWERS -> R.drawable.wic_cloud_rain
            WeatherIcons.SLEET -> R.drawable.wic_cloud_rain
            WeatherIcons.SLEET_STORM -> R.drawable.wic_cloud_rain
            WeatherIcons.SNOW -> R.drawable.wic_cloud_snow
            WeatherIcons.SNOW_THUNDERSTORM -> R.drawable.wic_cloud_snow
            WeatherIcons.SPRINKLE -> R.drawable.wic_cloud_rain_single
            WeatherIcons.STORM_SHOWERS -> R.drawable.wic_lightning
            WeatherIcons.THUNDERSTORM -> R.drawable.wic_lightning
            WeatherIcons.SNOW_WIND -> R.drawable.wic_cloud_snow
            WeatherIcons.SMOG -> R.drawable.wi_smog
            WeatherIcons.SMOKE -> R.drawable.wi_smoke
            WeatherIcons.LIGHTNING -> R.drawable.wic_lightning
            WeatherIcons.DUST -> R.drawable.wi_dust
            WeatherIcons.SNOWFLAKE_COLD -> R.drawable.wic_snowflake
            WeatherIcons.WINDY -> R.drawable.wic_wind
            WeatherIcons.STRONG_WIND -> R.drawable.wic_wind_high
            WeatherIcons.SANDSTORM -> R.drawable.wi_sandstorm
            WeatherIcons.HURRICANE -> R.drawable.wi_hurricane
            WeatherIcons.TORNADO -> R.drawable.wic_tornado
            WeatherIcons.FIRE -> R.drawable.wi_fire
            WeatherIcons.FLOOD -> R.drawable.wi_flood
            WeatherIcons.VOLCANO -> R.drawable.wi_volcano
            WeatherIcons.BAROMETER -> R.drawable.wic_barometer
            WeatherIcons.HUMIDITY -> R.drawable.wic_raindrop
            WeatherIcons.MOONRISE -> R.drawable.wi_moonrise
            WeatherIcons.MOONSET -> R.drawable.wi_moonset
            WeatherIcons.RAINDROP -> R.drawable.wi_raindrop
            WeatherIcons.RAINDROPS -> R.drawable.wi_raindrops
            WeatherIcons.SUNRISE -> R.drawable.wic_sunrise
            WeatherIcons.SUNSET -> R.drawable.wic_sunset
            WeatherIcons.THERMOMETER -> R.drawable.wic_thermometer_medium
            WeatherIcons.UMBRELLA -> R.drawable.wic_umbrella
            WeatherIcons.WIND_DIRECTION -> R.drawable.wic_compass
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

            WeatherIcons.MOON_NEW -> R.drawable.wic_moon_fullmoon
            WeatherIcons.MOON_WAXING_CRESCENT_3 -> R.drawable.wic_moon_waxing_crescent
            WeatherIcons.MOON_FIRST_QUARTER -> R.drawable.wic_moon_first_quarter
            WeatherIcons.MOON_WAXING_GIBBOUS_3 -> R.drawable.wic_moon_waxing_gibbous
            WeatherIcons.MOON_FULL -> R.drawable.wic_moon_newmoon
            WeatherIcons.MOON_WANING_GIBBOUS_3 -> R.drawable.wic_moon_waning_gibbous
            WeatherIcons.MOON_THIRD_QUARTER -> R.drawable.wic_moon_last_quarter
            WeatherIcons.MOON_WANING_CRESCENT_3 -> R.drawable.wic_moon_waning_crescent

            WeatherIcons.MOON_ALT_NEW -> R.drawable.wic_moon_fullmoon
            WeatherIcons.MOON_ALT_WAXING_CRESCENT_3 -> R.drawable.wic_moon_waxing_crescent
            WeatherIcons.MOON_ALT_FIRST_QUARTER -> R.drawable.wic_moon_first_quarter
            WeatherIcons.MOON_ALT_WAXING_GIBBOUS_3 -> R.drawable.wic_moon_waxing_gibbous
            WeatherIcons.MOON_ALT_FULL -> R.drawable.wic_moon_newmoon
            WeatherIcons.MOON_ALT_WANING_GIBBOUS_3 -> R.drawable.wic_moon_waning_gibbous
            WeatherIcons.MOON_ALT_THIRD_QUARTER -> R.drawable.wic_moon_last_quarter
            WeatherIcons.MOON_ALT_WANING_CRESCENT_3 -> R.drawable.wic_moon_waning_crescent

            WeatherIcons.FAHRENHEIT -> R.drawable.wic_fahrenheit
            WeatherIcons.CELSIUS -> R.drawable.wic_celsius

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
            WeatherIcons.UV_INDEX_11 -> R.drawable.wic_sun

            WeatherIcons.TREE_POLLEN -> R.drawable.ic_outline_tree
            WeatherIcons.GRASS_POLLEN -> R.drawable.ic_baseline_grass
            WeatherIcons.RAGWEED_POLLEN -> R.drawable.ic_ragweed_pollen

            WeatherIcons.NA -> R.drawable.wui_unknown

            else -> -1
        }

        if (weatherIcon == -1) {
            // Not Available
            weatherIcon = R.drawable.wui_unknown
        }

        return weatherIcon
    }
}