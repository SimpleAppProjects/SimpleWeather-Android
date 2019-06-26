package com.thewizrd.simpleweather;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.wear.widget.SwipeDismissFrameLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SwipeDismissFragment extends Fragment {
    protected Activity mActivity;
    private SwipeDismissFrameLayout.Callback swipeCallback;
    private SwipeDismissFrameLayout swipeLayout;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (Activity) context;
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

