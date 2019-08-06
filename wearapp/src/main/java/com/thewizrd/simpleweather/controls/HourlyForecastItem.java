package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.simpleweather.R;

public class HourlyForecastItem extends LinearLayout {
    private TextView forecastDate;
    private TextView forecastIcon;
    private TextView forecastTempHi;

    public HourlyForecastItem(Context context) {
        super(context);
        initialize(context);
    }

    public HourlyForecastItem(Context context, HourlyForecastItemViewModel forecastView) {
        super(context);
        initialize(context);
        setForecast(forecastView);
    }

    public HourlyForecastItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public HourlyForecastItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View viewLayout = inflater.inflate(R.layout.weather_hrforecast_panel, this);

        viewLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setOrientation(HORIZONTAL);

        forecastDate = viewLayout.findViewById(R.id.hrforecast_date);
        forecastIcon = viewLayout.findViewById(R.id.hrforecast_icon);
        forecastTempHi = viewLayout.findViewById(R.id.hrforecast_temphi);
    }

    public void setForecast(HourlyForecastItemViewModel forecastView) {
        forecastDate.setText(forecastView.getDate());
        forecastIcon.setText(forecastView.getWeatherIcon());
        forecastTempHi.setText(forecastView.getHiTemp());
    }
}
