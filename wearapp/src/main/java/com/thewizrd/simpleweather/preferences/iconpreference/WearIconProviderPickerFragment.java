package com.thewizrd.simpleweather.preferences.iconpreference;

import android.content.Context;

import androidx.annotation.NonNull;

import com.thewizrd.simpleweather.preferences.radiopreference.RadioButtonPreference;

public abstract class WearIconProviderPickerFragment extends IconProviderPickerFragment {
    @NonNull
    @Override
    protected RadioButtonPreference createRadioButtonPreference(@NonNull Context context) {
        return new WearIconProviderPreference(context);
    }

    @Override
    protected int getRadioButtonPreferenceCustomLayoutResId() {
        return 0;
    }
}
