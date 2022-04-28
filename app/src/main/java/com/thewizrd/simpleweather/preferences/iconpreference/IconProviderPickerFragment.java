package com.thewizrd.simpleweather.preferences.iconpreference;

import android.app.Activity;
import android.graphics.drawable.Drawable;

import androidx.annotation.LayoutRes;
import androidx.preference.PreferenceScreen;

import com.thewizrd.shared_resources.SharedModuleKt;
import com.thewizrd.shared_resources.icons.WeatherIconProvider;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.preferences.radiopreference.CandidateInfo;
import com.thewizrd.simpleweather.preferences.radiopreference.RadioButtonPickerFragment;
import com.thewizrd.simpleweather.preferences.radiopreference.RadioButtonPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class IconProviderPickerFragment extends RadioButtonPickerFragment {

    private static final String TAG = "IconProviderPckrFrgmt";

    @Override
    protected void onSelectionPerformed(boolean success) {
        super.onSelectionPerformed(success);

        final Activity activity = getActivity();
        if (activity != null) {
            activity.recreate();
        }
    }

    protected List<CandidateInfo> getCandidates() {
        final Map<String, WeatherIconProvider> providers = SharedModuleKt.getSharedDeps().getWeatherIconsManager().getIconProviders();
        final List<CandidateInfo> mCandidates = new ArrayList<>(providers.size());

        providers.forEach((s, wiProvider) -> {
            CandidateInfo info = new CandidateInfo(true) {
                @Override
                public CharSequence loadLabel() {
                    return wiProvider.getDisplayName();
                }

                @Override
                public Drawable loadIcon() {
                    return null;
                }

                @Override
                public String getKey() {
                    return s;
                }
            };

            mCandidates.add(info);
        });

        return mCandidates;
    }

    /**
     * Provides a custom layout for each candidate row.
     */
    @LayoutRes
    protected int getRadioButtonPreferenceCustomLayoutResId() {
        return R.layout.preference_icon;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.empty_preferences;
    }

    @Override
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
            RadioButtonPreference pref = new IconProviderPreference(getPrefContext());
            if (customLayoutResId > 0) {
                pref.setLayoutResource(customLayoutResId);
            }
            bindPreference(pref, info.getKey(), info, defaultKey);
            bindPreferenceExtra(pref, info.getKey(), info, defaultKey, systemDefaultKey);
            screen.addPreference(pref);
        }
        mayCheckOnlyRadioButton();
        updateCheckedState(defaultKey);
    }
}
