package com.thewizrd.simpleweather;

import android.util.SparseArray;

public enum WeatherListType {
    FORECAST(0),
    HOURLYFORECAST(1),
    ALERTS(2);

    private final int value;

    public int getValue() {
        return value;
    }

    private WeatherListType(int value) {
        this.value = value;
    }

    private static SparseArray<WeatherListType> map = new SparseArray<>();

    static {
        for (WeatherListType weatherListType : values()) {
            map.put(weatherListType.value, weatherListType);
        }
    }

    public static WeatherListType valueOf(int value) {
        return map.get(value);
    }
}
