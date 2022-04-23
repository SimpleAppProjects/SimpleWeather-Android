package com.thewizrd.shared_resources.icons;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public abstract class WeatherIconProvider implements WeatherIconsProvider {
    public abstract String getKey();

    public abstract String getDisplayName();

    public abstract String getAuthorName();

    public abstract String getAttributionLink();

    @DrawableRes
    public abstract int getWeatherIconResource(@NonNull String icon);

    public abstract boolean isFontIcon();
}
