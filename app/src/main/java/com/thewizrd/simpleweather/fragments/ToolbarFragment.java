package com.thewizrd.simpleweather.fragments;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

        // Toolbar
        binding.toolbar.setTitle(getTitle());

        // For landscape orientation
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                layoutParams.setMargins(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0);
                return insets;
            }
        });

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

        int color = ActivityUtils.getColor(getAppCompatActivity(), android.R.attr.colorBackground);
        if (Settings.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
            color = Colors.BLACK;
        }

        binding.rootView.setBackgroundColor(color);
        binding.appBar.setBackgroundColor(color);
        binding.rootView.setStatusBarBackgroundColor(color);
    }
}
