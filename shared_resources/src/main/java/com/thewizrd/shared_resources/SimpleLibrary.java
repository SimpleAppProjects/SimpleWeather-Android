package com.thewizrd.shared_resources;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.common.collect.Iterables;
import com.thewizrd.shared_resources.icons.WeatherIconProvider;
import com.thewizrd.shared_resources.icons.WeatherIconsManager;
import com.thewizrd.shared_resources.icons.WeatherIconsProvider;
import com.thewizrd.shared_resources.okhttp3.CacheInterceptor;
import com.thewizrd.shared_resources.utils.SettingsManager;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

public final class SimpleLibrary {
    private ApplicationLib mApp;
    private Context mContext;
    private OkHttpClient client;

    private final LinkedHashMap<String, WeatherIconProvider> mIconProviders;

    @SuppressLint("StaticFieldLeak")
    private static SimpleLibrary sSimpleLib;

    private SimpleLibrary() {
        mIconProviders = new LinkedHashMap<>();

        // Register default icon providers
        resetIconProviders();
    }

    private SimpleLibrary(ApplicationLib app) {
        this();
        mApp = app;
        mContext = app.getAppContext();
    }

    public static SimpleLibrary getInstance() {
        if (sSimpleLib == null)
            sSimpleLib = new SimpleLibrary();

        return sSimpleLib;
    }

    public static void initialize(ApplicationLib app) {
        if (sSimpleLib == null) {
            sSimpleLib = new SimpleLibrary(app);
        } else {
            sSimpleLib.mApp = app;
            sSimpleLib.mContext = app.getAppContext();
        }

        // For added network security
        GMSSecurityProvider.installAsync(app.getAppContext());
    }

    public static void unregister() {
        sSimpleLib = null;
    }

    public ApplicationLib getApp() {
        return mApp;
    }

    public Context getAppContext() {
        return mContext;
    }

    public OkHttpClient getHttpClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .readTimeout(SettingsManager.READ_TIMEOUT, TimeUnit.MILLISECONDS)
                    .connectTimeout(SettingsManager.CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                    .followRedirects(true)
                    .retryOnConnectionFailure(true)
                    .cache(new Cache(new File(mContext.getCacheDir(), "okhttp3"), 50L * 1024 * 1024))
                    .addNetworkInterceptor(new CacheInterceptor())
                    .build();
        }

        return client;
    }

    public void registerIconProvider(@NonNull WeatherIconProvider provider) {
        if (!mIconProviders.containsKey(provider.getKey())) {
            mIconProviders.put(provider.getKey(), provider);
        }
    }

    @NonNull
    public WeatherIconProvider getIconProvider(@NonNull String key) {
        WeatherIconProvider provider = mIconProviders.get(key);
        if (provider == null) {
            // Can't find the provider for this key; fallback to default/first available
            if (mIconProviders.size() > 0) {
                provider = Iterables.getFirst(mIconProviders.values(), null);
            } else {
                registerIconProvider(provider = new WeatherIconsProvider());
            }
        }
        return provider;
    }

    @NonNull
    public Map<String, WeatherIconProvider> getIconProviders() {
        return Collections.unmodifiableMap(mIconProviders);
    }

    public void resetIconProviders() {
        mIconProviders.clear();
        mIconProviders.putAll(WeatherIconsManager.DEFAULT_ICONS);
    }
}
