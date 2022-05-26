package com.thewizrd.common.databinding;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.databinding.BindingAdapter;

import com.thewizrd.shared_resources.SharedModuleKt;
import com.thewizrd.shared_resources.icons.WeatherIconsManager;
import com.thewizrd.shared_resources.utils.StringUtils;

import java.util.Collection;
import java.util.Map;

public class ViewBindingAdapter {
    @BindingAdapter("hideIfNull")
    public static void hideIfNull(@NonNull View view, Object object) {
        view.setVisibility(object == null ? View.GONE : View.VISIBLE);
    }

    @BindingAdapter("hideIfNullOrWhitespace")
    public static void hideIfNullOrWhitespace(@NonNull View view, String s) {
        view.setVisibility(StringUtils.isNullOrWhitespace(s) ? View.GONE : View.VISIBLE);
    }

    @BindingAdapter("hideIfEmpty")
    public static <T> void hideIfEmpty(@NonNull View view, Collection<T> c) {
        view.setVisibility(c == null || c.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @BindingAdapter("invisibleIfEmpty")
    public static <T> void invisibleIfEmpty(@NonNull View view, Collection<T> c) {
        view.setVisibility(c == null || c.isEmpty() ? View.INVISIBLE : View.VISIBLE);
    }

    @BindingAdapter("showIfTrue")
    public static void showIfTrue(@NonNull View view, boolean show) {
        view.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter(value = {"showIfTrue", "showIfNotEmpty"}, requireAll = true)
    public static <T> void showIfTrue(@NonNull View view, boolean show, Collection<T> c) {
        view.setVisibility(show && c != null && !c.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter(value = {"showIfTrue", "showIfNotEmpty1", "showIfNotEmpty2"}, requireAll = true)
    public static void showIfTrueArgs(@NonNull View view, boolean show, Collection<?> c1, Collection<?> c2) {
        view.setVisibility(show && ((c1 != null && !c1.isEmpty()) || (c2 != null && !c2.isEmpty())) ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter(value = {"showIfTrue", "hideIfNullOrWhitespace"}, requireAll = true)
    public static void hideIfNullOrWhitespace(@NonNull View view, boolean show, String s) {
        view.setVisibility(show && !StringUtils.isNullOrWhitespace(s) ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter("drawableTint")
    public static void setDrawableTint(TextView view, int color) {
        TextViewCompat.setCompoundDrawableTintList(view, ColorStateList.valueOf(color));
    }

    @BindingAdapter(value = {"weatherIconStart", "weatherIconTop", "weatherIconEnd", "weatherIconBottom"}, requireAll = false)
    public static void setWeatherIconCompoundDrawablesRelative(TextView view, String start, String top, String end, String bottom) {
        final WeatherIconsManager wim = SharedModuleKt.getSharedDeps().getWeatherIconsManager();

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

    /* Maps */
    @BindingAdapter("hideIfEmpty")
    public static void hideIfEmpty(@NonNull View view, Map<?, ?> m) {
        view.setVisibility(m == null || m.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @BindingAdapter("invisibleIfEmpty")
    public static void invisibleIfEmpty(@NonNull View view, Map<?, ?> m) {
        view.setVisibility(m == null || m.isEmpty() ? View.INVISIBLE : View.VISIBLE);
    }

    @BindingAdapter(value = {"showIfTrue", "showIfNotEmpty"}, requireAll = true)
    public static void showIfTrue(@NonNull View view, boolean show, Map<?, ?> m) {
        view.setVisibility(show && m != null && !m.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter(value = {"showIfTrue", "showIfNotEmpty1", "showIfNotEmpty2"}, requireAll = true)
    public static void showIfTrueArgs(@NonNull View view, boolean show, Map<?, ?> m1, Map<?, ?> m2) {
        view.setVisibility(show && ((m1 != null && !m1.isEmpty()) || (m2 != null && !m2.isEmpty())) ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter(value = {"showIfNotEmpty", "showIfNotEmpty1", "showIfNotEmpty2", "showIfNotEmpty3"}, requireAll = false)
    public static void showIfNotEmpty(@NonNull View view, @Nullable Collection<?> c, @Nullable Collection<?> c1, @Nullable Collection<?> c2, @Nullable Collection<?> c3) {
        view.setVisibility((c != null && !c.isEmpty()) || (c1 != null && !c1.isEmpty()) || (c2 != null && !c2.isEmpty()) || (c3 != null && !c3.isEmpty()) ? View.VISIBLE : View.GONE);
    }
}
