package com.thewizrd.shared_resources.controls;

import android.content.Context;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.weatherdata.Beaufort;
import com.thewizrd.shared_resources.weatherdata.MoonPhase;

public class DetailItemViewModel {
    private String label;
    private String icon;
    private String value;
    private int iconRotation;

    public DetailItemViewModel(WeatherDetailsType detailsType, String value) {
        this(detailsType, value, 0);
    }

    public DetailItemViewModel(WeatherDetailsType detailsType, String value, int iconRotation) {
        Context context = SimpleLibrary.getInstance().getAppContext();

        switch (detailsType) {
            case SUNRISE:
                this.label = context.getString(R.string.label_sunrise);
                this.icon = context.getString(R.string.wi_sunrise);
                break;
            case SUNSET:
                this.label = context.getString(R.string.label_sunset);
                this.icon = context.getString(R.string.wi_sunset);
                break;
            case FEELSLIKE:
                this.label = context.getString(R.string.label_feelslike);
                this.icon = context.getString(R.string.wi_thermometer);
                break;
            case WINDSPEED:
                this.label = context.getString(R.string.label_wind);
                this.icon = context.getString(R.string.wi_wind_direction);
                break;
            case HUMIDITY:
                this.label = context.getString(R.string.label_humidity);
                this.icon = context.getString(R.string.wi_humidity);
                break;
            case PRESSURE:
                this.label = context.getString(R.string.label_pressure);
                this.icon = context.getString(R.string.wi_barometer);
                break;
            case VISIBILITY:
                this.label = context.getString(R.string.label_visibility);
                this.icon = context.getString(R.string.wi_fog);
                break;
            case POPCHANCE:
                this.label = context.getString(R.string.label_chance);
                this.icon = context.getString(R.string.wi_raindrop);
                break;
            case POPCLOUDINESS:
                this.label = context.getString(R.string.label_cloudiness);
                this.icon = context.getString(R.string.wi_cloudy);
                break;
            case POPRAIN:
                this.label = context.getString(R.string.label_qpf_rain);
                this.icon = context.getString(R.string.wi_raindrops);
                break;
            case POPSNOW:
                this.label = context.getString(R.string.label_qpf_snow);
                this.icon = context.getString(R.string.wi_snowflake_cold);
                break;
            case DEWPOINT:
                this.label = context.getString(R.string.label_dewpoint);
                this.icon = context.getString(R.string.wi_thermometer);
                break;
            case MOONRISE:
                this.label = context.getString(R.string.label_moonrise);
                this.icon = context.getString(R.string.wi_moonrise);
                break;
            case MOONSET:
                this.label = context.getString(R.string.label_moonset);
                this.icon = context.getString(R.string.wi_moonset);
                break;
            case MOONPHASE:
                this.label = context.getString(R.string.label_moonphase);
                this.icon = context.getString(R.string.wi_moon_alt_new);
                break;
            case BEAUFORT:
                this.label = context.getString(R.string.label_beaufort);
                this.icon = context.getString(R.string.wi_wind_beaufort_0);
                break;
            case UV:
                this.label = context.getString(R.string.label_uv);
                this.icon = context.getString(R.string.wi_day_sunny);
                break;
        }

        this.value = value;
        this.iconRotation = iconRotation;
    }

    public DetailItemViewModel(MoonPhase.MoonPhaseType moonPhaseType, String description) {
        Context context = SimpleLibrary.getInstance().getAppContext();

        this.label = context.getString(R.string.label_moonphase);
        this.value = description;
        this.iconRotation = 0;

        switch (moonPhaseType) {
            case NEWMOON:
                this.icon = context.getString(R.string.wi_moon_alt_new);
                break;
            case WAXING_CRESCENT:
                this.icon = context.getString(R.string.wi_moon_alt_waxing_crescent_3);
                break;
            case FIRST_QTR:
                this.icon = context.getString(R.string.wi_moon_alt_first_quarter);
                break;
            case WAXING_GIBBOUS:
                this.icon = context.getString(R.string.wi_moon_alt_waxing_gibbous_3);
                break;
            case FULL_MOON:
                this.icon = context.getString(R.string.wi_moon_alt_full);
                break;
            case WANING_GIBBOUS:
                this.icon = context.getString(R.string.wi_moon_alt_waning_gibbous_3);
                break;
            case LAST_QTR:
                this.icon = context.getString(R.string.wi_moon_alt_third_quarter);
                break;
            case WANING_CRESCENT:
                this.icon = context.getString(R.string.wi_moon_alt_waning_crescent_3);
                break;
        }
    }

    public DetailItemViewModel(Beaufort.BeaufortScale beaufortScale, String description) {
        Context context = SimpleLibrary.getInstance().getAppContext();

        this.label = context.getString(R.string.label_beaufort);
        this.value = description;
        this.iconRotation = 0;

        switch (beaufortScale) {
            case B0:
                this.icon = context.getString(R.string.wi_wind_beaufort_0);
                break;
            case B1:
                this.icon = context.getString(R.string.wi_wind_beaufort_1);
                break;
            case B2:
                this.icon = context.getString(R.string.wi_wind_beaufort_2);
                break;
            case B3:
                this.icon = context.getString(R.string.wi_wind_beaufort_3);
                break;
            case B4:
                this.icon = context.getString(R.string.wi_wind_beaufort_4);
                break;
            case B5:
                this.icon = context.getString(R.string.wi_wind_beaufort_5);
                break;
            case B6:
                this.icon = context.getString(R.string.wi_wind_beaufort_6);
                break;
            case B7:
                this.icon = context.getString(R.string.wi_wind_beaufort_7);
                break;
            case B8:
                this.icon = context.getString(R.string.wi_wind_beaufort_8);
                break;
            case B9:
                this.icon = context.getString(R.string.wi_wind_beaufort_9);
                break;
            case B10:
                this.icon = context.getString(R.string.wi_wind_beaufort_10);
                break;
            case B11:
                this.icon = context.getString(R.string.wi_wind_beaufort_11);
                break;
            case B12:
                this.icon = context.getString(R.string.wi_wind_beaufort_12);
                break;
        }
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getIconRotation() {
        return iconRotation;
    }

    public void setIconRotation(int iconRotation) {
        this.iconRotation = iconRotation;
    }
}
