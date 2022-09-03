package com.thewizrd.simpleweather.preferences.iconpreference;

import android.graphics.drawable.Drawable;

import androidx.annotation.LayoutRes;

import com.thewizrd.shared_resources.SharedModuleKt;
import com.thewizrd.shared_resources.icons.WeatherIconProvider;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.preferences.radiopreference.CandidateInfo;
import com.thewizrd.simpleweather.preferences.radiopreference.RadioButtonPickerFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class IconProviderPickerFragment extends RadioButtonPickerFragment {

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
        return R.xml.preference_icon_screen;
    }
}
