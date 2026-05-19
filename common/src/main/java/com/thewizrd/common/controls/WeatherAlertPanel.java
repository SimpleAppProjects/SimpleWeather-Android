package com.thewizrd.common.controls;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.core.text.method.LinkMovementMethodCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.transition.ChangeBounds;
import androidx.transition.Fade;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.thewizrd.shared_resources.R;
import com.thewizrd.common.databinding.WeatherAlertPanelBinding;

public class WeatherAlertPanel extends FrameLayout {
    /**
     * State indicating the group is expanded.
     */
    private static final int[] GROUP_EXPANDED_STATE_SET = {R.attr.state_expanded};

    private WeatherAlertPanelBinding binding;

    private boolean expandable = true;
    private boolean expanded = false;

    private View.OnClickListener onToggleListener = null;

    private TransitionSet transitionSet;

    public WeatherAlertPanel(Context context) {
        super(context);
        initialize(context);
    }

    public WeatherAlertPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public WeatherAlertPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    public void setOnToggleListener(View.OnClickListener listener) {
        this.onToggleListener = listener;
    }

    private void initialize(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        binding = WeatherAlertPanelBinding.inflate(inflater, this, true);

        this.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        binding.headerCard.setOnClickListener(v -> toggle());
        binding.bodyTextview.setMovementMethod(LinkMovementMethodCompat.getInstance());

        transitionSet = new TransitionSet()
                .setDuration(250)
                .addTransition(
                        new ChangeBounds()
                                .setInterpolator(new AccelerateDecelerateInterpolator())
                )
                .addTransition(
                        new Fade()
                                .setInterpolator(new FastOutSlowInInterpolator())
                );
    }

    public boolean isExpandable() {
        return expandable;
    }

    public void setExpandable(boolean expandable) {
        this.expandable = expandable;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        if (this.expanded != expanded) {
            toggle();
        }
    }

    public void toggle() {
        if (isExpandable() && isEnabled()) {
            TransitionManager.beginDelayedTransition(this, transitionSet);

            expanded = !expanded;
            binding.bodyTextview.setVisibility(expanded ? View.VISIBLE : View.GONE);
            refreshDrawableState();

            if (onToggleListener != null) {
                onToggleListener.onClick(this);
            }
        }
    }

    public void bindModel(WeatherAlertViewModel model) {
        // Reset expanded state
        setExpandable(true);
        setExpanded(false);

        binding.setViewModel(model);
        binding.executePendingBindings();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);

        if (isExpanded()) {
            mergeDrawableStates(drawableState, GROUP_EXPANDED_STATE_SET);
        }

        return drawableState;
    }
}
