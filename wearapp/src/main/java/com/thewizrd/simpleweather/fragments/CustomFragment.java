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
        mActivity = null;
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        mActivity = null;
        super.onDestroy();
    }
}
