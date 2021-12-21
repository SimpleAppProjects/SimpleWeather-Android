package com.thewizrd.simpleweather.databinding;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.graphics.drawable.WrappedDrawable;
import androidx.databinding.BindingAdapter;

import com.google.android.material.progressindicator.BaseProgressIndicator;
import com.thewizrd.shared_resources.icons.WeatherIcons;
import com.thewizrd.shared_resources.utils.AirQualityUtils;
import com.thewizrd.shared_resources.utils.LocaleUtils;
import com.thewizrd.simpleweather.R;

import java.util.Locale;

public class ViewBindingAdapter {
    @BindingAdapter("progressColor")
    public static void updateProgressColor(ProgressBar progressBar, @ColorInt int progressColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            progressBar.setProgressTintList(ColorStateList.valueOf(progressColor));
        } else {
            Drawable drawable = progressBar.getProgressDrawable().mutate();
            drawable.setColorFilter(progressColor, PorterDuff.Mode.SRC_IN);
            progressBar.setProgressDrawable(drawable);
        }
    }

    @BindingAdapter("progressColor")
    public static void updateProgressColor(@NonNull BaseProgressIndicator<?> progressBar, @ColorInt int progressColor) {
        progressBar.setIndicatorColor(progressColor);
    }

    @BindingAdapter("progressBackgroundColor")
    public static void updateProgressBackgroundColor(ProgressBar progressBar, @ColorInt int progressBackgroundColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(progressBackgroundColor));
        } else {
            LayerDrawable drawable = null;
            if (progressBar.getProgressDrawable() instanceof LayerDrawable) {
                drawable = (LayerDrawable) progressBar.getProgressDrawable();
            } else if (progressBar.getProgressDrawable() instanceof WrappedDrawable) {
                Drawable unwrapped = DrawableCompat.unwrap(progressBar.getProgressDrawable());
                if (unwrapped instanceof LayerDrawable) {
                    drawable = (LayerDrawable) unwrapped;
                }
            }
            if (drawable != null) {
                GradientDrawable background = (GradientDrawable) drawable.findDrawableByLayerId(android.R.id.background);
                background.setColorFilter(progressBackgroundColor, PorterDuff.Mode.SRC_IN);
            }
        }
    }

    @BindingAdapter("attribution")
    public static void updateAttribution(final TextView view, final CharSequence attrib) {
        if (!TextUtils.isEmpty(attrib)) {
            view.setText(String.format(Locale.ROOT, "%s %s", view.getContext().getString(R.string.credit_prefix), attrib));
            view.setVisibility(View.VISIBLE);
        } else {
            view.setText("");
            view.setVisibility(View.GONE);
        }
    }

    @BindingAdapter(value = {"aqiIndex", "fallbackTextColor"}, requireAll = true)
    public static void setAQIIndexColor(final TextView view, final Integer index, @ColorInt int fallbackColor) {
        if (index != null) {
            view.setText(String.format(LocaleUtils.getLocale(), "%s", index));
            view.setTextColor(AirQualityUtils.getColorFromIndex(index));
        } else {
            view.setText(WeatherIcons.EM_DASH);
            view.setTextColor(fallbackColor);
        }
    }

    @BindingAdapter("aqiIndexLevel")
    public static void setAQIIndexLevel(final TextView view, final Integer index) {
        final Context context = view.getContext();
        String level;

        if (index == null) {
            level = WeatherIcons.EM_DASH;
        } else if (index < 51) {
            level = context.getString(com.thewizrd.shared_resources.R.string.aqi_level_0_50);
        } else if (index < 101) {
            level = context.getString(com.thewizrd.shared_resources.R.string.aqi_level_51_100);
        } else if (index < 151) {
            level = context.getString(com.thewizrd.shared_resources.R.string.aqi_level_101_150);
        } else if (index < 201) {
            level = context.getString(com.thewizrd.shared_resources.R.string.aqi_level_151_200);
        } else if (index < 301) {
            level = context.getString(com.thewizrd.shared_resources.R.string.aqi_level_201_300);
        } else {
            level = context.getString(com.thewizrd.shared_resources.R.string.aqi_level_300);
        }

        view.setText(level);
    }

    @BindingAdapter("aqiIndexDescription")
    public static void setAQIIndexDescription(final TextView view, final Integer index) {
        final Context context = view.getContext();
        String level;

        if (index == null) {
            level = WeatherIcons.EM_DASH;
        } else if (index < 51) {
            level = context.getString(com.thewizrd.shared_resources.R.string.aqi_level_0_50);
        } else if (index < 101) {
            level = context.getString(com.thewizrd.shared_resources.R.string.aqi_level_51_100);
        } else if (index < 151) {
            level = context.getString(com.thewizrd.shared_resources.R.string.aqi_level_101_150);
        } else if (index < 201) {
            level = context.getString(com.thewizrd.shared_resources.R.string.aqi_level_151_200);
        } else if (index < 301) {
            level = context.getString(com.thewizrd.shared_resources.R.string.aqi_level_201_300);
        } else {
            level = context.getString(com.thewizrd.shared_resources.R.string.aqi_level_300);
        }

        view.setText(level);
    }
}
