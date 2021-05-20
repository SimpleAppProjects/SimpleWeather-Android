package com.thewizrd.simpleweather.databinding;

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
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.graphics.drawable.WrappedDrawable;
import androidx.databinding.BindingAdapter;

import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.controls.graphs.ForecastGraphPanel;
import com.thewizrd.simpleweather.controls.graphs.RangeBarGraphPanel;
import com.thewizrd.simpleweather.controls.viewmodels.ForecastGraphViewModel;
import com.thewizrd.simpleweather.controls.viewmodels.RangeBarGraphViewModel;

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

    @BindingAdapter("forecast_data")
    public static void updateForecastGraph(final ForecastGraphPanel view, final ForecastGraphViewModel graphData) {
        view.updateForecasts(graphData);
    }

    @BindingAdapter("forecast_data")
    public static void updateForecastGraph(final RangeBarGraphPanel view, final RangeBarGraphViewModel graphData) {
        view.updateForecasts(graphData);
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
}
