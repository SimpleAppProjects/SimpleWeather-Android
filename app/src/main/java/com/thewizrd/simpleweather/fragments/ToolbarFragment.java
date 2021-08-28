package com.thewizrd.simpleweather.fragments;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.navigation.Navigation;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.transition.MaterialFadeThrough;
import com.thewizrd.shared_resources.helpers.ContextUtils;
import com.thewizrd.shared_resources.helpers.OnBackPressedFragmentListener;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentToolbarLayoutBinding;

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

    protected @IdRes
    int getScrollTargetViewId() {
        return View.NO_ID;
    }

    @CallSuper
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialFadeThrough());
        setExitTransition(new MaterialFadeThrough());
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

        return root;
    }

    @CallSuper
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.appBar.setLiftOnScrollTargetViewId(getScrollTargetViewId());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @CallSuper
    public void updateWindowColors() {
        int backgroundColor = ContextUtils.getColor(getAppCompatActivity(), android.R.attr.colorBackground);
        int statusBarColor = ContextUtils.getColor(getAppCompatActivity(), R.attr.colorSurface);
        if (getSettingsManager().getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
            backgroundColor = statusBarColor = Colors.BLACK;
        }

        binding.rootView.setBackgroundColor(backgroundColor);
        if (binding.appBar.getBackground() instanceof MaterialShapeDrawable) {
            MaterialShapeDrawable materialShapeDrawable = (MaterialShapeDrawable) binding.appBar.getBackground();
            materialShapeDrawable.setFillColor(ColorStateList.valueOf(statusBarColor));
        } else {
            binding.appBar.setBackgroundColor(statusBarColor);
        }
    }
}
