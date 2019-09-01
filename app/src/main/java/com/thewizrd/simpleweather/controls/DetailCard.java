package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.card.MaterialCardView;
import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.simpleweather.R;

public class DetailCard extends MaterialCardView {
    private TextView detailLabel;
    private TextView detailIcon;
    private TextView detailValue;

    public DetailCard(Context context) {
        super(context);
        initialize(context);
    }

    public DetailCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public DetailCard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.card_weather_detail, this);

        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics());
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        setCardBackgroundColor(0x10FFFFFF);
        setCardElevation(0);
        setUseCompatPadding(false);

        detailLabel = view.findViewById(R.id.detail_label);
        detailIcon = view.findViewById(R.id.detail_icon);
        detailValue = view.findViewById(R.id.detail_value);
    }

    public void setDetails(DetailItemViewModel viewModel) {
        detailLabel.setText(viewModel.getLabel());
        detailIcon.setText(viewModel.getIcon());
        detailValue.setText(viewModel.getValue());
        detailIcon.setRotation(viewModel.getIconRotation());
    }

    @Override
    public void setBackgroundColor(@ColorInt int color) {
        setCardBackgroundColor(ColorUtils.setAlphaComponent(color, 0x10));
    }

    public void setTextColor(@ColorInt int color) {
        detailLabel.setTextColor(color);
        detailIcon.setTextColor(color);
        detailValue.setTextColor(color);
    }
}
