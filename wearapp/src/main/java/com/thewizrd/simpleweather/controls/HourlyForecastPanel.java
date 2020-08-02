package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.simpleweather.databinding.HrforecastItemBinding;

import java.util.List;

public class HourlyForecastPanel extends LinearLayout {
    public HourlyForecastPanel(Context context) {
        super(context);
        initialize(context);
    }

    public HourlyForecastPanel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public HourlyForecastPanel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    public HourlyForecastPanel(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initialize(Context context) {
        setOrientation(VERTICAL);
        removeAllViews();
    }

    @Override
    public void setOrientation(int orientation) {
        super.setOrientation(VERTICAL);
    }

    public void bindModel(List<HourlyForecastItemViewModel> forecasts) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (forecasts != null && !forecasts.isEmpty()) {
            final int itemCount = Math.min(forecasts.size(), 12);

            for (int i = 0; i < itemCount; i++) {
                View view = getChildAt(i);
                HrforecastItemBinding item;

                if (view != null) {
                    item = DataBindingUtil.getBinding(view);
                    if (item == null) {
                        item = HrforecastItemBinding.bind(view);
                    }
                } else {
                    item = HrforecastItemBinding.inflate(inflater);
                }

                HourlyForecastItemViewModel model = forecasts.get(i);
                item.setViewModel(model);

                if (getChildAt(i) == null) {
                    addView(item.getRoot());
                }
            }
        }
    }
}
