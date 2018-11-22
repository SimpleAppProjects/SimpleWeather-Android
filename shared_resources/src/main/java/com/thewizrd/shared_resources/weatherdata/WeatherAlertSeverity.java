package com.thewizrd.shared_resources.weatherdata;

import android.util.SparseArray;

public enum WeatherAlertSeverity {
    UNKNOWN(-1),
    MINOR(0),
    MODERATE(1),
    SEVERE(2),
    EXTREME(3);

    private final int value;

    public int getValue() {
        return value;
    }

    private WeatherAlertSeverity(int value) {
        this.value = value;
    }


    private static SparseArray<WeatherAlertSeverity> map = new SparseArray<>();

    static {
        for (WeatherAlertSeverity severity : values()) {
            map.put(severity.value, severity);
        }
    }

    public static WeatherAlertSeverity valueOf(int value) {
        return map.get(value);
    }
}
