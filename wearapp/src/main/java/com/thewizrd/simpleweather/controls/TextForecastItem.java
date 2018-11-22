package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.thewizrd.shared_resources.controls.TextForecastItemViewModel;

public class TextForecastItem extends LinearLayout {
    private View viewLayout;
    private TextView forecastIcon;
    private TextView forecastPoP;
    private TextView forecastText;

    public TextForecastItem(Context context) {
        super(context);
        initialize(context);
    }

    public TextForecastItem(Context context, TextForecastItemViewModel forecastView) {
        super(context);
        initialize(context);
        setForecast(forecastView);
    }

    public TextForecastItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public TextForecastItem(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    private void initialize(Context context) {
        /*
        LayoutInflater inflater = LayoutInflater.from(context);
        viewLayout = inflater.inflate(R.layout.txt_forecast_panel, this);

        forecastIcon = viewLayout.findViewById(R.id.txt_forecasticon);
        forecastPoP = viewLayout.findViewById(R.id.txtforecast_pop);
        forecastText = viewLayout.findViewById(R.id.txt_fcttext);
        */
    }

    public void setForecast(TextForecastItemViewModel forecastView) {
        /*
        forecastIcon.setText(forecastView.getWeatherIcon());
        forecastPoP.setText(forecastView.getPop());
        forecastText.setText(forecastView.getFctText());
        */
    }
}
