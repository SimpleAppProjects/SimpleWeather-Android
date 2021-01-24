package com.thewizrd.simpleweather.preferences.radiopreference;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.LayoutRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.thewizrd.simpleweather.preferences.ToolbarPreferenceFragmentCompat;

import java.util.List;

public abstract class RadioButtonPickerFragment extends ToolbarPreferenceFragmentCompat
        implements RadioButtonPreference.OnClickListener {

    private static final String TAG = "RadioButtonPckrFrgmt";

    protected final Context getPrefContext() {
        return getPreferenceManager().getContext();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        final int resId = getPreferenceScreenResId();
        if (resId > 0) {
            addPreferencesFromResource(resId);
        }
        updateCandidates();
    }

    protected abstract int getPreferenceScreenResId();

    @Override
    public void onRadioButtonClicked(RadioButtonPreference selected) {
        final String selectedKey = selected.getKey();
        onRadioButtonConfirmed(selectedKey);
    }

    /**
     * Called after the user tries to select an item.
     */
    protected void onSelectionPerformed(boolean success) {
    }

    protected void onRadioButtonConfirmed(String selectedKey) {
        final boolean success = setDefaultKey(selectedKey);
        if (success) {
            updateCheckedState(selectedKey);
        }
        onSelectionPerformed(success);
    }

    /**
     * A chance for subclasses to bind additional things to the preference.
     */
    public void bindPreferenceExtra(RadioButtonPreference pref,
                                    String key, CandidateInfo info, String defaultKey, String systemDefaultKey) {
    }

    public void updateCandidates() {
        final String defaultKey = getDefaultKey();
        final String systemDefaultKey = getSystemDefaultKey();
        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();

        final List<? extends CandidateInfo> candidateList = getCandidates();
        if (candidateList == null) {
            return;
        }

        final int customLayoutResId = getRadioButtonPreferenceCustomLayoutResId();
        for (CandidateInfo info : candidateList) {
            RadioButtonPreference pref = new RadioButtonPreference(getPrefContext());
            if (customLayoutResId > 0) {
                pref.setLayoutResource(customLayoutResId);
            }
            bindPreference(pref, info.getKey(), info, defaultKey);
            bindPreferenceExtra(pref, info.getKey(), info, defaultKey, systemDefaultKey);
            screen.addPreference(pref);
        }
        mayCheckOnlyRadioButton();
    }

    public RadioButtonPreference bindPreference(RadioButtonPreference pref,
                                                String key, CandidateInfo info, String defaultKey) {
        pref.setTitle(info.loadLabel());
        pref.setIcon(info.loadIcon());
        pref.setKey(key);
        if (TextUtils.equals(defaultKey, key)) {
            pref.setChecked(true);
        }
        pref.setEnabled(info.enabled);
        pref.setOnClickListener(this);
        return pref;
    }

    public void updateCheckedState(String selectedKey) {
        final PreferenceScreen screen = getPreferenceScreen();
        if (screen != null) {
            final int count = screen.getPreferenceCount();
            for (int i = 0; i < count; i++) {
                final Preference pref = screen.getPreference(i);
                if (pref instanceof RadioButtonPreference) {
                    final RadioButtonPreference radioPref = (RadioButtonPreference) pref;
                    final boolean newCheckedState = TextUtils.equals(pref.getKey(), selectedKey);
                    if (radioPref.isChecked() != newCheckedState) {
                        radioPref.setChecked(TextUtils.equals(pref.getKey(), selectedKey));
                    }
                }
            }
        }
    }

    public void mayCheckOnlyRadioButton() {
        final PreferenceScreen screen = getPreferenceScreen();
        // If there is only 1 thing on screen, select it.
        if (screen != null && screen.getPreferenceCount() == 1) {
            final Preference onlyPref = screen.getPreference(0);
            if (onlyPref instanceof RadioButtonPreference) {
                ((RadioButtonPreference) onlyPref).setChecked(true);
            }
        }
    }

    protected abstract List<? extends CandidateInfo> getCandidates();

    protected abstract String getDefaultKey();

    protected abstract boolean setDefaultKey(String key);

    protected String getSystemDefaultKey() {
        return null;
    }

    /**
     * Provides a custom layout for each candidate row.
     */
    @LayoutRes
    protected int getRadioButtonPreferenceCustomLayoutResId() {
        return 0;
    }
}
