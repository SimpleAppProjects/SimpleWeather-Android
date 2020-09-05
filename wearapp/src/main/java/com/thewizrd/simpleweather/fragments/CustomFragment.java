package com.thewizrd.simpleweather.fragments;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.thewizrd.shared_resources.lifecycle.LifecycleAwareFragment;

public abstract class CustomFragment extends LifecycleAwareFragment {

    private FragmentActivity mActivity;

    public final FragmentActivity getFragmentActivity() {
        return mActivity;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = (FragmentActivity) context;
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
