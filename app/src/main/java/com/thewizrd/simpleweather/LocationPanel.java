package com.thewizrd.simpleweather;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.thewizrd.simpleweather.utils.Settings;
import com.thewizrd.simpleweather.utils.WeatherUtils;
import com.thewizrd.simpleweather.weather.weatherunderground.data.Weather;

public class LocationPanel extends CardView {
    private View viewLayout;
    private View mainLayout;
    private TextView locationNameView;
    private TextView locationTempView;
    private WeatherIcon locationWeatherIcon;
    private ProgressBar progressBar;

    public LocationPanel(Context context) {
        super(context);
        init(context);
    }

    public LocationPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LocationPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        viewLayout = inflater.inflate(R.layout.location_panel, this);
        mainLayout = viewLayout.findViewById(R.id.main_layout);

        locationNameView = (TextView) viewLayout.findViewById(R.id.location_name);
        locationTempView = (TextView) viewLayout.findViewById(R.id.weather_temp);
        locationWeatherIcon = (WeatherIcon) viewLayout.findViewById(R.id.weather_icon);
        progressBar = (ProgressBar) viewLayout.findViewById(R.id.progressBar);

        showLoading(true);
    }

    public void setWeather(Weather weather) {
        // Background
        try {
            mainLayout.setBackground(WeatherUtils.GetBackground(weather, mainLayout.getRight(), mainLayout.getBottom()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        locationNameView.setText(weather.location.full_name);
        locationTempView.setText(Settings.getTempUnit().equals("F") ?
                Math.round(weather.condition.temp_f) + "°" : Math.round(weather.condition.temp_c) + "°");
        locationWeatherIcon.setText(WeatherUtils.GetWeatherIcon(weather.condition.icon));

        showLoading(false);
    }

    public void showLoading(boolean show) {
        progressBar.setVisibility(show ? VISIBLE : GONE);
    }

    @Override
    public void setBackgroundColor(int color) {
        mainLayout.setBackgroundColor(color);
    }

    @Override
    public void setBackground(Drawable background) {
        mainLayout.setBackground(background);
    }
}
