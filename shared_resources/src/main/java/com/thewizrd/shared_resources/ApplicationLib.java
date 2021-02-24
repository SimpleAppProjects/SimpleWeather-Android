package com.thewizrd.shared_resources;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

public interface ApplicationLib {
    Context getAppContext();

    SharedPreferences getPreferences();

    SharedPreferences.OnSharedPreferenceChangeListener getSharedPreferenceListener();

    AppState getAppState();

    boolean isPhone();

    Bundle getProperties();
}
