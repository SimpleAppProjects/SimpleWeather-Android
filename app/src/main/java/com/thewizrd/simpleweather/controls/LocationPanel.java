package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.card.MaterialCardView;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.LocationPanelBinding;
import com.thewizrd.simpleweather.preferences.FeatureSettings;

public class LocationPanel extends MaterialCardView {
    private LocationPanelBinding binding;
    private Drawable colorDrawable;
    private Drawable overlayDrawable;
    private RequestManager mGlide;

    public LocationPanel(@NonNull Context context) {
        super(context);
        initialize(getContext());
    }

    public LocationPanel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(getContext());
    }

    public LocationPanel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(getContext());
    }

    public Drawable getColorDrawable() {
        return colorDrawable;
    }

    private void initialize(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        binding = LocationPanelBinding.inflate(inflater, this, true);

        int height = context.getResources().getDimensionPixelSize(R.dimen.location_panel_height);
        this.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));

        colorDrawable = new ColorDrawable(ContextCompat.getColor(context, R.color.colorSurface));
        overlayDrawable = ContextCompat.getDrawable(context, R.drawable.background_overlay);

        // NOTE: Bug: Explicitly set tintmode on Lollipop devices
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP)
            binding.progressBar.setIndeterminateTintMode(PorterDuff.Mode.SRC_IN);

        // Remove extra (compat) padding on pre-Lollipop devices
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            setPreventCornerOverlap(false);
        }
        setCardElevation(0f);
        setMaxCardElevation(0f);
        setStrokeWidth((int) ActivityUtils.dpToPx(context, 1f));
        setStrokeColor(AppCompatResources.getColorStateList(context, FeatureSettings.isLocationPanelImageEnabled() ? R.color.location_panel_card_stroke_imageon : R.color.location_panel_card_stroke_imageoff));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setStateListAnimator(null);
        }
        setCheckedIconTint(FeatureSettings.isLocationPanelImageEnabled() ? ColorStateList.valueOf(Colors.WHITE) : ActivityUtils.getColorStateList(context, R.attr.colorPrimary));
        setRippleColorResource(FeatureSettings.isLocationPanelImageEnabled() ? R.color.location_panel_ripple_imageon : R.color.location_panel_ripple_imageoff);
        setCardForegroundColor(AppCompatResources.getColorStateList(context, FeatureSettings.isLocationPanelImageEnabled() ? R.color.location_panel_foreground_imageon : R.color.location_panel_foreground_imageoff));

        mGlide = Glide.with(this);
        showLoading(true);
    }

    public void setWeatherBackground(LocationPanelViewModel panelView) {
        setWeatherBackground(panelView, false);
    }

    public void setWeatherBackground(LocationPanelViewModel panelView, boolean skipCache) {
        if (panelView.getImageData() == null)
            panelView.updateBackground();

        String backgroundUri = panelView.getImageData() != null ?
                panelView.getImageData().getImageURI() : null;

        // Background
        if (FeatureSettings.isLocationPanelImageEnabled() && backgroundUri != null) {
            mGlide.asBitmap()
                    .load(backgroundUri)
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
                            binding.imageOverlay.setBackground(overlayDrawable);
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
        } else {
            clearBackground();
        }
    }

    public void clearBackground() {
        mGlide.clear(binding.imageView);
        binding.imageView.setImageDrawable(colorDrawable);
    }

    public void bindModel(LocationPanelViewModel model) {
        binding.setViewModel(model);
        binding.executePendingBindings();

        // Do this before checkable state is changed to disable check state
        this.setChecked(model.isChecked());
        this.setCheckable(model.isEditMode());
        // Do this after if checkable state is now enabled
        this.setChecked(model.isChecked());

        this.setTag(model.getLocationData());

        if (model.getLocationName() != null && getTag() != null)
            showLoading(false);
    }

    public void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? VISIBLE : GONE);
        setClickable(!show);
    }

    @Override
    public void setDragged(boolean dragged) {
        super.setDragged(dragged);
        setStroke();
    }

    @Override
    public void setChecked(boolean checked) {
        super.setChecked(checked);
        setStroke();
    }

    private void setStroke() {
        float strokeDp = 1f;
        if (FeatureSettings.isLocationPanelImageEnabled()) {
            if (isDragged()) {
                strokeDp = 3f;
            } else if (isChecked()) {
                strokeDp = 2f;
            }
        } else {
            strokeDp = isDragged() ? 2f : 1f;
        }
        setStrokeWidth((int) ActivityUtils.dpToPx(getContext(), strokeDp));
    }
}
