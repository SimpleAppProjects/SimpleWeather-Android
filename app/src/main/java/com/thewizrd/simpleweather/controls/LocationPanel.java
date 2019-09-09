package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.card.MaterialCardView;
import com.thewizrd.shared_resources.helpers.ColorsUtils;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.helpers.ActivityUtils;

public class LocationPanel extends MaterialCardView {
    private View viewLayout;
    private ImageView bgImageView;
    private TextView locationNameView;
    private TextView locationTempView;
    private TextView locationWeatherIcon;
    private ProgressBar progressBar;
    private Drawable colorDrawable;
    private RequestManager mGlide;

    public LocationPanel(@NonNull Context context) {
        super(context);
        initialize(context);
    }

    public LocationPanel(@NonNull Context context, LocationPanelViewModel panelView) {
        super(context);
        initialize(context);
        setWeather(panelView);
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
        viewLayout = inflater.inflate(R.layout.location_panel, this);

        int height = context.getResources().getDimensionPixelSize(R.dimen.location_panel_height);
        viewLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));

        bgImageView = viewLayout.findViewById(R.id.image_view);
        locationNameView = viewLayout.findViewById(R.id.location_name);
        locationTempView = viewLayout.findViewById(R.id.weather_temp);
        locationWeatherIcon = viewLayout.findViewById(R.id.weather_icon);
        progressBar = viewLayout.findViewById(R.id.progressBar);
        colorDrawable = new ColorDrawable(Colors.SIMPLEBLUEMEDIUM);

        // NOTE: Bug: Explicitly set tintmode on Lollipop devices
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP)
            progressBar.setIndeterminateTintMode(PorterDuff.Mode.SRC_IN);

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
                .apply(new RequestOptions()
                        .centerCrop()
                        .format(DecodeFormat.PREFER_RGB_565)
                        .error(colorDrawable)
                        .placeholder(colorDrawable)
                        .skipMemoryCache(skipCache))
                .into(new BitmapImageViewTarget(bgImageView) {
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

                        setTextColor(textColor);
                        setTextShadowColor(shadowColor);
                    }
                });
    }

    public void clearBackground() {
        mGlide.clear(bgImageView);
        bgImageView.setImageDrawable(colorDrawable);
        setTextColor(Colors.WHITE);
    }

    public void setWeather(LocationPanelViewModel panelView) {
        locationNameView.setText(panelView.getLocationName());
        locationTempView.setText(panelView.getCurrTemp());
        locationWeatherIcon.setText(panelView.getWeatherIcon());
        setTag(panelView.getLocationData());

        if (panelView.getLocationName() != null && getTag() != null)
            showLoading(false);
    }

    public void showLoading(boolean show) {
        progressBar.setVisibility(show ? VISIBLE : GONE);
        setClickable(!show);
    }

    public void setTextColor(@ColorInt int color) {
        locationNameView.setTextColor(color);
        locationTempView.setTextColor(color);
        locationWeatherIcon.setTextColor(color);
    }

    public void setTextShadowColor(@ColorInt int color) {
        locationNameView.setShadowLayer(
                locationNameView.getShadowRadius(), locationNameView.getShadowDx(), locationNameView.getShadowDy(), color);
        locationTempView.setShadowLayer(
                locationTempView.getShadowRadius(), locationTempView.getShadowDx(), locationTempView.getShadowDy(), color);
        locationWeatherIcon.setShadowLayer(
                locationWeatherIcon.getShadowRadius(), locationWeatherIcon.getShadowDx(), locationWeatherIcon.getShadowDy(), color);
    }

    @Override
    public void setDragged(boolean dragged) {
        super.setDragged(dragged);
        setStrokeColor(ActivityUtils.getColor(getContext(), R.attr.colorOnSurface));
        setStrokeWidth(dragged ? (int) ActivityUtils.dpToPx(getContext(), 2) : 0);
    }
}
