package com.thewizrd.simpleweather.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.wear.widget.SwipeDismissFrameLayout;

import com.thewizrd.simpleweather.databinding.ActivitySettingsBinding;

public class SwipeDismissPreferenceFragment extends PreferenceFragment {
    private Activity mActivity;
    private ActivitySettingsBinding binding;
    private SwipeDismissFrameLayout.Callback swipeCallback;

    public final Activity getParentActivity() {
        return mActivity;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
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
        return binding != null && mActivity != null;
    }

    @SuppressLint("RestrictedApi")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        binding = ActivitySettingsBinding.inflate(inflater, container, false);
        View inflatedView = super.onCreateView(inflater, container, savedInstanceState);

        binding.swipeLayout.addView(inflatedView);
        binding.swipeLayout.setSwipeable(true);
        swipeCallback = new SwipeDismissFrameLayout.Callback() {
            @Override
            public void onDismissed(SwipeDismissFrameLayout layout) {
                if (mActivity != null) mActivity.onBackPressed();
            }
        };
        binding.swipeLayout.addCallback(swipeCallback);
        binding.swipeLayout.requestFocus();

        return binding.swipeLayout;
    }

    @Override
    public void onDestroyView() {
        binding.swipeLayout.removeCallback(swipeCallback);
        binding = null;
        super.onDestroyView();
    }
}
