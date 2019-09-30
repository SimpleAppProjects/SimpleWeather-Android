package com.thewizrd.shared_resources.wearable;

import android.util.SparseArray;

public enum WearableDataSync {
    OFF(0),
    DEVICEONLY(1);

    private final int value;

    public int getValue() {
        return value;
    }

    private WearableDataSync(int value) {
        this.value = value;
    }

    private static SparseArray<WearableDataSync> map = new SparseArray<>();

    static {
        for (WearableDataSync dataSync : values()) {
            map.put(dataSync.value, dataSync);
        }
    }

    public static WearableDataSync valueOf(int value) {
        return map.get(value);
    }
}
