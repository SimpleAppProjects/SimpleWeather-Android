package com.thewizrd.simpleweather.fragments;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.thewizrd.simpleweather.helpers.WindowColorManager;

public abstract class WindowColorFragment extends Fragment implements WindowColorManager {

    private Configuration prevConfig;

    @Override
    public void onResume() {
        super.onResume();
        prevConfig = new Configuration(getResources().getConfiguration());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateWindowColors();
    }

    @CallSuper
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden && this.isVisible()) {
            updateWindowColors();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int diff = newConfig.diff(prevConfig);
        if ((diff & ActivityInfo.CONFIG_UI_MODE) != 0) {
            updateWindowColors();
        }

        prevConfig = new Configuration(newConfig);
    }

    public abstract void updateWindowColors();
}
