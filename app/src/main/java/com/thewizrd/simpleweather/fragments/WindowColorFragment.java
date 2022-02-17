package com.thewizrd.simpleweather.fragments;

import android.os.Bundle;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.thewizrd.simpleweather.helpers.WindowColorManager;

public abstract class WindowColorFragment extends CustomFragment implements WindowColorManager {

    @CallSuper
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                DefaultLifecycleObserver.super.onStart(owner);
                updateWindowColors();
            }
        });
    }

    public abstract void updateWindowColors();
}
