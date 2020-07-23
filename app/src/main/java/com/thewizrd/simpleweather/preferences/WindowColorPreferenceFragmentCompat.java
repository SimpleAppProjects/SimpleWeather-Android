package com.thewizrd.simpleweather.preferences;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.simpleweather.helpers.WindowColorManager;

public abstract class WindowColorPreferenceFragmentCompat extends CustomPreferenceFragmentCompat
        implements OnBackPressedFragmentListener, WindowColorManager {

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            private void updateColors() {
                updateWindowColors();
            }
        });
    }

    public void updateWindowColors() {
    }
}
