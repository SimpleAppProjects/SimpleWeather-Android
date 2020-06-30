package com.thewizrd.simpleweather.fragments;

import android.content.Context;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

public abstract class CustomFragment extends Fragment {

    private AppCompatActivity mActivity;

    public final AppCompatActivity getAppCompatActivity() {
        return mActivity;
    }

    protected final void runOnUiThread(@NonNull final Runnable action) {
        if (mActivity != null && isAlive())
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isAlive()) {
                        action.run();
                    }
                }
            });
    }

    @CallSuper
    public boolean isAlive() {
        return mActivity != null && getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = (AppCompatActivity) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mActivity = null;
    }
}
