package com.thewizrd.shared_resources.utils;

import android.util.SparseArray;

public enum DarkMode {
    FOLLOW_SYSTEM(0),
    DARK(1),
    AMOLED_DARK(2);

    private final int value;

    public int getValue() {
        return value;
    }

    private DarkMode(int value) {
        this.value = value;
    }

    private static SparseArray<DarkMode> map = new SparseArray<>();

    static {
        for (DarkMode mode : values()) {
            map.put(mode.value, mode);
        }
    }

    public static DarkMode valueOf(int value) {
        return map.get(value);
    }

    public interface OnThemeChangeListener {
        void onThemeChanged(DarkMode mode);
    }
}
