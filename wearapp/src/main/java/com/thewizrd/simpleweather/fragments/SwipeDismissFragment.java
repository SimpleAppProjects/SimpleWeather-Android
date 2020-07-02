package com.thewizrd.simpleweather.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.wear.widget.SwipeDismissFrameLayout;

import com.thewizrd.simpleweather.databinding.ActivitySettingsBinding;

public class SwipeDismissFragment extends Fragment {
    private FragmentActivity mActivity;

    private ActivitySettingsBinding binding;
    private SwipeDismissFrameLayout.Callback swipeCallback;

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

    protected void runOnUiThread(Runnable action) {
        if (mActivity != null && isAlive())
            mActivity.runOnUiThread(action);
    }

    public boolean isAlive() {
        return binding != null && mActivity != null && getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED);
    }

    @SuppressLint("RestrictedApi")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        binding = ActivitySettingsBinding.inflate(inflater, container, false);

        binding.swipeLayout.setSwipeable(true);
        swipeCallback = new SwipeDismissFrameLayout.Callback() {
            @Override
            public void onDismissed(SwipeDismissFrameLayout layout) {
                if (mActivity != null) mActivity.onBackPressed();
            }
        };
        binding.swipeLayout.addCallback(swipeCallback);

        return binding.swipeLayout;
    }

    @Override
    public void onDestroyView() {
        binding.swipeLayout.removeCallback(swipeCallback);
        binding = null;
        super.onDestroyView();
    }
}

