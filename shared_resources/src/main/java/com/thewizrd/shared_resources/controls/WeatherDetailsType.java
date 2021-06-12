package com.thewizrd.shared_resources.controls;

import android.util.SparseArray;

public enum WeatherDetailsType {
    SUNRISE(0),
    SUNSET(1),
    FEELSLIKE(2),
    WINDSPEED(3),
    WINDGUST(4),
    HUMIDITY(5),
    PRESSURE(6),
    VISIBILITY(7),
    POPCLOUDINESS(8),
    POPCHANCE(9),
    POPRAIN(10),
    POPSNOW(11),
    DEWPOINT(12),
    MOONRISE(13),
    MOONSET(14),
    MOONPHASE(15),
    BEAUFORT(16),
    UV(17),
    AIRQUALITY(18),
    TREEPOLLEN(19),
    GRASSPOLLEN(20),
    RAGWEEDPOLLEN(21);

    private final int value;

    public int getValue() {
        return value;
    }

    private WeatherDetailsType(int value) {
        this.value = value;
    }

    private static final SparseArray<WeatherDetailsType> map = new SparseArray<>();

    static {
        for (WeatherDetailsType mode : values()) {
            map.put(mode.value, mode);
        }
    }

    public static WeatherDetailsType valueOf(int value) {
        return map.get(value);
    }
}
