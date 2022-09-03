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
        this(context, attrs, 0);
    }

    public RadioButtonPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.Preference_RadioPreference_Material);
    }

    public RadioButtonPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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
}
