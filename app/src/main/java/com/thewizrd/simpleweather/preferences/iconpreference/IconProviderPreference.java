package com.thewizrd.simpleweather.preferences.iconpreference;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceViewHolder;

import com.thewizrd.shared_resources.icons.WeatherIconProvider;
import com.thewizrd.shared_resources.icons.WeatherIcons;
import com.thewizrd.shared_resources.icons.WeatherIconsManager;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.PreferenceIconViewBinding;
import com.thewizrd.simpleweather.preferences.radiopreference.RadioButtonPreference;

public class IconProviderPreference extends RadioButtonPreference {
    private final String[] PREVIEW_ICONS = {WeatherIcons.DAY_SUNNY, WeatherIcons.NIGHT_CLEAR, WeatherIcons.DAY_SUNNY_OVERCAST, WeatherIcons.NIGHT_ALT_PARTLY_CLOUDY, WeatherIcons.RAIN};

    private View mIconFrame;
    private int mIconVisibility = View.GONE;

    public IconProviderPreference(Context context) {
        this(context, null);
    }

    public IconProviderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public IconProviderPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public IconProviderPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
        setLayoutResource(R.layout.preference_icon);
        setIconSpaceReserved(false);
    }

    @NonNull
    public final WeatherIconProvider getIconProvider() {
        return WeatherIconsManager.getProvider(getKey());
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        mIconFrame = holder.findViewById(R.id.icon_frame);
        setIconFrameVisibility(mIconVisibility);

        ViewGroup iconsContainer = (ViewGroup) holder.findViewById(R.id.icons_container);
        if (iconsContainer != null) {
            iconsContainer.removeAllViews();

            for (String icon : PREVIEW_ICONS) {
                ImageView v = PreferenceIconViewBinding.inflate(LayoutInflater.from(holder.itemView.getContext()), iconsContainer, true).getRoot();
                v.setImageResource(getIconProvider().getWeatherIconResource(icon));
            }
        }
    }

    public void setIconFrameVisibility(int visibility) {
        mIconVisibility = visibility;
        if (mIconFrame != null) {
            mIconFrame.setVisibility(visibility);
        }
    }
}
