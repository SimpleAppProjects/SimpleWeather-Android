package com.thewizrd.shared_resources.icons;

import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.utils.SettingsManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class WeatherIconsManager implements WeatherIconsProviderInterface {
    private static WeatherIconsManager instance;
    private static WeatherIconsProviderInterface iconsProvider;

    public static final Map<String, WeatherIconProvider> DEFAULT_ICONS;

    static {
        Map<String, WeatherIconProvider> defaultIconMap = new HashMap<>(3);
        addIconProvider(defaultIconMap, new WeatherIconsProvider());
        addIconProvider(defaultIconMap, new WUndergroundIconsProvider());
        addIconProvider(defaultIconMap, new WeatherIconicProvider());
        DEFAULT_ICONS = Collections.unmodifiableMap(defaultIconMap);
    }

    private static void addIconProvider(@NonNull Map<String, WeatherIconProvider> map,
                                        @NonNull WeatherIconProvider provider) {
        map.put(provider.getKey(), provider);
    }

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
        final SettingsManager settingsMgr = SimpleLibrary.getInstance().getApp().getSettingsManager();
        String iconsSource = settingsMgr.getIconsProvider();
        iconsProvider = getProvider(iconsSource);
    }

    public WeatherIconsProviderInterface getProvider() {
        if (iconsProvider == null) {
            updateIconProvider();
        }
        return iconsProvider;
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