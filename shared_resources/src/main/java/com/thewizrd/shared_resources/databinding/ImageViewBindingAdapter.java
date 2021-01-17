package com.thewizrd.shared_resources.databinding;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.databinding.BindingAdapter;

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
}
