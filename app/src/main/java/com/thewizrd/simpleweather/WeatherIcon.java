package com.thewizrd.simpleweather;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class WeatherIcon extends TextView {

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

        Typeface typeface = Typeface.createFromAsset(am, "fonts/weathericons-regular-webfont.ttf");
        setTypeface(typeface);
    }
}
