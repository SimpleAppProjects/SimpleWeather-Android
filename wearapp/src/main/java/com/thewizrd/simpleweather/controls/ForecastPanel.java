package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.utils.ContextUtils;
import com.thewizrd.simpleweather.databinding.ForecastItemBinding;

import java.util.List;

public class ForecastPanel extends LinearLayout {
    private int maxItemCount = 4;

    public ForecastPanel(Context context) {
        super(context);
        initialize(context);
    }

    public ForecastPanel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public ForecastPanel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    public ForecastPanel(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initialize(Context context) {
        setOrientation(HORIZONTAL);
        removeAllViews();
    }

    @Override
    public void setOrientation(int orientation) {
        super.setOrientation(HORIZONTAL);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int containerWidth = getMeasuredWidth();
        maxItemCount = (int) Math.max(4, containerWidth / ContextUtils.dpToPx(getContext(), 50f));
    }

    public void bindModel(List<ForecastItemViewModel> forecasts) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (forecasts != null && !forecasts.isEmpty()) {
            final int itemCount = Math.min(forecasts.size(), maxItemCount);

            for (int i = 0; i < itemCount; i++) {
                View view = getChildAt(i);
                ForecastItemBinding item;

                if (view != null) {
                    item = DataBindingUtil.getBinding(view);
                    if (item == null) {
                        item = ForecastItemBinding.bind(view);
                    }
                } else {
                    item = ForecastItemBinding.inflate(inflater);
                }

                ForecastItemViewModel model = forecasts.get(i);
                item.setViewModel(model);

                if (getChildAt(i) == null) {
                    addView(item.getRoot());
                }
            }

            removeViews(itemCount, getChildCount() - itemCount);
        } else {
            removeAllViews();
        }
    }
}
