package com.thewizrd.simpleweather.fragments;

import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Transition;

import com.google.android.material.appbar.AppBarLayout;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.simpleweather.databinding.FragmentToolbarLayoutBinding;
import com.thewizrd.simpleweather.helpers.TransitionHelper;

public abstract class ToolbarFragment extends WindowColorFragment
        implements OnBackPressedFragmentListener {

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TransitionHelper.onCreate(this);
        }
    }

    @Override
    @CallSuper
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentToolbarLayoutBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigateUp();
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
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
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    @CallSuper
    public void updateWindowColors() {
        if (!isAlive()) return;

        Configuration config = getCurrentConfiguration();
        final int currentNightMode = config.uiMode & Configuration.UI_MODE_NIGHT_MASK;

        @ColorInt int bg_color = ActivityUtils.getColor(getAppCompatActivity(), android.R.attr.colorBackground);
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            if (Settings.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
                bg_color = Colors.BLACK;
            } else {
                bg_color = ActivityUtils.getColor(getAppCompatActivity(), android.R.attr.colorBackground);
            }
        }
        binding.rootView.setBackgroundColor(bg_color);
        binding.appBar.setBackgroundColor(bg_color);
        binding.rootView.setStatusBarBackgroundColor(bg_color);
        if (getSysBarColorMgr() != null) {
            getSysBarColorMgr().setSystemBarColors(bg_color);
        }
    }
}
