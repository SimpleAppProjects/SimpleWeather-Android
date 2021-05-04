package com.thewizrd.simpleweather.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

public abstract class LifecyclePreferenceFragment extends PreferenceFragment
        implements LifecycleOwner {
    private final LifecycleRegistry mLifecycle = new LifecycleRegistry(this);
    // This is initialized in performCreateView and unavailable outside of the
    // onCreateView/onDestroyView lifecycle
    @Nullable
    private FragmentViewLifecycleOwner mViewLifecycleOwner;

    @Override
    @NonNull
    public Lifecycle getLifecycle() {
        return mLifecycle;
    }

    @MainThread
    @NonNull
    public LifecycleOwner getViewLifecycleOwner() {
        if (mViewLifecycleOwner == null) {
            throw new IllegalStateException("Can't access the Fragment View's LifecycleOwner when "
                    + "getView() is null i.e., before onCreateView() or after onDestroyView()");
        }
        return mViewLifecycleOwner;
    }

    @CallSuper
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        super.onCreate(savedInstanceState);
    }

    @CallSuper
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mViewLifecycleOwner = new FragmentViewLifecycleOwner();
        if (view != null) {
            mViewLifecycleOwner.initialize();
        } else {
            if (mViewLifecycleOwner.isInitialized()) {
                throw new IllegalStateException("Called getViewLifecycleOwner() but "
                        + "onCreateView() returned null");
            }
            mViewLifecycleOwner = null;
        }
        super.onViewCreated(view, savedInstanceState);
    }

    @CallSuper
    @Override
    public void onStart() {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        if (getView() != null) {
            mViewLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START);
        }
        super.onStart();
    }

    @CallSuper
    @Override
    public void onResume() {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        if (getView() != null) {
            mViewLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        }
        super.onResume();
    }

    @CallSuper
    @Override
    public void onPause() {
        if (getView() != null) {
            mViewLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        }
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        super.onPause();
    }

    @CallSuper
    @Override
    public void onStop() {
        if (getView() != null) {
            mViewLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        }
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        super.onStop();
    }

    @CallSuper
    @Override
    public void onDestroyView() {
        if (getView() != null) {
            mViewLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        }
        super.onDestroyView();
    }

    @CallSuper
    @Override
    public void onDestroy() {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY);
        super.onDestroy();
    }

    @CallSuper
    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (getView() != null) {
            mViewLifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        }
    }
}
