package com.thewizrd.shared_resources.controls;

import android.util.SparseArray;

public enum WeatherDetailsType {
    SUNRISE(0),
    SUNSET(1),
    FEELSLIKE(2),
    WINDSPEED(3),
    HUMIDITY(4),
    PRESSURE(5),
    VISIBILITY(6),
    POPCLOUDINESS(7),
    POPCHANCE(8),
    POPRAIN(9),
    POPSNOW(10),
    DEWPOINT(11),
    MOONRISE(12),
    MOONSET(13),
    MOONPHASE(14),
    BEAUFORT(15),
    UV(16),
    AIRQUALITY(17);

    private final int value;

    public int getValue() {
        return value;
    }

    private WeatherDetailsType(int value) {
        this.value = value;
    }

    private static SparseArray<WeatherDetailsType> map = new SparseArray<>();

    static {
        for (WeatherDetailsType mode : values()) {
            map.put(mode.value, mode);
        }
    }

    public static WeatherDetailsType valueOf(int value) {
        return map.get(value);
    }
}
