package com.thewizrd.simpleweather.helpers;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class WindowInsetsFrameLayout extends FrameLayout {
    private Rect windowInsets;
    private Rect tempInsets;

    public WindowInsetsFrameLayout(@NonNull Context context) {
        super(context);
        initialize();
    }

    public WindowInsetsFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public WindowInsetsFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WindowInsetsFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize();
    }

    private void initialize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOnHierarchyChangeListener(new OnHierarchyChangeListener() {
                @Override
                public void onChildViewAdded(View parent, View child) {
                    requestApplyInsets();
                }

                @Override
                public void onChildViewRemoved(View parent, View child) {

                }
            });
        } else {
            windowInsets = new Rect();
            tempInsets = new Rect();

            setOnHierarchyChangeListener(new OnHierarchyChangeListener() {
                @Override
                public void onChildViewAdded(View parent, View child) {
                    tempInsets.set(windowInsets);
                    WindowInsetsFrameLayout.super.fitSystemWindows(tempInsets);
                }

                @Override
                public void onChildViewRemoved(View parent, View child) {

                }
            });
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WindowInsets dispatchApplyWindowInsets(WindowInsets insets) {
        if (!insets.isConsumed()) {
            // each child gets a fresh set of window insets
            // to consume
            final int count = getChildCount();
            for (int i = 0; i < count; i++) {
                WindowInsets freshSets = new WindowInsets(insets);
                getChildAt(i).dispatchApplyWindowInsets(freshSets);
            }
        }
        return insets;
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            windowInsets.set(insets);
            super.fitSystemWindows(insets);
            return true;
        } else {
            return super.fitSystemWindows(insets);
        }
    }
}
