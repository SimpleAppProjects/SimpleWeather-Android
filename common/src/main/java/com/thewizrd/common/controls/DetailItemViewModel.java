package com.thewizrd.common.controls;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SharedModuleKt;
import com.thewizrd.shared_resources.icons.WeatherIcons;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.shared_resources.weatherdata.model.AirQuality;
import com.thewizrd.shared_resources.weatherdata.model.Beaufort;
import com.thewizrd.shared_resources.weatherdata.model.MoonPhase;
import com.thewizrd.shared_resources.weatherdata.model.UV;

import java.util.Objects;

public class DetailItemViewModel {
    @NonNull
    private WeatherDetailsType detailsType;
    private CharSequence label;
    private String icon;
    private CharSequence value;
    private CharSequence shortValue;
    private int iconRotation;

    public DetailItemViewModel(@NonNull WeatherDetailsType detailsType, CharSequence value) {
        this(detailsType, value, 0);
    }

    public DetailItemViewModel(@NonNull WeatherDetailsType detailsType, CharSequence value, CharSequence shortValue) {
        this(detailsType, value, shortValue, 0);
    }

    public DetailItemViewModel(@NonNull WeatherDetailsType detailsType, CharSequence value, int iconRotation) {
        this(detailsType, value, value, iconRotation);
    }

    public DetailItemViewModel(@NonNull WeatherDetailsType detailsType, CharSequence value, CharSequence shortValue, int iconRotation) {
        final Context context = SharedModuleKt.getSharedDeps().getContext();
        this.detailsType = detailsType;

        switch (detailsType) {
            case SUNRISE:
                this.label = context.getString(R.string.label_sunrise);
                this.icon = WeatherIcons.SUNRISE;
                break;
            case SUNSET:
                this.label = context.getString(R.string.label_sunset);
                this.icon = WeatherIcons.SUNSET;
                break;
            case FEELSLIKE:
                this.label = context.getString(R.string.label_feelslike);
                this.icon = WeatherIcons.THERMOMETER;
                break;
            case WINDSPEED:
                this.label = context.getString(R.string.label_wind);
                this.icon = WeatherIcons.WIND_DIRECTION;
                break;
            case WINDGUST:
                this.label = context.getString(R.string.label_windgust);
                this.icon = WeatherIcons.CLOUDY_GUSTS;
                break;
            case HUMIDITY:
                this.label = context.getString(R.string.label_humidity);
                this.icon = WeatherIcons.HUMIDITY;
                break;
            case PRESSURE:
                this.label = context.getString(R.string.label_pressure);
                this.icon = WeatherIcons.BAROMETER;
                break;
            case VISIBILITY:
                this.label = context.getString(R.string.label_visibility);
                this.icon = WeatherIcons.VISIBILITY;
                break;
            case POPCHANCE:
                this.label = context.getString(R.string.label_chance);
                this.icon = WeatherIcons.UMBRELLA;
                break;
            case POPCLOUDINESS:
                this.label = context.getString(R.string.label_cloudiness);
                this.icon = WeatherIcons.CLOUDY;
                break;
            case POPRAIN:
                this.label = context.getString(R.string.label_qpf_rain);
                this.icon = WeatherIcons.RAINDROPS;
                break;
            case POPSNOW:
                this.label = context.getString(R.string.label_qpf_snow);
                this.icon = WeatherIcons.SNOWFLAKE_COLD;
                break;
            case DEWPOINT:
                this.label = context.getString(R.string.label_dewpoint);
                this.icon = WeatherIcons.THERMOMETER;
                break;
            case MOONRISE:
                this.label = context.getString(R.string.label_moonrise);
                this.icon = WeatherIcons.MOONRISE;
                break;
            case MOONSET:
                this.label = context.getString(R.string.label_moonset);
                this.icon = WeatherIcons.MOONSET;
                break;
            case MOONPHASE:
                this.label = context.getString(R.string.label_moonphase);
                this.icon = WeatherIcons.MOON_ALT_NEW;
                break;
            case BEAUFORT:
                this.label = context.getString(R.string.label_beaufort);
                this.icon = WeatherIcons.WIND_BEAUFORT_0;
                break;
            case UV:
                this.label = context.getString(R.string.label_uv);
                this.icon = WeatherIcons.UV_INDEX;
                break;
            case AIRQUALITY:
                this.label = context.getString(R.string.label_airquality);
                this.icon = WeatherIcons.AIR_QUALITY;
                break;
            case TREEPOLLEN:
                this.label = context.getString(R.string.label_tree_pollen);
                this.icon = WeatherIcons.TREE_POLLEN;
                break;
            case GRASSPOLLEN:
                this.label = context.getString(R.string.label_grass_pollen);
                this.icon = WeatherIcons.GRASS_POLLEN;
                break;
            case RAGWEEDPOLLEN:
                this.label = context.getString(R.string.label_ragweed_pollen);
                this.icon = WeatherIcons.RAGWEED_POLLEN;
                break;
        }

        this.value = value;
        this.shortValue = shortValue;
        this.iconRotation = iconRotation;
    }

