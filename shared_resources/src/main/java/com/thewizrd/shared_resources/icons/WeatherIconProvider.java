package com.thewizrd.shared_resources.icons;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public abstract class WeatherIconProvider implements WeatherIconsProviderInterface {
    public abstract String getKey();

    public abstract String getDisplayName();

    @DrawableRes
    public abstract int getWeatherIconResource(@NonNull String icon);

    public abstract boolean isFontIcon();
}
