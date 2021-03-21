package com.thewizrd.shared_resources.controls;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.icons.WeatherIcons;
import com.thewizrd.shared_resources.icons.WeatherIconsManager;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.weatherdata.AirQuality;
import com.thewizrd.shared_resources.weatherdata.Beaufort;
import com.thewizrd.shared_resources.weatherdata.MoonPhase;
import com.thewizrd.shared_resources.weatherdata.UV;

public class DetailItemViewModel {
    @NonNull
    private WeatherDetailsType detailsType;
    private CharSequence label;
    private @DrawableRes
    int icon;
    private CharSequence value;
    private int iconRotation;

    private final WeatherIconsManager wim = WeatherIconsManager.getInstance();

    public DetailItemViewModel(@NonNull WeatherDetailsType detailsType, CharSequence value) {
        this(detailsType, value, 0);
    }

    public DetailItemViewModel(@NonNull WeatherDetailsType detailsType, CharSequence value, int iconRotation) {
        Context context = SimpleLibrary.getInstance().getAppContext();
        this.detailsType = detailsType;

        switch (detailsType) {
            case SUNRISE:
                this.label = context.getString(R.string.label_sunrise);
                this.icon = wim.getWeatherIconResource(WeatherIcons.SUNRISE);
                break;
            case SUNSET:
                this.label = context.getString(R.string.label_sunset);
                this.icon = wim.getWeatherIconResource(WeatherIcons.SUNSET);
                break;
            case FEELSLIKE:
                this.label = context.getString(R.string.label_feelslike);
                this.icon = wim.getWeatherIconResource(WeatherIcons.THERMOMETER);
                break;
            case WINDSPEED:
                this.label = context.getString(R.string.label_wind);
                this.icon = wim.getWeatherIconResource(WeatherIcons.WIND_DIRECTION);
                break;
            case WINDGUST:
                this.label = context.getString(R.string.label_windgust);
                this.icon = wim.getWeatherIconResource(WeatherIcons.CLOUDY_GUSTS);
                break;
            case HUMIDITY:
                this.label = context.getString(R.string.label_humidity);
                this.icon = wim.getWeatherIconResource(WeatherIcons.HUMIDITY);
                break;
            case PRESSURE:
                this.label = context.getString(R.string.label_pressure);
                this.icon = wim.getWeatherIconResource(WeatherIcons.BAROMETER);
                break;
            case VISIBILITY:
                this.label = context.getString(R.string.label_visibility);
                this.icon = wim.getWeatherIconResource(WeatherIcons.FOG);
                break;
            case POPCHANCE:
                this.label = context.getString(R.string.label_chance);
                this.icon = wim.getWeatherIconResource(WeatherIcons.UMBRELLA);
                break;
            case POPCLOUDINESS:
                this.label = context.getString(R.string.label_cloudiness);
                this.icon = wim.getWeatherIconResource(WeatherIcons.CLOUDY);
                break;
            case POPRAIN:
                this.label = context.getString(R.string.label_qpf_rain);
                this.icon = wim.getWeatherIconResource(WeatherIcons.RAINDROPS);
                break;
            case POPSNOW:
                this.label = context.getString(R.string.label_qpf_snow);
                this.icon = wim.getWeatherIconResource(WeatherIcons.SNOWFLAKE_COLD);
                break;
            case DEWPOINT:
                this.label = context.getString(R.string.label_dewpoint);
                this.icon = wim.getWeatherIconResource(WeatherIcons.THERMOMETER);
                break;
            case MOONRISE:
                this.label = context.getString(R.string.label_moonrise);
                this.icon = wim.getWeatherIconResource(WeatherIcons.MOONRISE);
                break;
            case MOONSET:
                this.label = context.getString(R.string.label_moonset);
                this.icon = wim.getWeatherIconResource(WeatherIcons.MOONSET);
                break;
            case MOONPHASE:
                this.label = context.getString(R.string.label_moonphase);
                this.icon = wim.getWeatherIconResource(WeatherIcons.MOON_ALT_NEW);
                break;
            case BEAUFORT:
                this.label = context.getString(R.string.label_beaufort);
                this.icon = R.drawable.wi_wind_beaufort_0;
                break;
            case UV:
                this.label = context.getString(R.string.label_uv);
                this.icon = wim.getWeatherIconResource(WeatherIcons.DAY_SUNNY);
                break;
            case AIRQUALITY:
                this.label = context.getString(R.string.label_airquality);
                this.icon = wim.getWeatherIconResource(WeatherIcons.CLOUDY_GUSTS);
                break;
        }

        this.value = value;
        this.iconRotation = iconRotation;
    }

