package com.thewizrd.simpleweather.fragments;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.thewizrd.simpleweather.helpers.SystemBarColorManager;
import com.thewizrd.simpleweather.helpers.WindowColorManager;

public abstract class WindowColorFragment extends CustomFragment implements WindowColorManager {

    private Configuration currentConfig;

    private SystemBarColorManager mSysBarColorsIface;

    public final SystemBarColorManager getSysBarColorMgr() {
        return mSysBarColorsIface;
    }

    protected void postToViewQueue(Runnable runnable) {
        if (getView() != null) {
            getView().post(runnable);
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mSysBarColorsIface = (SystemBarColorManager) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mSysBarColorsIface = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSysBarColorsIface = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        currentConfig = new Configuration(getResources().getConfiguration());
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
            postToViewQueue(new Runnable() {
                @Override
                public void run() {
                    updateWindowColors();
                }
            });
        }
    }

    public final Configuration getCurrentConfiguration() {
        if (currentConfig == null) {
            currentConfig = new Configuration(getResources().getConfiguration());
        }

        return currentConfig;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int diff = newConfig.diff(currentConfig);
        currentConfig = new Configuration(newConfig);
        if ((diff & ActivityInfo.CONFIG_UI_MODE) != 0 || (diff & ActivityInfo.CONFIG_ORIENTATION) != 0) {
            if (!this.isHidden() && this.isVisible()) {
                updateWindowColors();
            }
        }
    }

    public abstract void updateWindowColors();
}
