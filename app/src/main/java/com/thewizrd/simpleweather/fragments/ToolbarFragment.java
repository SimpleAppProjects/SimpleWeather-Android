package com.thewizrd.simpleweather.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.DarkMode;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.helpers.ActivityUtils;
import com.thewizrd.simpleweather.helpers.SystemBarColorManager;

public abstract class ToolbarFragment extends WindowColorFragment
        implements OnBackPressedFragmentListener {

    private AppCompatActivity mActivity;
    private SystemBarColorManager mSysBarColorsIface;

    // Views
    private AppBarLayout mAppBarLayout;
    private CoordinatorLayout mRootView;
    private Toolbar mToolbar;

    public final AppBarLayout getAppBarLayout() {
        return mAppBarLayout;
    }

    public final CoordinatorLayout getRootView() {
        return mRootView;
    }

    public final Toolbar getToolbar() {
        return mToolbar;
    }

    public final AppCompatActivity getAppCompatActivity() {
        return mActivity;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = (AppCompatActivity) context;
        mSysBarColorsIface = (SystemBarColorManager) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
        mSysBarColorsIface = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mActivity = null;
        mSysBarColorsIface = null;
    }

    protected void runOnUiThread(Runnable action) {
        if (mActivity != null)
            mActivity.runOnUiThread(action);
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    protected abstract @StringRes
    int getTitle();

    @Override
    @CallSuper
    public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_toolbar_layout, container, false);

        mRootView = (CoordinatorLayout) root;
        mAppBarLayout = root.findViewById(R.id.app_bar);
        mToolbar = root.findViewById(R.id.toolbar);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.onBackPressed();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(mAppBarLayout, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewCompat.setPaddingRelative(v, insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
                return insets;
            }
        });

        // Toolbar
        mToolbar.setTitle(getTitle());

        return root;
    }

    @CallSuper
    public void updateWindowColors() {
        int currentNightMode = AppCompatDelegate.getDefaultNightMode();
        int color = ActivityUtils.getColor(mActivity, R.attr.colorPrimary);
        if (currentNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
            if (Settings.getUserThemeMode() == DarkMode.AMOLED_DARK) {
                color = Colors.BLACK;
            } else {
                color = ActivityUtils.getColor(mActivity, android.R.attr.colorBackground);
            }
        }
        int bg_color = color != Colors.BLACK ? ActivityUtils.getColor(mActivity, android.R.attr.colorBackground) : color;
        mAppBarLayout.setBackgroundColor(color);
        mRootView.setStatusBarBackgroundColor(color);
        if (mSysBarColorsIface != null) {
            mSysBarColorsIface.setSystemBarColors(bg_color, color, color, color);
        }
    }
}
