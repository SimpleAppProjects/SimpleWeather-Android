package com.thewizrd.shared_resources.databinding;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.databinding.BindingAdapter;

import com.thewizrd.shared_resources.icons.WeatherIconsManager;
import com.thewizrd.shared_resources.utils.StringUtils;

import java.util.Collection;

public class ViewBindingAdapter {
    @BindingAdapter("hideIfNull")
    public static void hideIfNull(View view, Object object) {
        view.setVisibility(object == null ? View.GONE : View.VISIBLE);
    }

    @BindingAdapter("hideIfNullOrWhitespace")
    public static void hideIfNullOrWhitespace(View view, String s) {
        view.setVisibility(StringUtils.isNullOrWhitespace(s) ? View.GONE : View.VISIBLE);
    }

    @BindingAdapter("hideIfEmpty")
    public static <T extends Object> void hideIfEmpty(View view, Collection<T> c) {
        view.setVisibility(c == null || c.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @BindingAdapter("invisibleIfEmpty")
    public static <T extends Object> void invisibleIfEmpty(View view, Collection<T> c) {
        view.setVisibility(c == null || c.isEmpty() ? View.INVISIBLE : View.VISIBLE);
    }

    @BindingAdapter("showIfTrue")
    public static void showIfTrue(View view, boolean show) {
        view.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter(value = {"showIfTrue", "showIfNotEmpty"}, requireAll = true)
    public static <T extends Object> void showIfTrue(View view, boolean show, Collection<T> c) {
        view.setVisibility(show && c != null && !c.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter(value = {"showIfTrue", "hideIfNullOrWhitespace"}, requireAll = true)
    public static void hideIfNullOrWhitespace(View view, boolean show, String s) {
        view.setVisibility(show && !StringUtils.isNullOrWhitespace(s) ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter("drawableTint")
    public static void setDrawableTint(TextView view, int color) {
        TextViewCompat.setCompoundDrawableTintList(view, ColorStateList.valueOf(color));
    }

    @BindingAdapter(value = {"weatherIconStart", "weatherIconTop", "weatherIconEnd", "weatherIconBottom"}, requireAll = false)
    public static void setWeatherIconCompoundDrawablesRelative(TextView view, String start, String top, String end, String bottom) {
        final WeatherIconsManager wim = WeatherIconsManager.getInstance();

        final Drawable[] oldDrwbls = TextViewCompat.getCompoundDrawablesRelative(view);
        Drawable drawableStart = oldDrwbls[0];
        Drawable drawableTop = oldDrwbls[1];
        Drawable drawableEnd = oldDrwbls[2];
        Drawable drawableBottom = oldDrwbls[3];

        if (!StringUtils.isNullOrWhitespace(start)) {
            drawableStart = ContextCompat.getDrawable(view.getContext(), wim.getWeatherIconResource(start));
        }
        if (!StringUtils.isNullOrWhitespace(top)) {
            drawableTop = ContextCompat.getDrawable(view.getContext(), wim.getWeatherIconResource(top));
        }
        if (!StringUtils.isNullOrWhitespace(end)) {
            drawableBottom = ContextCompat.getDrawable(view.getContext(), wim.getWeatherIconResource(end));
        }
        if (!StringUtils.isNullOrWhitespace(bottom)) {
            drawableEnd = ContextCompat.getDrawable(view.getContext(), wim.getWeatherIconResource(bottom));
        }

        TextViewCompat.setCompoundDrawablesRelative(view, drawableStart, drawableTop, drawableEnd, drawableBottom);
    }
}
