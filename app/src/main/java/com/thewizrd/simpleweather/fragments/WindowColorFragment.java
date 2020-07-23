package com.thewizrd.simpleweather.fragments;

import android.os.Bundle;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.thewizrd.simpleweather.helpers.WindowColorManager;

public abstract class WindowColorFragment extends CustomFragment implements WindowColorManager {

    @CallSuper
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

    public abstract void updateWindowColors();
}
