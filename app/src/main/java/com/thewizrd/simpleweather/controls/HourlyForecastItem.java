package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.simpleweather.R;

public class HourlyForecastItem extends ConstraintLayout {
    private TextView forecastDate;
    private TextView forecastIcon;
    private TextView forecastCondition;
    private TextView forecastTempHi;
    private TextView forecastPoPIcon;
    private TextView forecastPoP;
    private TextView forecastWindDirection;
    private TextView forecastWindSpeed;

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

        forecastDate = viewLayout.findViewById(R.id.hrforecast_date);
        forecastIcon = viewLayout.findViewById(R.id.hrforecast_icon);
        forecastCondition = viewLayout.findViewById(R.id.hrforecast_condition);
        forecastTempHi = viewLayout.findViewById(R.id.hrforecast_temphi);
        forecastPoPIcon = viewLayout.findViewById(R.id.hrforecast_pop_icon);
        forecastPoP = viewLayout.findViewById(R.id.hrforecast_pop);
        forecastWindDirection = viewLayout.findViewById(R.id.hrforecast_wind_dir);
        forecastWindSpeed = viewLayout.findViewById(R.id.hrforecast_wind);
    }

    public void setForecast(HourlyForecastItemViewModel forecastView) {
        forecastDate.setText(forecastView.getDate());
        forecastIcon.setText(forecastView.getWeatherIcon());
        forecastCondition.setText(forecastView.getCondition());
        forecastTempHi.setText(forecastView.getHiTemp());

        if (Settings.getAPI().equals(WeatherAPI.OPENWEATHERMAP) ||
                Settings.getAPI().equals(WeatherAPI.METNO))
            forecastPoPIcon.setText(R.string.wi_cloudy);
        else
            forecastPoPIcon.setText(R.string.wi_raindrop);

        forecastPoP.setText(forecastView.getPop());
        forecastWindDirection.setRotation(forecastView.getWindDirection());
        forecastWindSpeed.setText(forecastView.getWindSpeed());
    }
}
