package com.thewizrd.simpleweather.preferences;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.transition.Transition;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentSettingsBinding;
import com.thewizrd.simpleweather.helpers.SystemBarColorManager;
import com.thewizrd.simpleweather.helpers.TransitionHelper;
import com.thewizrd.simpleweather.helpers.WindowColorManager;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;

public abstract class WindowColorPreferenceFragmentCompat extends CustomPreferenceFragmentCompat
        implements OnBackPressedFragmentListener, WindowColorManager {

    private FragmentSettingsBinding binding;
    private SystemBarColorManager mSysBarColorsIface;

    private Configuration currentConfig;

    public final AppBarLayout getAppBarLayout() {
        return binding.appBar;
    }

    public final CoordinatorLayout getRootView() {
        return binding.coordinatorLayout;
    }

    public final Toolbar getToolbar() {
        return binding.toolbar;
    }

    @NonNull
    @Override
    public SnackbarManager createSnackManager() {
        SnackbarManager mSnackMgr = new SnackbarManager(binding.getRoot());
        mSnackMgr.setSwipeDismissEnabled(true);
        mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);
        return mSnackMgr;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mSysBarColorsIface = (SystemBarColorManager) context;
    }

    @Override
    public void onDetach() {
        mSysBarColorsIface = null;
        super.onDetach();
    }

    @Override
    public boolean isAlive() {
        return binding != null && super.isAlive();
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    protected abstract @StringRes
    int getTitle();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TransitionHelper.onCreate(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        currentConfig = new Configuration(getResources().getConfiguration());

        // Toolbar
        binding.toolbar.setTitle(getTitle());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        ViewGroup root = binding.coordinatorLayout;
        View inflatedView = super.onCreateView(inflater, container, savedInstanceState);

        binding.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getAppCompatActivity().onBackPressed();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBar, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewCompat.setPaddingRelative(v, insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
                return insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), 0);
            }
        });

        CoordinatorLayout.LayoutParams lp = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setBehavior(new AppBarLayout.ScrollingViewBehavior());
        root.addView(inflatedView, lp);

        ViewCompat.setOnApplyWindowInsetsListener(inflatedView, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewCompat.setPaddingRelative(v, insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0);
                return insets;
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (currentConfig == null) {
            currentConfig = new Configuration(getResources().getConfiguration());
        }

        binding.getRoot().getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (isAlive()) {
                    binding.getRoot().getViewTreeObserver().removeOnPreDrawListener(this);
                    updateWindowColors();
                }
                return true;
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViewGroupCompat.setTransitionGroup(getRootView(), false);
            ViewGroupCompat.setTransitionGroup(getAppBarLayout(), false);
            ViewGroupCompat.setTransitionGroup(getToolbar(), false);
            ViewGroupCompat.setTransitionGroup(getListView(), true);

            TransitionHelper.onViewCreated(this, (ViewGroup) view.getParent(), new TransitionHelper.OnPrepareTransitionListener() {
                @Override
                public void prepareTransitions(@Nullable Transition enterTransition, @Nullable Transition exitTransition, @Nullable Transition reenterTransition, @Nullable Transition returnTransition) {
                    if (enterTransition != null) {
                        enterTransition.addTarget(getListView());
                    }
                    if (exitTransition != null) {
                        exitTransition.addTarget(getListView());
                    }
                    if (reenterTransition != null) {
                        reenterTransition.addTarget(getListView());
                    }
                    if (returnTransition != null) {
                        returnTransition.addTarget(getListView());
                    }
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    @CallSuper
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden && this.isVisible()) {
            updateWindowColors();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int diff = newConfig.diff(currentConfig);
        currentConfig = new Configuration(newConfig);
        if ((diff & ActivityInfo.CONFIG_UI_MODE) != 0) {
            if (!this.isHidden() && this.isVisible()) {
                updateWindowColors();
                getListView().setAdapter(getListView().getAdapter());
            }
        } else if ((diff & ActivityInfo.CONFIG_ORIENTATION) != 0) {
            if (!this.isHidden() && this.isVisible()) {
                updateWindowColors();
            }
        }
    }

    public final void updateWindowColors() {
        updateWindowColors(Settings.getUserThemeMode());
    }

    protected final void updateWindowColors(UserThemeMode mode) {
        if (!isAlive()) return;
        if (currentConfig == null) {
            currentConfig = new Configuration(getResources().getConfiguration());
        }
        final int currentNightMode = currentConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;

        int color = ActivityUtils.getColor(getAppCompatActivity(), R.attr.colorPrimary);
        int bg_color = ActivityUtils.getColor(getAppCompatActivity(), android.R.attr.colorBackground);
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            if (mode == UserThemeMode.AMOLED_DARK) {
                bg_color = Colors.BLACK;
            } else {
                bg_color = ActivityUtils.getColor(getAppCompatActivity(), android.R.attr.colorBackground);
            }
            color = bg_color;
        }
        binding.coordinatorLayout.setBackgroundColor(bg_color);
        binding.appBar.setBackgroundColor(color);
        binding.coordinatorLayout.setStatusBarBackgroundColor(color);
        if (mSysBarColorsIface != null) {
            mSysBarColorsIface.setSystemBarColors(bg_color, color, color, color);
        }
    }
}
