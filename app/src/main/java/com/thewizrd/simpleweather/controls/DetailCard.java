package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.core.graphics.ColorUtils;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.thewizrd.shared_resources.controls.DetailItemViewModel;
import com.thewizrd.shared_resources.icons.WeatherIconsManager;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.CardWeatherDetailBinding;

public class DetailCard extends LinearLayout {
    private CardWeatherDetailBinding binding;
    private float mShadowRadius;
    private float mShadowDx;
    private float mShadowDy;
    private @ColorInt
    int mShadowColor;
    private MaterialShapeDrawable bgDrawable;

    private Configuration currentConfig;

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
        this.currentConfig = new Configuration(context.getResources().getConfiguration());
        setOrientation(VERTICAL);

        LayoutInflater inflater = LayoutInflater.from(context);
        binding = CardWeatherDetailBinding.inflate(inflater, this, true);
        bgDrawable = new MaterialShapeDrawable(
                ShapeAppearanceModel.builder(context, R.style.ShapeAppearance_Material_MediumComponent, 0)
                        .build());

        int height = context.getResources().getDimensionPixelSize(R.dimen.detail_card_height);
        this.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));
        this.setBackground(bgDrawable);
        this.setBackgroundColor(0xB3FFFFFF);

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

        updateColors();
    }

    private void updateColors() {
        final int systemNightMode = currentConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        final boolean isNightMode = systemNightMode == Configuration.UI_MODE_NIGHT_YES;

        setBackgroundColor(isNightMode ? Colors.BLACK : Colors.WHITE);
        ImageViewCompat.setImageTintList(binding.detailIcon, ColorStateList.valueOf(isNightMode ? Colors.SIMPLEBLUELIGHT : Colors.SIMPLEBLUEDARK));
        setStrokeColor(ColorUtils.setAlphaComponent(isNightMode ? Colors.LIGHTGRAY : Colors.BLACK, 0x40));
        setShadowColor(isNightMode ? Colors.BLACK : Colors.GRAY);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        currentConfig = new Configuration(newConfig);
        updateColors();
    }

    public void bindModel(DetailItemViewModel model) {
        binding.setViewModel(model);
        binding.executePendingBindings();

        WeatherIconsManager wim = WeatherIconsManager.getInstance();
        if (binding.detailIcon.getIconProvider() != null) {
            binding.detailIcon.setShowAsMonochrome(wim.shouldUseMonochrome(binding.detailIcon.getIconProvider()));
        } else {
            binding.detailIcon.setShowAsMonochrome(wim.shouldUseMonochrome());
        }
    }

    @Override
    public void setBackgroundColor(@ColorInt int color) {
        bgDrawable.setFillColor(ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 0xB3)));
    }

    public void setStrokeColor(@ColorInt int color) {
        bgDrawable.setStrokeColor(ColorStateList.valueOf(color));
    }

    public void setStrokeWidth(float strokeWidth) {
        bgDrawable.setStrokeWidth(strokeWidth);
    }

    public void setShadowColor(@ColorInt int color) {
        binding.detailLabel.setShadowLayer(mShadowRadius, mShadowDx, mShadowDy, color);
        binding.detailValue.setShadowLayer(mShadowRadius, mShadowDx, mShadowDy, color);
    }

    public void setShadowLayer(float radius, float dx, float dy, @ColorInt int color) {
        mShadowRadius = radius;
        mShadowDx = dx;
        mShadowDy = dy;
        mShadowColor = color;

        binding.detailLabel.setShadowLayer(radius, dx, dy, color);
        binding.detailValue.setShadowLayer(radius, dx, dy, color);
    }
}
