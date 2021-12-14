package com.thewizrd.shared_resources.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.thewizrd.shared_resources.R;
import com.thewizrd.shared_resources.databinding.WeatherAlertPanelBinding;

public class WeatherAlertPanel extends RelativeLayout {
    /**
     * State indicating the group is expanded.
     */
    private static final int[] GROUP_EXPANDED_STATE_SET = {R.attr.state_expanded};

    private WeatherAlertPanelBinding binding;

    private boolean expandable = true;
    private boolean expanded = false;

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

    private void initialize(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        binding = WeatherAlertPanelBinding.inflate(inflater, this, true);

        this.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        binding.headerCard.setOnClickListener(v -> toggle());
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
            expanded = !expanded;
            binding.bodyCard.setVisibility(expanded ? View.VISIBLE : View.GONE);
            refreshDrawableState();
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
