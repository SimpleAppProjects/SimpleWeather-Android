package com.thewizrd.shared_resources.databinding;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.databinding.BindingAdapter;

import com.thewizrd.shared_resources.controls.IconControl;
import com.thewizrd.shared_resources.icons.AVDIconsProviderInterface;
import com.thewizrd.shared_resources.icons.WeatherIconsManager;
import com.thewizrd.shared_resources.icons.WeatherIconsProviderInterface;

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

    @BindingAdapter(value = {"weatherIcon", "animate"}, requireAll = false)
    public static void setWeatherIcon(@NonNull final ImageView view, @DrawableRes final int resId, boolean animate) {
        view.setImageResource(resId);

        final Drawable drwbl = view.getDrawable();
        if (animate && drwbl instanceof Animatable) {
            Animatable animVDrwbl = ((Animatable) drwbl);
            if (!animVDrwbl.isRunning())
                animVDrwbl.start();
        }
    }

    private static void animateIconIfAvailable(@NonNull final ImageView view, Drawable drawable) {
        view.setImageDrawable(drawable);

        final Drawable drwbl = view.getDrawable();
        if (drwbl instanceof Animatable) {
            Animatable animVDrwbl = ((Animatable) drwbl);
            if (!animVDrwbl.isRunning())
                animVDrwbl.start();
        }
    }

    @BindingAdapter(value = {"weatherIcon", "iconProvider", "animate"}, requireAll = false)
    public static void setWeatherIcon(@NonNull final ImageView view, String icon, @Nullable String iconProvider, boolean animate) {
        if (view instanceof IconControl) {
            ((IconControl) view).setWeatherIcon(icon);
        } else {
            final WeatherIconsProviderInterface wip;
            if (iconProvider == null) {
                wip = WeatherIconsManager.getInstance().getProvider();
            } else {
                wip = WeatherIconsManager.getProvider(iconProvider);
            }

            if (animate && wip instanceof AVDIconsProviderInterface) {
                final AVDIconsProviderInterface avdProvider = (AVDIconsProviderInterface) wip;
                final Drawable drwbl = avdProvider.getAnimatedDrawable(view.getContext(), icon);
                animateIconIfAvailable(view, drwbl);
            } else {
                setWeatherIcon(view, icon != null ? wip.getWeatherIconResource(icon) : 0, animate);
            }
        }
    }
}
