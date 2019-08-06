package com.thewizrd.simpleweather.controls;

import android.content.Context;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.card.MaterialCardView;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.simpleweather.R;

public class LocationPanel extends MaterialCardView {
    private View viewLayout;
    private ImageView bgImageView;
    private TextView locationNameView;
    private TextView locationTempView;
    private TextView locationWeatherIcon;
    private ProgressBar progressBar;
    private Drawable colorDrawable;

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
        colorDrawable = new ColorDrawable(Colors.SIMPLEBLUE);

        // NOTE: Bug: Explicitly set tintmode on Lollipop devices
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP)
            progressBar.setIndeterminateTintMode(PorterDuff.Mode.SRC_IN);

        // Remove extra (compat) padding on pre-Lollipop devices
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            setPreventCornerOverlap(false);
            setCardElevation(0f);
            setMaxCardElevation(0f);
        }

        showLoading(true);
    }

    public void setWeatherBackground(LocationPanelViewModel panelView) {
        // Background
        Glide.with(getContext())
                .load(panelView.getBackground())
                .apply(new RequestOptions()
                        .centerCrop()
                        .error(colorDrawable)
                        .placeholder(colorDrawable))
                .into(bgImageView);
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
}
