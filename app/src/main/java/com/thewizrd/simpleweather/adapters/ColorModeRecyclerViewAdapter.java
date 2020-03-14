package com.thewizrd.simpleweather.adapters;

import androidx.annotation.ColorInt;
import androidx.recyclerview.widget.RecyclerView;

import com.thewizrd.shared_resources.helpers.ColorsUtils;
import com.thewizrd.simpleweather.helpers.DarkMode;

public abstract class ColorModeRecyclerViewAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<T> {
    public static class Payload {
        public static final int COLOR_UPDATE = 0;
    }

    private DarkMode darkThemeMode;
    private @ColorInt
    int itemColor;
    private boolean isLightBackground;

    public final DarkMode getDarkThemeMode() {
        return darkThemeMode;
    }

    public final boolean isLightBackground() {
        return isLightBackground;
    }

    public final void setDarkThemeMode(DarkMode mode) {
        darkThemeMode = mode;
        notifyItemRangeChanged(0, getItemCount(), Payload.COLOR_UPDATE);
    }

    public @ColorInt
    int getItemColor() {
        return itemColor;
    }

    public void setItemColor(@ColorInt int itemColor) {
        this.itemColor = itemColor;
        isLightBackground = ColorsUtils.isSuperLight(this.itemColor);
        notifyItemRangeChanged(0, getItemCount(), Payload.COLOR_UPDATE);
    }
}
