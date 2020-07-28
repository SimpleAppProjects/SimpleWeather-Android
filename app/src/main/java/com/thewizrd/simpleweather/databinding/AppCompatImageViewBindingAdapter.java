package com.thewizrd.simpleweather.databinding;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.databinding.BindingAdapter;

public class AppCompatImageViewBindingAdapter {
    @BindingAdapter("srcCompat")
    public void setImageViewDrawable(AppCompatImageView view, Drawable drawable) {
        view.setImageDrawable(drawable);
    }

    @BindingAdapter("srcCompat")
    public void setImageViewBitmap(AppCompatImageView view, Bitmap bmp) {
        view.setImageBitmap(bmp);
    }
}
