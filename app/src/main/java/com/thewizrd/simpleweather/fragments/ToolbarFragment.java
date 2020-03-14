package com.thewizrd.simpleweather.fragments;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Transition;

import com.google.android.material.appbar.AppBarLayout;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentToolbarLayoutBinding;
import com.thewizrd.simpleweather.helpers.SystemBarColorManager;
import com.thewizrd.simpleweather.helpers.TransitionHelper;

public abstract class ToolbarFragment extends WindowColorFragment
        implements OnBackPressedFragmentListener {

    private AppCompatActivity mActivity;
    private SystemBarColorManager mSysBarColorsIface;

    // Views
    private FragmentToolbarLayoutBinding binding;

    public final AppBarLayout getAppBarLayout() {
        return binding.appBar;
    }

    public final CoordinatorLayout getRootView() {
        return binding.rootView;
    }

    public final Toolbar getToolbar() {
        return binding.toolbar;
    }

    public final AppCompatActivity getAppCompatActivity() {
        return mActivity;
    }

    public final SystemBarColorManager getSysBarColorMgr() {
        return mSysBarColorsIface;
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

    @CallSuper
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TransitionHelper.onCreate(this);
    }

    @Override
    @CallSuper
    public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentToolbarLayoutBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivity.onBackPressed();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBar, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewCompat.setPaddingRelative(v, insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
                return insets;
            }
        });

        // Toolbar
        binding.toolbar.setTitle(getTitle());

        return root;
    }

    @CallSuper
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewGroupCompat.setTransitionGroup(getRootView(), false);
        ViewGroupCompat.setTransitionGroup(getAppBarLayout(), false);
        ViewGroupCompat.setTransitionGroup(getToolbar(), false);

        TransitionHelper.onViewCreated(this, (ViewGroup) view.getParent(), new TransitionHelper.OnPrepareTransitionListener() {
            @Override
            public void prepareTransitions(@Nullable Transition enterTransition, @Nullable Transition exitTransition, @Nullable Transition reenterTransition, @Nullable Transition returnTransition) {
                if (enterTransition != null) {
                    enterTransition
                            .addTarget(RecyclerView.class);
                }
                if (exitTransition != null) {
                    exitTransition
                            .addTarget(RecyclerView.class);
                }
                if (reenterTransition != null) {
                    reenterTransition
                            .addTarget(RecyclerView.class);
                }
                if (returnTransition != null) {
                    returnTransition
                            .addTarget(RecyclerView.class);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @CallSuper
    public void updateWindowColors() {
        Configuration config = getCurrentConfiguration();
        final int currentNightMode = config.uiMode & Configuration.UI_MODE_NIGHT_MASK;

        @ColorInt int color = ActivityUtils.getColor(mActivity, R.attr.colorPrimary);
        @ColorInt int bg_color = ActivityUtils.getColor(mActivity, android.R.attr.colorBackground);
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            if (Settings.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
                bg_color = Colors.BLACK;
            } else {
                bg_color = ActivityUtils.getColor(mActivity, android.R.attr.colorBackground);
            }
            color = bg_color;
        }
        binding.rootView.setBackgroundColor(bg_color);
        binding.appBar.setBackgroundColor(color);
        binding.rootView.setStatusBarBackgroundColor(color);
        if (mSysBarColorsIface != null) {
            mSysBarColorsIface.setSystemBarColors(bg_color, color, color, color);
        }
    }
}
