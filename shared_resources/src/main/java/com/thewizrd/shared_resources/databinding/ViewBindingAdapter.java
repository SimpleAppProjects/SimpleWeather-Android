package com.thewizrd.shared_resources.databinding;

import android.view.View;

import androidx.databinding.BindingAdapter;

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
}
