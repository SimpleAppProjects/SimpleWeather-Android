package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.card.MaterialCardView;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.ColorsUtils;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.LocationPanelBinding;

public class LocationPanel extends MaterialCardView {
    private LocationPanelBinding binding;
    private Drawable colorDrawable;
    private Drawable overlayDrawable;
    private RequestManager mGlide;

    public LocationPanel(@NonNull Context context) {
        super(context);
        initialize(context);
    }

    public LocationPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public LocationPanel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    public Drawable getColorDrawable() {
        return colorDrawable;
    }

    private void initialize(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        binding = LocationPanelBinding.inflate(inflater, this, true);

        int height = context.getResources().getDimensionPixelSize(R.dimen.location_panel_height);
        this.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));

        colorDrawable = new ColorDrawable(Colors.SIMPLEBLUEMEDIUM);
        overlayDrawable = ContextCompat.getDrawable(context, R.drawable.background_overlay);

        // NOTE: Bug: Explicitly set tintmode on Lollipop devices
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP)
            binding.progressBar.setIndeterminateTintMode(PorterDuff.Mode.SRC_IN);

        // Remove extra (compat) padding on pre-Lollipop devices
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            setPreventCornerOverlap(false);
            setCardElevation(0f);
            setMaxCardElevation(0f);
        }

        mGlide = Glide.with(this);
        showLoading(true);
    }

    public void setWeatherBackground(LocationPanelViewModel panelView) {
        setWeatherBackground(panelView, false);
    }

    public void setWeatherBackground(LocationPanelViewModel panelView, boolean skipCache) {
        // Background
        mGlide.asBitmap()
                .load(panelView.getBackground())
                .apply(RequestOptions
                        .centerCropTransform()
                        .format(DecodeFormat.PREFER_RGB_565)
                        .error(colorDrawable)
                        .placeholder(colorDrawable)
                        .skipMemoryCache(skipCache))
                .transition(BitmapTransitionOptions.withCrossFade())
                .into(new BitmapImageViewTarget(binding.imageView) {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        super.onResourceReady(resource, transition);
                        Palette p = Palette.from(resource).generate();
                        int textColor = Colors.WHITE;
                        int shadowColor = Colors.BLACK;
                        if (ColorsUtils.isSuperLight(p)) {
                            textColor = Colors.BLACK;
                            shadowColor = Colors.GRAY;
                        }

                        binding.imageOverlay.setBackground(overlayDrawable);

                        setTextColor(textColor);
                        setTextShadowColor(shadowColor);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        binding.imageOverlay.setBackground(null);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        super.onLoadCleared(placeholder);
                        binding.imageOverlay.setBackground(null);
                    }

                    @Override
                    public void onLoadStarted(@Nullable Drawable placeholder) {
                        super.onLoadStarted(placeholder);
                        binding.imageOverlay.setBackground(null);
                    }
                });
    }

    public void clearBackground() {
        mGlide.clear(binding.imageView);
        binding.imageView.setImageDrawable(colorDrawable);
        setTextColor(Colors.WHITE);
    }

    public void bindModel(LocationPanelViewModel model) {
        binding.setViewModel(model);
        binding.executePendingBindings();

        this.setTag(model.getLocationData());

        if (model.getLocationName() != null && getTag() != null)
            showLoading(false);
    }

    public void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? VISIBLE : GONE);
        setClickable(!show);
    }

    public void setTextColor(@ColorInt int color) {
        binding.locationName.setTextColor(color);
        binding.weatherTemp.setTextColor(color);
        binding.weatherIcon.setTextColor(color);
    }

    public void setTextShadowColor(@ColorInt int color) {
        binding.locationName.setShadowLayer(
                binding.locationName.getShadowRadius(), binding.locationName.getShadowDx(), binding.locationName.getShadowDy(), color);
        binding.weatherTemp.setShadowLayer(
                binding.weatherTemp.getShadowRadius(), binding.weatherTemp.getShadowDx(), binding.weatherTemp.getShadowDy(), color);
        binding.weatherIcon.setShadowLayer(
                binding.weatherIcon.getShadowRadius(), binding.weatherIcon.getShadowDx(), binding.weatherIcon.getShadowDy(), color);
    }

    @Override
    public void setDragged(boolean dragged) {
        super.setDragged(dragged);
        setStrokeColor(ActivityUtils.getColor(getContext(), R.attr.colorOnSurface));
        setStrokeWidth(dragged ? (int) ActivityUtils.dpToPx(getContext(), 2) : 0);
    }
}
