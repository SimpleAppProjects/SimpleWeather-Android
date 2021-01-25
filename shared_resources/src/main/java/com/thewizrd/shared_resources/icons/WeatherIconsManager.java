package com.thewizrd.shared_resources.icons;

import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.Settings;

public final class WeatherIconsManager implements WeatherIconsProviderInterface {
    private static WeatherIconsManager instance;
    private static WeatherIconsProviderInterface iconsProvider;

    // Prevent instance from being created outside of this class
    private WeatherIconsManager() {
        updateIconProvider();
    }

    public static synchronized WeatherIconsManager getInstance() {
        if (instance == null)
            instance = new WeatherIconsManager();

        return instance;
    }

    public void updateIconProvider() {
        String iconsSource = Settings.getIconsProvider();
        iconsProvider = getProvider(iconsSource);
    }

    @NonNull
    public static WeatherIconProvider getProvider(String iconsSource) {
        return SimpleLibrary.getInstance().getIconProvider(iconsSource);
    }

    @Override
    public boolean isFontIcon() {
        return iconsProvider.isFontIcon();
    }

    @Override
    public int getWeatherIconResource(@NonNull String icon) {
        return iconsProvider.getWeatherIconResource(icon);
    }
}