package com.thewizrd.simpleweather;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.thewizrd.simpleweather.utils.Settings;
import com.thewizrd.simpleweather.utils.WeatherUtils;
import com.thewizrd.simpleweather.weather.weatherunderground.WeatherDataLoader;
import com.thewizrd.simpleweather.weather.weatherunderground.data.Forecastday1;
import com.thewizrd.simpleweather.weather.weatherunderground.data.Weather;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class WeatherNowFragment extends Fragment implements WeatherLoadedListener {
    private static final String ARG_QUERY = "query";
    private static final String ARG_INDEX = "index";

    private String mQuery;
    private int mIndex;

    private Context context;
    private WeatherDataLoader wLoader = null;

    // Views
    private View contentView;
    private ProgressBar progressBar;
    // Condition
    private TextView locationName;
    private TextView updateTime;
    private WeatherIcon weatherIcon;
    private TextView weatherCondition;
    private WeatherIcon weatherTemp;
    // Details
    private LinearLayout detailsPanel;
    private TextView humidity;
    private WeatherIcon pressureState;
    private TextView pressure;
    private TextView pressureUnit;
    private TextView visiblity;
    private TextView visiblityUnit;
    private TextView feelslike;
    private WeatherIcon windDirection;
    private TextView windSpeed;
    private TextView windUnit;
    private TextView sunrise;
    private TextView sunset;

    private final String fahrenheit = App.getAppContext().getString(R.string.wi_fahrenheit);
    private final String celsius = App.getAppContext().getString(R.string.wi_celsius);

    public void onWeatherLoaded(int index, Object weather) {
        if (weather != null) {
            updateView((Weather) weather);
        }
        else
            Toast.makeText(context, "Can't get weather", Toast.LENGTH_LONG).show();

        showLoadingView(false);
    }

    public WeatherNowFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param query Weather query.
     * @param index Location index.
     * @return A new instance of fragment WeatherNowFragment.
     */
    public static WeatherNowFragment newInstance(String query, int index) {
        WeatherNowFragment fragment = new WeatherNowFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUERY, query);
        args.putInt(ARG_INDEX, index);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mQuery = getArguments().getString(ARG_QUERY, null);
            mIndex = getArguments().getInt(ARG_INDEX, -1);
        }

        context = getActivity().getApplicationContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_weather_now, container, false);

        // Setup ActionBar
        setHasOptionsMenu(true);

        contentView = view.findViewById(R.id.content_view);
        progressBar = (ProgressBar)view.findViewById(R.id.progressBar);
        // Condition
        locationName = (TextView) view.findViewById(R.id.label_location_name);
        updateTime = (TextView) view.findViewById(R.id.label_updatetime);
        weatherIcon = (WeatherIcon) view.findViewById(R.id.weather_icon);
        weatherCondition = (TextView) view.findViewById(R.id.weather_condition);
        weatherTemp = (WeatherIcon) view.findViewById(R.id.weather_temp);
        // Details
        detailsPanel = (LinearLayout) view.findViewById(R.id.details_panel);
        humidity = (TextView) view.findViewById(R.id.humidity);
        pressureState = (WeatherIcon) view.findViewById(R.id.pressure_state);
        pressure = (TextView) view.findViewById(R.id.pressure);
        pressureUnit = (TextView) view.findViewById(R.id.pressure_unit);
        visiblity = (TextView) view.findViewById(R.id.visibility_val);
        visiblityUnit = (TextView) view.findViewById(R.id.visibility_unit);
        feelslike = (TextView) view.findViewById(R.id.feelslike);
        windDirection = (WeatherIcon) view.findViewById(R.id.wind_direction);
        windSpeed = (TextView) view.findViewById(R.id.wind_speed);
        windUnit = (TextView) view.findViewById(R.id.wind_unit);
        sunrise = (TextView) view.findViewById(R.id.sunrise_time);
        sunset = (TextView) view.findViewById(R.id.sunset_time);

        view.post(new Runnable() {
            @Override
            public void run() {
                Restore();
            }
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.clear();
        inflater.inflate(R.menu.weather_now, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            RefreshWeather(true);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update view on resume
        // ex. If temperature unit changed
        if (wLoader != null) {
            if (wLoader.getWeather() != null) {
                updateView(wLoader.getWeather());
            }
        }

        // Title
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setTitle("SimpleWeather");
    }

    private void Restore() {
        if (wLoader == null)
            wLoader = new WeatherDataLoader(context, this, mQuery, mIndex);

        RefreshWeather(false);
    }

    private void RefreshWeather(boolean forceRefresh) {
        // Hide view until weather is loaded
        showLoadingView(true);

        try {
            wLoader.loadWeatherData(forceRefresh);
        } catch (Exception e) {
            e.printStackTrace();
            showLoadingView(false);
        }
    }

    private void showLoadingView(boolean show) {
        contentView.setVisibility(show ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void updateView(Weather weather) {
        // Background
        try {
            getView().setBackground(WeatherUtils.GetBackground(weather, getView().getRight(), getView().getBottom()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        LinearLayout forecastPanel = (LinearLayout) contentView.findViewById(R.id.forecast_panel);
        forecastPanel.setBackgroundColor(WeatherUtils.isNight(weather) ? Color.parseColor("#20808080") : Color.parseColor("#10080808"));
        detailsPanel.setBackgroundColor(WeatherUtils.isNight(weather) ? Color.parseColor("#20808080") : Color.parseColor("#10080808"));

        // Location
        locationName.setText(weather.location.full_name);

        // Date Updated
        updateTime.setText(WeatherUtils.GetLastBuildDate(weather));

        // Update Current Condition
        weatherTemp.setText(Settings.getTempUnit().equals("F") ?
                Math.round(weather.condition.temp_f) + fahrenheit : Math.round(weather.condition.temp_c) + celsius);
        weatherCondition.setText(weather.condition.weather);
        weatherIcon.setText(WeatherUtils.GetWeatherIcon(weather.condition.icon_url));

        // WeatherDetails
        // Astronomy
        DateFormat sunphaseFormat = new SimpleDateFormat("hh:mm a");
        Calendar sunriseCal = Calendar.getInstance();
        Calendar sunsetCal = Calendar.getInstance();
        sunriseCal.set(0, 0, 0, Integer.valueOf(weather.sun_phase.sunrise.hour), Integer.valueOf(weather.sun_phase.sunrise.minute));
        sunsetCal.set(0, 0, 0, Integer.valueOf(weather.sun_phase.sunset.hour), Integer.valueOf(weather.sun_phase.sunset.minute));

        sunrise.setText(sunphaseFormat.format(sunriseCal.getTime()));
        sunset.setText(sunphaseFormat.format(sunsetCal.getTime()));

        // Wind
        feelslike.setText(Settings.getTempUnit().equals("F") ?
                Math.round(Float.valueOf(weather.condition.feelslike_f)) + "°": Math.round(Float.valueOf(weather.condition.feelslike_c)) + "°");
        windSpeed.setText(String.format("%.1f",
                Settings.getTempUnit().equals("F") ? weather.condition.wind_mph : weather.condition.wind_kph));
        windUnit.setText(Settings.getTempUnit().equals("F") ? "mph" : "kph");
        updateWindDirection(weather.condition.wind_degrees);

        // Atmosphere
        humidity.setText(weather.condition.relative_humidity);
        pressure.setText(Settings.getTempUnit().equals("F") ? weather.condition.pressure_in : weather.condition.pressure_mb);
        pressureUnit.setText(Settings.getTempUnit().equals("F") ? "in" : "mb");

        switch (weather.condition.pressure_trend) {
            case "+":
                updatePressureState(1);
                break;
            case "-":
                updatePressureState(2);
                break;
            default:
                updatePressureState(0);
                break;
        }

        visiblity.setText(Settings.getTempUnit().equals("F") ? weather.condition.visibility_mi : weather.condition.visibility_km);
        visiblityUnit.setText(Settings.getTempUnit().equals("F") ? "mi" : "km");

        // Add UI elements
        forecastPanel.removeAllViews();
        for (Forecastday1 forecast : weather.forecast.forecastday)
        {
            ForecastView view = new ForecastView(context);
            view.setForecast(forecast);
            forecastPanel.addView(view);
        }
    }

    private void updatePressureState(int rising)
    {
        switch (rising)
        {
            // Steady
            case 0:
            default:
                pressureState.setVisibility(View.GONE);
                pressureState.setText("");
                break;
            // Rising
            case 1:
                pressureState.setVisibility(View.VISIBLE);
                pressureState.setText("\uf058\uf058");
                break;
            // Falling
            case 2:
                pressureState.setVisibility(View.VISIBLE);
                pressureState.setText("\uf044\uf044");
                break;
        }
    }

    private void updateWindDirection(int angle)
    {
        windDirection.setRotation(angle);
    }
}
