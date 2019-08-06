package com.thewizrd.simpleweather.controls;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.simpleweather.R;

import java.util.Locale;

public class ForecastItem extends MaterialCardView {
    private View viewLayout;
    private TextView forecastDate;
    private TextView forecastIcon;
    private TextView forecastCondition;
    private TextView forecastTempHi;
    private TextView forecastTempLo;
    private TextView forecastPoPIcon;
    private TextView forecastPoP;
    private TextView forecastWindDirection;
    private TextView forecastWindSpeed;

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
        viewLayout = inflater.inflate(R.layout.weather_forecast_panel, this);

        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setCardBackgroundColor(0x10FFFFFF);
        setCardElevation(0);
        setUseCompatPadding(false);

        forecastDate = viewLayout.findViewById(R.id.forecast_date);
        forecastIcon = viewLayout.findViewById(R.id.forecast_icon);
        forecastCondition = viewLayout.findViewById(R.id.forecast_condition);
        forecastTempHi = viewLayout.findViewById(R.id.forecast_temphi);
        forecastTempLo = viewLayout.findViewById(R.id.forecast_templo);
        forecastPoPIcon = viewLayout.findViewById(R.id.forecast_pop_icon);
        forecastPoP = viewLayout.findViewById(R.id.forecast_pop);
        forecastWindDirection = viewLayout.findViewById(R.id.forecast_wind_dir);
        forecastWindSpeed = viewLayout.findViewById(R.id.forecast_wind);
    }

    public void setForecast(ForecastItemViewModel forecastView) {
        forecastDate.setText(forecastView.getDate());
        forecastIcon.setText(forecastView.getWeatherIcon());
        forecastCondition.setText(forecastView.getCondition());
        forecastTempHi.setText(forecastView.getHiTemp());
        forecastTempLo.setText(forecastView.getLoTemp());

        if (StringUtils.isNullOrWhitespace(forecastView.getWindSpeed())
                || StringUtils.isNullOrWhitespace(forecastView.getPop().replace("%", ""))) {
            forecastPoPIcon.setVisibility(GONE);
            forecastPoP.setVisibility(GONE);
            forecastWindDirection.setVisibility(GONE);
            forecastWindSpeed.setVisibility(GONE);
        } else {
            forecastPoPIcon.setVisibility(VISIBLE);
            forecastPoP.setVisibility(VISIBLE);
            forecastWindDirection.setVisibility(VISIBLE);
            forecastWindSpeed.setVisibility(VISIBLE);
        }

        forecastPoPIcon.setText(R.string.wi_raindrop);
        forecastPoP.setText(forecastView.getPop());
        forecastWindDirection.setRotation(forecastView.getWindDirection());
        forecastWindSpeed.setText(String.format(Locale.ROOT, "%s, %s", forecastView.getWindSpeed(), forecastView.getWindDirLabel()));
    }
}
