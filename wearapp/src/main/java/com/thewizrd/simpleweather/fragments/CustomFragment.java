package com.thewizrd.simpleweather.fragments;

import android.content.Context;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

public abstract class CustomFragment extends Fragment {

    private FragmentActivity mActivity;

    public final FragmentActivity getFragmentActivity() {
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
        mActivity = (FragmentActivity) context;
    }

    @Override
    public void onDetach() {
        mActivity = null;
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        mActivity = null;
        super.onDestroy();
    }
}