    public DetailItemViewModel(@NonNull MoonPhase.MoonPhaseType moonPhaseType) {
        final Context context = SharedModuleKt.getSharedDeps().getContext();
        this.detailsType = WeatherDetailsType.MOONPHASE;

        this.label = context.getString(R.string.label_moonphase);
        this.iconRotation = 0;

        switch (moonPhaseType) {
            case NEWMOON:
                this.icon = WeatherIcons.MOON_NEW;
                this.value = context.getString(R.string.moonphase_new);
                this.shortValue = context.getString(R.string.moonphase_new_short);
                break;
            case WAXING_CRESCENT:
                this.icon = WeatherIcons.MOON_ALT_WAXING_CRESCENT_3;
                this.value = context.getString(R.string.moonphase_waxcrescent);
                this.shortValue = context.getString(R.string.moonphase_waxcrescent_short);
                break;
            case FIRST_QTR:
                this.icon = WeatherIcons.MOON_ALT_FIRST_QUARTER;
                this.value = context.getString(R.string.moonphase_firstqtr);
                this.shortValue = context.getString(R.string.moonphase_firstqtr_short);
                break;
            case WAXING_GIBBOUS:
                this.icon = WeatherIcons.MOON_ALT_WAXING_GIBBOUS_3;
                this.value = context.getString(R.string.moonphase_waxgibbous);
                this.shortValue = context.getString(R.string.moonphase_waxgibbous_short);
                break;
            case FULL_MOON:
                this.icon = WeatherIcons.MOON_ALT_FULL;
                this.value = context.getString(R.string.moonphase_full);
                this.shortValue = context.getString(R.string.moonphase_full_short);
                break;
            case WANING_GIBBOUS:
                this.icon = WeatherIcons.MOON_ALT_WANING_GIBBOUS_3;
                this.value = context.getString(R.string.moonphase_wangibbous);
                this.shortValue = context.getString(R.string.moonphase_wangibbous_short);
                break;
            case LAST_QTR:
                this.icon = WeatherIcons.MOON_ALT_THIRD_QUARTER;
                this.value = context.getString(R.string.moonphase_lastqtr);
                this.shortValue = context.getString(R.string.moonphase_lastqtr_short);
                break;
            case WANING_CRESCENT:
                this.icon = WeatherIcons.MOON_ALT_WANING_CRESCENT_3;
                this.value = context.getString(R.string.moonphase_wancrescent);
                this.shortValue = context.getString(R.string.moonphase_wancrescent_short);
                break;
        }
    }

