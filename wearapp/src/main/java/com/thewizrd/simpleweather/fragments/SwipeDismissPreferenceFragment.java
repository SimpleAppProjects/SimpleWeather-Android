package com.thewizrd.simpleweather.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.wear.widget.SwipeDismissFrameLayout;

import com.thewizrd.simpleweather.databinding.ActivitySettingsBinding;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class SwipeDismissPreferenceFragment extends PreferenceFragment {

    @IntDef({
            Toast.LENGTH_SHORT,
            Toast.LENGTH_LONG
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ToastDuration {
    }

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onCreatePreferences(savedInstanceState);
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
        super.onDestroyView();
        binding = null;
    }

    public abstract void onCreatePreferences(@Nullable Bundle savedInstanceState);

    public void showToast(@StringRes int resId, @ToastDuration int duration) {
        if (mActivity != null) {
            mActivity.runOnUiThread(() -> {
                if (mActivity != null && isVisible()) {
                    Toast.makeText(mActivity, resId, duration).show();
                }
            });
        }
    }

    public void showToast(CharSequence message, @ToastDuration int duration) {
        if (mActivity != null) {
            mActivity.runOnUiThread(() -> {
                if (mActivity != null && isVisible()) {
                    Toast.makeText(mActivity, message, duration).show();
                }
            });
        }
    }
}
