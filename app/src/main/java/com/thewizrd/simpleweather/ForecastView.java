package com.thewizrd.simpleweather;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.thewizrd.simpleweather.utils.Settings;
import com.thewizrd.simpleweather.utils.WeatherUtils;
import com.thewizrd.simpleweather.weather.weatherunderground.data.Forecastday1;
import com.thewizrd.simpleweather.weather.yahoo.data.Forecast;

public class ForecastView extends LinearLayout {

    private View viewLayout;
    private TextView forecastDate;
    private WeatherIcon forecastIcon;
    private TextView forecastCondition;
    private TextView forecastTempHi;
    private TextView forecastTempLo;

    public ForecastView(Context context) {
        super(context);
        init(context);
    }

    public ForecastView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ForecastView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public ForecastView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        viewLayout = inflater.inflate(R.layout.weather_forecast_panel, this);

        forecastDate = (TextView) viewLayout.findViewById(R.id.forecast_date);
        forecastIcon = (WeatherIcon) viewLayout.findViewById(R.id.forecast_icon);
        forecastCondition = (TextView) viewLayout.findViewById(R.id.forecast_condition);
        forecastTempHi = (TextView) viewLayout.findViewById(R.id.forecast_temphi);
        forecastTempLo = (TextView) viewLayout.findViewById(R.id.forecast_templo);
    }

    public void setForecast(Forecast forecast) {
        forecastDate.setText(forecast.getDate());
        forecastIcon.setText(WeatherUtils.GetWeatherIcon(Integer.valueOf(forecast.code)));
        forecastCondition.setText(forecast.text);
        forecastTempHi.setText(forecast.high + "º");
        forecastTempLo.setText(forecast.low + "º");
    }

    public void setForecast(Forecastday1 forecast) {
        forecastDate.setText(String.format("%s %s", forecast.date.weekday, forecast.date.day));
        forecastIcon.setText(WeatherUtils.GetWeatherIcon(forecast.icon_url));
        forecastCondition.setText(forecast.conditions);
        forecastTempHi.setText(Settings.getTempUnit().equals("F") ?
                forecast.high.fahrenheit + "º" : forecast.high.celsius + "º");
        forecastTempLo.setText(Settings.getTempUnit().equals("F") ?
                forecast.low.fahrenheit + "º" : forecast.low.celsius + "º");
    }
}
