package com.thewizrd.shared_resources.weatherdata.model;

import android.util.SparseArray;

public enum LocationType {
    GPS(-1),
    SEARCH(0);

    private final int value;

    public int getValue() {
        return value;
    }

    private LocationType(int value) {
        this.value = value;
    }

    private static SparseArray<LocationType> map = new SparseArray<>();

    static {
        for (LocationType locationType : values()) {
            map.put(locationType.value, locationType);
        }
    }

    public static LocationType valueOf(int value) {
        return map.get(value);
    }
}
