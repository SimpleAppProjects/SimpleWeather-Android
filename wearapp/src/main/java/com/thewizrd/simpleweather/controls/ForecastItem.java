package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.simpleweather.R;

public class ForecastItem extends LinearLayout {
    private TextView forecastDate;
    private TextView forecastIcon;
    private TextView forecastTempHi;
    private TextView forecastTempLo;

    public ForecastItem(Context context) {
        super(context);
        initialize(context);
    }

    public ForecastItem(Context context, ForecastItemViewModel forecastView) {
        super(context);
        initialize(context);
        setForecast(forecastView);
    }

    public ForecastItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public ForecastItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View viewLayout = inflater.inflate(R.layout.weather_forecast_panel, this);

        forecastDate = viewLayout.findViewById(R.id.forecast_date);
        forecastIcon = viewLayout.findViewById(R.id.forecast_icon);
        forecastTempHi = viewLayout.findViewById(R.id.forecast_temphi);
        forecastTempLo = viewLayout.findViewById(R.id.forecast_templo);
    }

    public void setForecast(ForecastItemViewModel forecastView) {
        forecastDate.setText(forecastView.getDate());
        forecastIcon.setText(forecastView.getWeatherIcon());
        forecastTempHi.setText(forecastView.getHiTemp());
        forecastTempLo.setText(forecastView.getLoTemp());
    }
}