    public DetailItemViewModel(@NonNull Beaufort.BeaufortScale beaufortScale) {
        final Context context = SharedModuleKt.getSharedDeps().getContext();
        this.detailsType = WeatherDetailsType.BEAUFORT;

        this.label = context.getString(R.string.label_beaufort);
        this.iconRotation = 0;

        switch (beaufortScale) {
            case B0:
                this.icon = WeatherIcons.WIND_BEAUFORT_0;
                this.value = context.getString(R.string.beaufort_0);
                break;
            case B1:
                this.icon = WeatherIcons.WIND_BEAUFORT_1;
                this.value = context.getString(R.string.beaufort_1);
                break;
            case B2:
                this.icon = WeatherIcons.WIND_BEAUFORT_2;
                this.value = context.getString(R.string.beaufort_2);
                break;
            case B3:
                this.icon = WeatherIcons.WIND_BEAUFORT_3;
                this.value = context.getString(R.string.beaufort_3);
                break;
            case B4:
                this.icon = WeatherIcons.WIND_BEAUFORT_4;
                this.value = context.getString(R.string.beaufort_4);
                break;
            case B5:
                this.icon = WeatherIcons.WIND_BEAUFORT_5;
                this.value = context.getString(R.string.beaufort_5);
                break;
            case B6:
                this.icon = WeatherIcons.WIND_BEAUFORT_6;
                this.value = context.getString(R.string.beaufort_6);
                break;
            case B7:
                this.icon = WeatherIcons.WIND_BEAUFORT_7;
                this.value = context.getString(R.string.beaufort_7);
                break;
            case B8:
                this.icon = WeatherIcons.WIND_BEAUFORT_8;
                this.value = context.getString(R.string.beaufort_8);
                break;
            case B9:
                this.icon = WeatherIcons.WIND_BEAUFORT_9;
                this.value = context.getString(R.string.beaufort_9);
                break;
            case B10:
                this.icon = WeatherIcons.WIND_BEAUFORT_10;
                this.value = context.getString(R.string.beaufort_10);
                break;
            case B11:
                this.icon = WeatherIcons.WIND_BEAUFORT_11;
                this.value = context.getString(R.string.beaufort_11);
                break;
            case B12:
                this.icon = WeatherIcons.WIND_BEAUFORT_12;
                this.value = context.getString(R.string.beaufort_12);
                break;
        }

        this.shortValue = String.format(LocaleUtils.getLocale(), "%d", beaufortScale.ordinal());
    }

    public DetailItemViewModel(@NonNull AirQuality aqi) {
        final Context context = SharedModuleKt.getSharedDeps().getContext();
        this.detailsType = WeatherDetailsType.AIRQUALITY;
        this.label = context.getString(R.string.label_airquality_short);
        this.icon = WeatherIcons.AIR_QUALITY;
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

        this.shortValue = String.format(LocaleUtils.getLocale(), "%d", aqi.getIndex());
    }

    public DetailItemViewModel(@NonNull UV uv) {
        final Context context = SharedModuleKt.getSharedDeps().getContext();
        this.detailsType = WeatherDetailsType.UV;
        this.label = context.getString(R.string.label_uv);
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

        switch (uv.getIndex().intValue()) {
            case 1 -> icon = WeatherIcons.UV_INDEX_1;
            case 2 -> icon = WeatherIcons.UV_INDEX_2;
            case 3 -> icon = WeatherIcons.UV_INDEX_3;
            case 4 -> icon = WeatherIcons.UV_INDEX_4;
            case 5 -> icon = WeatherIcons.UV_INDEX_5;
            case 6 -> icon = WeatherIcons.UV_INDEX_6;
            case 7 -> icon = WeatherIcons.UV_INDEX_7;
            case 8 -> icon = WeatherIcons.UV_INDEX_8;
            case 9 -> icon = WeatherIcons.UV_INDEX_9;
            case 10 -> icon = WeatherIcons.UV_INDEX_10;
            case 11 -> icon = WeatherIcons.UV_INDEX_11;
            default -> icon = WeatherIcons.UV_INDEX;
        }

        this.shortValue = String.format(LocaleUtils.getLocale(), "%d", Math.round(uv.getIndex()));
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    public DetailItemViewModel(@NonNull WeatherDetailsType detailsType) {
        this.detailsType = detailsType;
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public CharSequence getValue() {
        return value;
    }

    public void setValue(CharSequence value) {
        this.value = value;
    }

    public CharSequence getShortValue() {
        return shortValue;
    }

    public void setShortValue(CharSequence value) {
        this.shortValue = value;
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
        if (!(o instanceof DetailItemViewModel that)) return false;
        return iconRotation == that.iconRotation && detailsType == that.detailsType && Objects.equals(label, that.label) && Objects.equals(icon, that.icon) && Objects.equals(value, that.value) && Objects.equals(shortValue, that.shortValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(detailsType, label, icon, value, shortValue, iconRotation);
    }
}
