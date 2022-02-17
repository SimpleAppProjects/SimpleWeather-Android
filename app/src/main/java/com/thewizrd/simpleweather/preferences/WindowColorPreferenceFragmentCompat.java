package com.thewizrd.simpleweather.preferences;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

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

        getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                DefaultLifecycleObserver.super.onStart(owner);
                updateWindowColors();
            }
        });
    }

    public void updateWindowColors() {
    }
}
