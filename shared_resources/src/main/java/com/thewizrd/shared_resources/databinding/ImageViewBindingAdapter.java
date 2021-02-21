package com.thewizrd.shared_resources.databinding;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.databinding.BindingAdapter;

import com.thewizrd.shared_resources.icons.WeatherIconsManager;

public class ImageViewBindingAdapter {
    @BindingAdapter("srcCompat")
    public static void setImageViewDrawable(ImageView view, Drawable drawable) {
        if (view instanceof AppCompatImageView) {
            view.setImageDrawable(drawable);
        } else {
            view.setImageDrawable(DrawableCompat.wrap(drawable));
        }
    }

    @BindingAdapter("srcCompat")
    public static void setImageViewBitmap(ImageView view, Bitmap bmp) {
        view.setImageBitmap(bmp);
    }

    @BindingAdapter("srcCompat")
    public static void setImageViewResource(ImageView view, @DrawableRes int resId) {
        view.setImageResource(resId);
    }

    @BindingAdapter("tint")
    public static void setImageTintList(ImageView view, int color) {
        ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(color));
    }

    @BindingAdapter("weatherIcon")
    public static void animateIconIfAvailable(@NonNull final ImageView view, @DrawableRes final int resId) {
        view.setImageResource(resId);

        final Drawable drwbl = view.getDrawable();
        if (drwbl instanceof AnimatedVectorDrawable) {
            AnimatedVectorDrawable animVDrwbl = ((AnimatedVectorDrawable) drwbl);
            if (!animVDrwbl.isRunning())
                animVDrwbl.start();
        }
    }

    @BindingAdapter("weatherIcon")
    public static void animateIconIfAvailable(@NonNull final ImageView view, String icon) {
        animateIconIfAvailable(view, icon != null ? WeatherIconsManager.getInstance().getWeatherIconResource(icon) : 0);
    }
}