    public DetailItemViewModel(@NonNull MoonPhase.MoonPhaseType moonPhaseType) {
        Context context = SimpleLibrary.getInstance().getAppContext();
        this.detailsType = WeatherDetailsType.MOONPHASE;

        this.label = context.getString(R.string.label_moonphase);
        this.iconRotation = 0;

        switch (moonPhaseType) {
            case NEWMOON:
                this.icon = wim.getWeatherIconResource(WeatherIcons.MOON_NEW);
                this.value = context.getString(R.string.moonphase_new);
                break;
            case WAXING_CRESCENT:
                this.icon = wim.getWeatherIconResource(WeatherIcons.MOON_ALT_WAXING_CRESCENT_3);
                this.value = context.getString(R.string.moonphase_waxcrescent);
                break;
            case FIRST_QTR:
                this.icon = wim.getWeatherIconResource(WeatherIcons.MOON_ALT_FIRST_QUARTER);
                this.value = context.getString(R.string.moonphase_firstqtr);
                break;
            case WAXING_GIBBOUS:
                this.icon = wim.getWeatherIconResource(WeatherIcons.MOON_ALT_WAXING_GIBBOUS_3);
                this.value = context.getString(R.string.moonphase_waxgibbous);
                break;
            case FULL_MOON:
                this.icon = wim.getWeatherIconResource(WeatherIcons.MOON_ALT_FULL);
                this.value = context.getString(R.string.moonphase_full);
                break;
            case WANING_GIBBOUS:
                this.icon = wim.getWeatherIconResource(WeatherIcons.MOON_ALT_WANING_GIBBOUS_3);
                this.value = context.getString(R.string.moonphase_wangibbous);
                break;
            case LAST_QTR:
                this.icon = wim.getWeatherIconResource(WeatherIcons.MOON_ALT_THIRD_QUARTER);
                this.value = context.getString(R.string.moonphase_lastqtr);
                break;
            case WANING_CRESCENT:
                this.icon = wim.getWeatherIconResource(WeatherIcons.MOON_ALT_WANING_CRESCENT_3);
                this.value = context.getString(R.string.moonphase_wancrescent);
                break;
        }
    }

    public DetailItemViewModel(@NonNull Beaufort.BeaufortScale beaufortScale) {
        Context context = SimpleLibrary.getInstance().getAppContext();
        this.detailsType = WeatherDetailsType.BEAUFORT;

        this.label = context.getString(R.string.label_beaufort);
        this.iconRotation = 0;

        switch (beaufortScale) {
            case B0:
                this.icon = R.drawable.wi_wind_beaufort_0;
                this.value = context.getString(R.string.beaufort_0);
                break;
            case B1:
                this.icon = R.drawable.wi_wind_beaufort_1;
                this.value = context.getString(R.string.beaufort_1);
                break;
            case B2:
                this.icon = R.drawable.wi_wind_beaufort_2;
                this.value = context.getString(R.string.beaufort_2);
                break;
            case B3:
                this.icon = R.drawable.wi_wind_beaufort_3;
                this.value = context.getString(R.string.beaufort_3);
                break;
            case B4:
                this.icon = R.drawable.wi_wind_beaufort_4;
                this.value = context.getString(R.string.beaufort_4);
                break;
            case B5:
                this.icon = R.drawable.wi_wind_beaufort_5;
                this.value = context.getString(R.string.beaufort_5);
                break;
            case B6:
                this.icon = R.drawable.wi_wind_beaufort_6;
                this.value = context.getString(R.string.beaufort_6);
                break;
            case B7:
                this.icon = R.drawable.wi_wind_beaufort_7;
                this.value = context.getString(R.string.beaufort_7);
                break;
            case B8:
                this.icon = R.drawable.wi_wind_beaufort_8;
                this.value = context.getString(R.string.beaufort_8);
                break;
            case B9:
                this.icon = R.drawable.wi_wind_beaufort_9;
                this.value = context.getString(R.string.beaufort_9);
                break;
            case B10:
                this.icon = R.drawable.wi_wind_beaufort_10;
                this.value = context.getString(R.string.beaufort_10);
                break;
            case B11:
                this.icon = R.drawable.wi_wind_beaufort_11;
                this.value = context.getString(R.string.beaufort_11);
                break;
            case B12:
                this.icon = R.drawable.wi_wind_beaufort_12;
                this.value = context.getString(R.string.beaufort_12);
                break;
        }
    }

