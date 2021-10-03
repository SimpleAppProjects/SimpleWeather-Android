package com.thewizrd.simpleweather.preferences.radiopreference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.CheckBoxPreference;

import com.thewizrd.simpleweather.R;

/**
 * Based on AOSP RadioButtonPreference implementation
 */
public class RadioButtonPreference extends CheckBoxPreference {
    /**
     * Interface definition for a callback to be invoked when the preference is clicked.
     */
    public interface OnClickListener {
        /**
         * Called when a preference has been clicked.
         *
         * @param emiter The clicked preference
         */
        void onRadioButtonClicked(RadioButtonPreference emiter);
    }

    private OnClickListener mListener = null;

    public RadioButtonPreference(Context context) {
        this(context, null);
    }

    public RadioButtonPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RadioButtonPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public RadioButtonPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    /**
     * Sets the callback to be invoked when this preference is clicked by the user.
     *
     * @param listener The callback to be invoked
     */
    public void setOnClickListener(OnClickListener listener) {
        mListener = listener;
    }

    /**
     * Processes a click on the preference.
     */
    @Override
    public void onClick() {
        if (mListener != null) {
            mListener.onRadioButtonClicked(this);
        }
    }

    private void init() {
        setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
        setLayoutResource(R.layout.preference_radio);
    }
}
