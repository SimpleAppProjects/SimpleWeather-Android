package com.thewizrd.simpleweather;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.support.wearable.input.RotaryEncoder;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;

public class WeatherDetailsFragment extends SwipeDismissFragment {
    private WeatherNowViewModel weatherView = null;

    // Details
    private NestedScrollView scrollView;
    private View detailsPanel;
    private TextView humidity;
    private TextView pressureState;
    private TextView pressure;
    private TextView visiblity;
    private TextView feelslike;
    private TextView windDirection;
    private TextView windSpeed;
    private TextView sunrise;
    private TextView sunset;
    private RelativeLayout precipitationPanel;
    private TextView chanceLabel;
    private TextView chance;
    private TextView qpfRain;
    private TextView qpfSnow;
    private TextView cloudinessLabel;
    private TextView cloudiness;
    private TextView weatherCredit;

    public static WeatherDetailsFragment newInstance(WeatherNowViewModel weatherViewModel) {
        WeatherDetailsFragment fragment = new WeatherDetailsFragment();
        if (weatherViewModel != null) {
            fragment.weatherView = weatherViewModel;
        }
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        // Use this to return your custom view for this Fragment
        View outerView = super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_weather_details, (ViewGroup) outerView, true);
        outerView.setFocusableInTouchMode(true);
        outerView.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_SCROLL && RotaryEncoder.isFromRotaryEncoder(event)) {
                    // Don't forget the negation here
                    float delta = -RotaryEncoder.getRotaryAxisValue(event) * RotaryEncoder.getScaledScrollFactor(getActivity());

                    // Swap these axes if you want to do horizontal scrolling instead
                    scrollView.scrollBy(0, Math.round(delta));

                    return true;
                }

                return false;
            }
        });

        scrollView = view.findViewById(R.id.scrollView);
        // Details
        detailsPanel = view.findViewById(R.id.details_panel);
        humidity = view.findViewById(R.id.humidity);
        pressureState = view.findViewById(R.id.pressure_state);
        pressure = view.findViewById(R.id.pressure);
        visiblity = view.findViewById(R.id.visibility_val);
        feelslike = view.findViewById(R.id.feelslike);
        windDirection = view.findViewById(R.id.wind_direction);
        windSpeed = view.findViewById(R.id.wind_speed);
        sunrise = view.findViewById(R.id.sunrise_time);
        sunset = view.findViewById(R.id.sunset_time);

        // Additional Details
        precipitationPanel = view.findViewById(R.id.precipitation_card);
        precipitationPanel.setVisibility(View.GONE);
        chanceLabel = view.findViewById(R.id.chance_label);
        chance = view.findViewById(R.id.chance_val);
        cloudinessLabel = view.findViewById(R.id.cloudiness_label);
        cloudiness = view.findViewById(R.id.cloudiness);
        qpfRain = view.findViewById(R.id.qpf_rain_val);
        qpfSnow = view.findViewById(R.id.qpf_snow_val);

        // Cloudiness only supported by OWM
        cloudinessLabel.setVisibility(View.GONE);
        cloudiness.setVisibility(View.GONE);

        weatherCredit = view.findViewById(R.id.weather_credit);

        return outerView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Don't resume if fragment is hidden
        if (this.isHidden())
            return;
        else
            initialize();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        if (!hidden && this.isVisible()) {
            initialize();
        }
    }

    private void initialize() {
        if (weatherView != null) {
            if (getView() != null) {
                getView().setBackgroundColor(weatherView.getPendingBackground());
                getView().requestFocus();
            }

            // WeatherDetails
            // Astronomy
            sunrise.setText(weatherView.getSunrise());
            sunset.setText(weatherView.getSunset());

            // Wind
            feelslike.setText(weatherView.getWindChill());
            windSpeed.setText(weatherView.getWindSpeed());
            windDirection.setRotation(weatherView.getWindDirection());

            // Atmosphere
            humidity.setText(weatherView.getHumidity());
            pressure.setText(weatherView.getPressure());

            pressureState.setVisibility(weatherView.getRisingVisiblity());
            pressureState.setText(weatherView.getRisingIcon());

            visiblity.setText(weatherView.getVisibility());

            if (!StringUtils.isNullOrWhitespace(weatherView.getExtras().getChance())) {
                cloudiness.setText(weatherView.getExtras().getChance());
                chance.setText(weatherView.getExtras().getChance());
                qpfRain.setText(weatherView.getExtras().getQpfRain());
                qpfSnow.setText(weatherView.getExtras().getQpfSnow());

                if (!Settings.getAPI().equals(WeatherAPI.METNO)) {
                    precipitationPanel.setVisibility(View.VISIBLE);
                } else {
                    precipitationPanel.setVisibility(View.GONE);
                }

                if (Settings.getAPI().equals(WeatherAPI.OPENWEATHERMAP) || Settings.getAPI().equals(WeatherAPI.METNO)) {
                    chanceLabel.setVisibility(View.GONE);
                    chance.setVisibility(View.GONE);

                    cloudinessLabel.setVisibility(View.VISIBLE);
                    cloudiness.setVisibility(View.VISIBLE);
                } else {
                    chanceLabel.setVisibility(View.VISIBLE);
                    chance.setVisibility(View.VISIBLE);

                    cloudinessLabel.setVisibility(View.GONE);
                    cloudiness.setVisibility(View.GONE);
                }
            } else {
                precipitationPanel.setVisibility(View.GONE);

                cloudinessLabel.setVisibility(View.GONE);
                cloudiness.setVisibility(View.GONE);
            }

            weatherCredit.setText(weatherView.getWeatherCredit());
        }
    }
}
