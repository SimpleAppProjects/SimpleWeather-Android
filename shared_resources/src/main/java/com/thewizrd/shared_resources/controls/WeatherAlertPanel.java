package com.thewizrd.shared_resources.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.thewizrd.shared_resources.databinding.WeatherAlertPanelBinding;

public class WeatherAlertPanel extends RelativeLayout {
    private WeatherAlertPanelBinding binding;

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

        binding.bodyCard.setVisibility(GONE);
        binding.expandIcon.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.bodyCard.setVisibility(isChecked ? VISIBLE : GONE);
        });
        binding.headerCard.setOnClickListener(v -> {
            binding.expandIcon.setChecked(!binding.expandIcon.isChecked());
        });
    }

    public void bindModel(WeatherAlertViewModel model) {
        binding.setViewModel(model);

        // Reset expanded state
        binding.expandIcon.setChecked(false);

        binding.executePendingBindings();
    }
}
