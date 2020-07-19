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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

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
import com.thewizrd.simpleweather.databinding.ViewBindingAdapter;
import com.thewizrd.simpleweather.main.WeatherNowFragment;
import com.thewizrd.simpleweather.preferences.FeatureSettings;

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
        binding = DataBindingUtil.inflate(inflater, R.layout.location_panel, this, true,
                new LocationPanelDataBindingComponent());

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
        setStrokeColor(ActivityUtils.getColor(getContext(), R.attr.colorOnSurface));
        setStrokeWidth(dragged ? (int) ActivityUtils.dpToPx(getContext(), 2) : 0);
    }

    public class LocationPanelDataBindingComponent implements androidx.databinding.DataBindingComponent {
        private final ViewBindingAdapter viewBindingAdapter;

        public LocationPanelDataBindingComponent() {
            this.viewBindingAdapter = new ViewBindingAdapter();
        }

        @Override
        public ViewBindingAdapter getViewBindingAdapter() {
            return viewBindingAdapter;
        }

        @Override
        public WeatherNowFragment.WeatherNowFragmentBindingAdapter getWeatherNowFragmentBindingAdapter() {
            return null;
        }
    }
}
