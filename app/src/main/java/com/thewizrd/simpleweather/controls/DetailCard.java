package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.card.MaterialCardView;
import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.CardWeatherDetailBinding;

public class DetailCard extends MaterialCardView {
    private CardWeatherDetailBinding binding;
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
        binding = CardWeatherDetailBinding.inflate(inflater, this, true);

        int height = context.getResources().getDimensionPixelSize(R.dimen.detail_card_height);
        this.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        this.setCardBackgroundColor(0xB3FFFFFF);
        this.setCardElevation(0);
        this.setUseCompatPadding(false);

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

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void bindModel(DetailItemViewModel model) {
        binding.setViewModel(model);
        binding.executePendingBindings();
    }

    @Override
    public void setBackgroundColor(@ColorInt int color) {
        setCardBackgroundColor(ColorUtils.setAlphaComponent(color, 0xB3));
    }

    public void setTextColor(@ColorInt int color) {
        binding.detailLabel.setTextColor(color);
        binding.detailIcon.setTextColor(color);
        binding.detailValue.setTextColor(color);
    }

    public void setShadowColor(@ColorInt int color) {
        binding.detailLabel.setShadowLayer(mShadowRadius, mShadowDx, mShadowDy, color);
        binding.detailIcon.setShadowLayer(mShadowRadius, mShadowDx, mShadowDy, color);
        binding.detailValue.setShadowLayer(mShadowRadius, mShadowDx, mShadowDy, color);
    }

    public void setShadowLayer(float radius, float dx, float dy, @ColorInt int color) {
        mShadowRadius = radius;
        mShadowDx = dx;
        mShadowDy = dy;
        mShadowColor = color;

        binding.detailLabel.setShadowLayer(radius, dx, dy, color);
        binding.detailIcon.setShadowLayer(radius, dx, dy, color);
        binding.detailValue.setShadowLayer(radius, dx, dy, color);
    }
}
