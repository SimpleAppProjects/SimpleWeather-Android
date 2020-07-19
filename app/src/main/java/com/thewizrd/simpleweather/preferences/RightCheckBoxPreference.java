package com.thewizrd.simpleweather.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceViewHolder;

public class RightCheckBoxPreference extends CheckBoxPreference {

    public RightCheckBoxPreference(Context context) {
        super(context);
    }

    public RightCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RightCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RightCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        ViewGroup parent = (ViewGroup) holder.itemView;
        View widgetFrame = parent.findViewById(android.R.id.widget_frame);
        if (widgetFrame != null) {
            parent.removeView(widgetFrame);
            parent.addView(widgetFrame, 1);
        }
    }
}
