package com.thewizrd.simpleweather.preferences.iconpreference;

import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.thewizrd.shared_resources.icons.AVDIconsProviderInterface;
import com.thewizrd.shared_resources.icons.WeatherIconProvider;
import com.thewizrd.shared_resources.icons.WeatherIcons;
import com.thewizrd.shared_resources.icons.WeatherIconsManager;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.PreferenceIconViewBinding;
import com.thewizrd.simpleweather.preferences.radiopreference.RadioButtonPreference;

import java.util.Stack;

public class IconProviderPreference extends RadioButtonPreference {
    private final String[] PREVIEW_ICONS = {WeatherIcons.DAY_SUNNY, WeatherIcons.NIGHT_CLEAR, WeatherIcons.RAIN};

    private View mIconFrame;
    private int mIconVisibility = View.GONE;

    private final Stack<Animatable> animatedDrawables = new Stack<>();

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
    }

    @NonNull
    public final WeatherIconProvider getIconProvider() {
        return WeatherIconsManager.getProvider(getKey());
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        mIconFrame = view.findViewById(R.id.icon_frame);
        setIconFrameVisibility(mIconVisibility);

        ViewGroup iconsContainer = view.findViewById(R.id.icons_container);
        if (iconsContainer != null) {
            iconsContainer.removeAllViews();
            // Stop running animations
            while (!animatedDrawables.empty()) {
                Animatable drw = animatedDrawables.pop();
                drw.stop();
                drw = null;
            }

            for (String icon : PREVIEW_ICONS) {
                ImageView v = PreferenceIconViewBinding.inflate(LayoutInflater.from(view.getContext()), iconsContainer, true).getRoot();
                if (getIconProvider() instanceof AVDIconsProviderInterface) {
                    v.setImageDrawable(((AVDIconsProviderInterface) getIconProvider()).getAnimatedDrawable(v.getContext(), icon));
                } else {
                    v.setImageResource(getIconProvider().getWeatherIconResource(icon));
                }

                final Drawable drwbl = v.getDrawable();
                if (drwbl instanceof Animatable) {
                    ((Animatable) drwbl).start();
                    animatedDrawables.push((Animatable) drwbl);
                }
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
