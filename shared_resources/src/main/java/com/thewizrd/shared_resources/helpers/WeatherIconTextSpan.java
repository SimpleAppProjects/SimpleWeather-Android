package com.thewizrd.shared_resources.helpers;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.thewizrd.shared_resources.R;

public class WeatherIconTextSpan extends TypefaceSpan {
    private Typeface typeface;

    public WeatherIconTextSpan(Context context) {
        super("");

        typeface = ResourcesCompat.getFont(context, R.font.weathericons);
    }

    @Nullable
    @Override
    public Typeface getTypeface() {
        return typeface;
    }

    @Nullable
    @Override
    public String getFamily() {
        return null;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        ds.setTypeface(typeface);
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint paint) {
        paint.setTypeface(typeface);
    }
}
