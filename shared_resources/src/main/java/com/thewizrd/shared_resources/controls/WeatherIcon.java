package com.thewizrd.shared_resources.controls;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.res.ResourcesCompat;

import com.thewizrd.shared_resources.R;

public class WeatherIcon extends AppCompatTextView {
    public WeatherIcon(Context context) {
        super(context);
        initialize(context);
    }

    public WeatherIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public WeatherIcon(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        setTypeface(ResourcesCompat.getFont(context, R.font.weathericons));
    }
}