    public DetailItemViewModel(@NonNull AirQuality aqi) {
        Context context = SimpleLibrary.getInstance().getAppContext();
        this.detailsType = WeatherDetailsType.AIRQUALITY;
        this.label = context.getString(R.string.label_airquality_short);
        this.icon = wim.getWeatherIconResource(WeatherIcons.CLOUDY_GUSTS);
        this.iconRotation = 0;

        if (aqi.getIndex() < 51) {
            this.value = String.format(LocaleUtils.getLocale(), "%d, %s", aqi.getIndex(), context.getString(R.string.aqi_level_0_50));
        } else if (aqi.getIndex() < 101) {
            this.value = String.format(LocaleUtils.getLocale(), "%d, %s", aqi.getIndex(), context.getString(R.string.aqi_level_51_100));
        } else if (aqi.getIndex() < 151) {
            this.value = String.format(LocaleUtils.getLocale(), "%d, %s", aqi.getIndex(), context.getString(R.string.aqi_level_101_150));
        } else if (aqi.getIndex() < 201) {
            this.value = String.format(LocaleUtils.getLocale(), "%d, %s", aqi.getIndex(), context.getString(R.string.aqi_level_151_200));
        } else if (aqi.getIndex() < 301) {
            this.value = String.format(LocaleUtils.getLocale(), "%d, %s", aqi.getIndex(), context.getString(R.string.aqi_level_201_300));
        } else if (aqi.getIndex() >= 301) {
            this.value = String.format(LocaleUtils.getLocale(), "%d, %s", aqi.getIndex(), context.getString(R.string.aqi_level_300));
        }
    }

    public DetailItemViewModel(@NonNull UV uv) {
        Context context = SimpleLibrary.getInstance().getAppContext();
        this.detailsType = WeatherDetailsType.UV;
        this.label = context.getString(R.string.label_uv);
        this.icon = wim.getWeatherIconResource(WeatherIcons.DAY_SUNNY);
        this.iconRotation = 0;

        if (uv.getIndex() < 3) {
            this.value = context.getString(R.string.uv_0);
        } else if (uv.getIndex() < 6) {
            this.value = context.getString(R.string.uv_3);
        } else if (uv.getIndex() < 8) {
            this.value = context.getString(R.string.uv_6);
        } else if (uv.getIndex() < 11) {
            this.value = context.getString(R.string.uv_8);
        } else if (uv.getIndex() >= 11) {
            this.value = context.getString(R.string.uv_11);
        }
    }

    @NonNull
    public WeatherDetailsType getDetailsType() {
        return detailsType;
    }

    public void setDetailsType(@NonNull WeatherDetailsType detailsType) {
        this.detailsType = detailsType;
    }

    public CharSequence getLabel() {
        return label;
    }

    public void setLabel(CharSequence label) {
        this.label = label;
    }

    public @DrawableRes
    int getIcon() {
        return icon;
    }

    public void setIcon(@DrawableRes int icon) {
        this.icon = icon;
    }

    public CharSequence getValue() {
        return value;
    }

    public void setValue(CharSequence value) {
        this.value = value;
    }

    public int getIconRotation() {
        return iconRotation;
    }

    public void setIconRotation(int iconRotation) {
        this.iconRotation = iconRotation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DetailItemViewModel that = (DetailItemViewModel) o;

        if (getIconRotation() != that.getIconRotation()) return false;
        if (getDetailsType() != that.getDetailsType()) return false;
        if (getLabel() != null ? !getLabel().equals(that.getLabel()) : that.getLabel() != null)
            return false;
        if (getIcon() != that.getIcon()) return false;
        return getValue() != null ? getValue().equals(that.getValue()) : that.getValue() == null;
    }

    @Override
    public int hashCode() {
        int result = getDetailsType() != null ? getDetailsType().hashCode() : 0;
        result = 31 * result + (getLabel() != null ? getLabel().hashCode() : 0);
        result = 31 * result + getIcon();
        result = 31 * result + (getValue() != null ? getValue().hashCode() : 0);
        result = 31 * result + getIconRotation();
        return result;
    }
}
