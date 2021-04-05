package com.thewizrd.shared_resources;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.utils.SettingsManager;

public interface ApplicationLib {
    Context getAppContext();

    SharedPreferences getPreferences();

    void registerAppSharedPreferenceListener();

    void unregisterAppSharedPreferenceListener();

    void registerAppSharedPreferenceListener(@NonNull SharedPreferences.OnSharedPreferenceChangeListener listener);

    void unregisterAppSharedPreferenceListener(@NonNull SharedPreferences.OnSharedPreferenceChangeListener listener);

    AppState getAppState();

    boolean isPhone();

    Bundle getProperties();

    SettingsManager getSettingsManager();
}
