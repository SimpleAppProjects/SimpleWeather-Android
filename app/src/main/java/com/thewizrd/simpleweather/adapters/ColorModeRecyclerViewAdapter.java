package com.thewizrd.simpleweather.adapters;

import androidx.recyclerview.widget.RecyclerView;

public abstract class ColorModeRecyclerViewAdapter extends RecyclerView.Adapter {
    private boolean isDarkMode = true;

    public final boolean isLightBackground() {
        return !isDarkMode;
    }

    public final void updateColors(boolean isDark) {
        isDarkMode = isDark;
        notifyDataSetChanged();
    }
}
