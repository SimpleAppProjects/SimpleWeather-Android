package com.thewizrd.shared_resources;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.security.ProviderInstaller;
import com.thewizrd.shared_resources.icons.WeatherIconProvider;
import com.thewizrd.shared_resources.icons.WeatherIconsProvider;
import com.thewizrd.shared_resources.okhttp3.CacheInterceptor;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;

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

    private LinkedHashMap<String, WeatherIconProvider> mIconProviders;

    @SuppressLint("StaticFieldLeak")
    private static SimpleLibrary sSimpleLib;

    private SimpleLibrary() {
        mIconProviders = new LinkedHashMap<>();

        // Register default icon provider
        registerIconProvider(new WeatherIconsProvider());
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

    public static void init(ApplicationLib app) {
        if (sSimpleLib == null) {
            sSimpleLib = new SimpleLibrary(app);
        } else {
            sSimpleLib.mApp = app;
            sSimpleLib.mContext = app.getAppContext();
        }

        try {
            ProviderInstaller.installIfNeeded(sSimpleLib.getAppContext());
        } catch (Exception e) {
            Logger.writeLine(Log.ERROR, e);
        }
    }

    public static void unRegister() {
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
                    .readTimeout(Settings.READ_TIMEOUT, TimeUnit.MILLISECONDS)
                    .connectTimeout(Settings.CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                    .followRedirects(true)
                    .retryOnConnectionFailure(true)
                    .cache(new Cache(new File(mContext.getCacheDir(), "okhttp3"), 50L * 1024 * 1024))
                    .addNetworkInterceptor(new CacheInterceptor())
                    .build();
        }

        return client;
    }

    public void registerIconProvider(@NonNull WeatherIconProvider provider) {
        mIconProviders.put(provider.getKey(), provider);
    }

    @NonNull
    public WeatherIconProvider getIconProvider(@NonNull String key) {
        WeatherIconProvider provider = mIconProviders.get(key);
        if (provider == null) {
            registerIconProvider(provider = new WeatherIconsProvider());
        }
        return provider;
    }

    @NonNull
    public Map<String, WeatherIconProvider> getIconProviders() {
        return Collections.unmodifiableMap(mIconProviders);
    }
}
