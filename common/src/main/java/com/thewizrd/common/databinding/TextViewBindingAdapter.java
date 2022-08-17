package com.thewizrd.common.databinding;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;

import com.thewizrd.common.utils.TextViewUtilsKt;

public class TextViewBindingAdapter {
    @BindingAdapter("clickable")
    public static void checkClickable(@NonNull TextView view, CharSequence text) {
        view.setClickable(TextViewUtilsKt.isTextTruncated(view));
    }
}
