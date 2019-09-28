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
import androidx.wear.widget.SwipeDismissFrameLayout;

import com.thewizrd.simpleweather.R;

public class SwipeDismissFragment extends Fragment {
    protected FragmentActivity mActivity;
    private SwipeDismissFrameLayout.Callback swipeCallback;
    private SwipeDismissFrameLayout swipeLayout;

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

    protected void runOnUiThread(Runnable action) {
        if (mActivity != null)
            mActivity.runOnUiThread(action);
    }

    @SuppressLint("RestrictedApi")
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        swipeLayout = (SwipeDismissFrameLayout) inflater.inflate(R.layout.activity_settings, container, false);

        swipeLayout.setSwipeable(true);
        swipeCallback = new SwipeDismissFrameLayout.Callback() {
            @Override
            public void onDismissed(SwipeDismissFrameLayout layout) {
                if (mActivity != null) mActivity.onBackPressed();
            }
        };
        swipeLayout.addCallback(swipeCallback);

        return swipeLayout;
    }

    @Override
    public void onDestroyView() {
        swipeLayout.removeCallback(swipeCallback);
        super.onDestroyView();
    }
}

