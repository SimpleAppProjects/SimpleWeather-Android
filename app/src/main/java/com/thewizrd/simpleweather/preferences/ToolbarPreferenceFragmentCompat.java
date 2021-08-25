package com.thewizrd.simpleweather.preferences;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.transition.MaterialFadeThrough;
import com.thewizrd.shared_resources.helpers.ContextUtils;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentSettingsBinding;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;
import com.thewizrd.simpleweather.utils.MaterialShapeDrawableUtils;

public abstract class ToolbarPreferenceFragmentCompat extends WindowColorPreferenceFragmentCompat {

    private FragmentSettingsBinding binding;

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

    protected abstract @StringRes
    int getTitle();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setExitTransition(new MaterialFadeThrough());
        setEnterTransition(new MaterialFadeThrough());
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

        Context context = root.getContext();
        Drawable navIcon = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_arrow_back_white_24dp)).mutate();
        DrawableCompat.setTint(navIcon, ContextCompat.getColor(context, R.color.invButtonColorText));
        binding.toolbar.setNavigationIcon(navIcon);

        CoordinatorLayout.LayoutParams lp = new CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setBehavior(new AppBarLayout.ScrollingViewBehavior());
        root.addView(inflatedView, lp);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Toolbar
        binding.toolbar.setTitle(getTitle());
        binding.appBar.setLiftOnScrollTargetViewId(getListView().getId());
        getListView().addOnScrollListener(new RecyclerView.OnScrollListener() {
            boolean mShouldLift = false;

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                boolean shouldLift = recyclerView.canScrollVertically(-1) || recyclerView.getScrollY() > 0 || recyclerView.computeVerticalScrollOffset() > 0;
                if (mShouldLift != shouldLift) {
                    recyclerView.postOnAnimationDelayed(() -> updateWindowColors(), 150);
                    mShouldLift = shouldLift;
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public final void updateWindowColors() {
        super.updateWindowColors();
        updateWindowColors(getSettingsManager().getUserThemeMode());
    }

    protected final void updateWindowColors(UserThemeMode mode) {
        int backgroundColor = ContextUtils.getColor(getAppCompatActivity(), android.R.attr.colorBackground);
        int statusBarColor = ContextUtils.getColor(getAppCompatActivity(), R.attr.colorSurface);
        if (mode == UserThemeMode.AMOLED_DARK) {
            backgroundColor = statusBarColor = Colors.BLACK;
        }

        binding.coordinatorLayout.setBackgroundColor(backgroundColor);
        if (binding.appBar.getBackground() instanceof MaterialShapeDrawable) {
            MaterialShapeDrawable materialShapeDrawable = (MaterialShapeDrawable) binding.appBar.getBackground();
            materialShapeDrawable.setFillColor(ColorStateList.valueOf(statusBarColor));
            statusBarColor = MaterialShapeDrawableUtils.getTintColor(materialShapeDrawable, binding.appBar.getContext());
        } else {
            binding.appBar.setBackgroundColor(statusBarColor);
        }
        binding.coordinatorLayout.setStatusBarBackgroundColor(statusBarColor);
    }
}
