package com.thewizrd.simpleweather.preferences;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.simpleweather.helpers.SystemBarColorManager;
import com.thewizrd.simpleweather.helpers.WindowColorManager;

public abstract class WindowColorPreferenceFragmentCompat extends CustomPreferenceFragmentCompat
        implements OnBackPressedFragmentListener, WindowColorManager {

    private SystemBarColorManager mSysBarColorsIface;

    public final SystemBarColorManager getSysBarColorMgr() {
        return mSysBarColorsIface;
    }

    private Configuration currentConfig;

    public Configuration getCurrentConfig() {
        return currentConfig;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mSysBarColorsIface = (SystemBarColorManager) context;
    }

    @Override
    public void onDetach() {
        mSysBarColorsIface = null;
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        currentConfig = new Configuration(getResources().getConfiguration());
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (currentConfig == null) {
            currentConfig = new Configuration(getResources().getConfiguration());
        }

        view.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (isAlive()) {
                    view.getViewTreeObserver().removeOnPreDrawListener(this);
                    updateWindowColors();
                }
                return true;
            }
        });
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

        int diff = newConfig.diff(currentConfig);
        currentConfig = new Configuration(newConfig);
        if ((diff & ActivityInfo.CONFIG_UI_MODE) != 0) {
            if (!this.isHidden() && this.isVisible()) {
                updateWindowColors();
                getListView().setAdapter(getListView().getAdapter());
            }
        } else if ((diff & ActivityInfo.CONFIG_ORIENTATION) != 0) {
            if (!this.isHidden() && this.isVisible()) {
                updateWindowColors();
            }
        }
    }

    @Override
    @CallSuper
    public void updateWindowColors() {
        if (!isAlive()) return;
        if (currentConfig == null) {
            currentConfig = new Configuration(getResources().getConfiguration());
        }
    }
}
