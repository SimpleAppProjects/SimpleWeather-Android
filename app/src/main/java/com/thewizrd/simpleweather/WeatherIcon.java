package com.thewizrd.simpleweather;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

public class WeatherIcon extends AppCompatTextView {

    public WeatherIcon(Context context) {
        super(context);
        init(context);
    }

    public WeatherIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WeatherIcon(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        AssetManager am = context.getApplicationContext().getAssets();

        Typeface typeface = null;

        try {
            typeface = Typeface.createFromAsset(am, "fonts/weathericons-regular-webfont.ttf");
        } catch (Exception e) {
            typeface = Typeface.DEFAULT;
        }

        setTypeface(typeface);
    }
}
