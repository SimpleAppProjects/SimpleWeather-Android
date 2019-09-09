package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
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
    private float mShadowRadius;
    private float mShadowDx;
    private float mShadowDy;
    private @ColorInt
    int mShadowColor;

    public float getShadowRadius() {
        return mShadowRadius;
    }

    public float getShadowDx() {
        return mShadowDx;
    }

    public float getShadowDy() {
        return mShadowDy;
    }

    @ColorInt
    public int getShadowColor() {
        return mShadowColor;
    }

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

        int height = context.getResources().getDimensionPixelSize(R.dimen.detail_card_height);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        setCardBackgroundColor(0xB3FFFFFF);
        setCardElevation(0);
        setUseCompatPadding(false);

        detailLabel = view.findViewById(R.id.detail_label);
        detailIcon = view.findViewById(R.id.detail_icon);
        detailValue = view.findViewById(R.id.detail_value);

        Resources.Theme currentTheme = context.getTheme();
        TypedArray array;

        array = currentTheme.obtainStyledAttributes(R.style.ShadowText, new int[]{android.R.attr.shadowRadius});
        mShadowRadius = array.getFloat(0, 0);
        array.recycle();

        array = currentTheme.obtainStyledAttributes(R.style.ShadowText, new int[]{android.R.attr.shadowDx});
        mShadowDx = array.getFloat(0, 0);
        array.recycle();

        array = currentTheme.obtainStyledAttributes(R.style.ShadowText, new int[]{android.R.attr.shadowDy});
        mShadowDy = array.getFloat(0, 0);
        array.recycle();

        array = currentTheme.obtainStyledAttributes(R.style.ShadowText, new int[]{android.R.attr.shadowColor});
        mShadowColor = array.getColor(0, 0);
        array.recycle();
    }

    public void setDetails(DetailItemViewModel viewModel) {
        detailLabel.setText(viewModel.getLabel());
        detailIcon.setText(viewModel.getIcon());
        detailValue.setText(viewModel.getValue());
        detailIcon.setRotation(viewModel.getIconRotation());
    }

    @Override
    public void setBackgroundColor(@ColorInt int color) {
        setCardBackgroundColor(ColorUtils.setAlphaComponent(color, 0xB3));
    }

    public void setTextColor(@ColorInt int color) {
        detailLabel.setTextColor(color);
        detailIcon.setTextColor(color);
        detailValue.setTextColor(color);
    }

    public void setShadowColor(@ColorInt int color) {
        detailLabel.setShadowLayer(mShadowRadius, mShadowDx, mShadowDy, color);
        detailIcon.setShadowLayer(mShadowRadius, mShadowDx, mShadowDy, color);
        detailValue.setShadowLayer(mShadowRadius, mShadowDx, mShadowDy, color);
    }

    public void setShadowLayer(float radius, float dx, float dy, @ColorInt int color) {
        mShadowRadius = radius;
        mShadowDx = dx;
        mShadowDy = dy;
        mShadowColor = color;

        detailLabel.setShadowLayer(radius, dx, dy, color);
        detailIcon.setShadowLayer(radius, dx, dy, color);
        detailValue.setShadowLayer(radius, dx, dy, color);
    }
}
