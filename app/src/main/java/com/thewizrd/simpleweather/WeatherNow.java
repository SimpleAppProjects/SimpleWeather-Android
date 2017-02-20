package com.thewizrd.simpleweather;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.thewizrd.simpleweather.utils.Settings;
import com.thewizrd.simpleweather.utils.WeatherUtils;
import com.thewizrd.simpleweather.weather.weatherunderground.WeatherDataLoader;
import com.thewizrd.simpleweather.weather.weatherunderground.data.Weather;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class WeatherNow extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, WeatherDataLoader.OnWeatherLoadedListener {

    private WeatherDataLoader wLoader = null;

    // Views
    private ScrollView contentView;
    // Condition
    private TextView locationName;
    private TextView updateTime;
    private WeatherIcon weatherIcon;
    private TextView weatherCondition;
    private WeatherIcon weatherTemp;
    // Details
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

    private final String farenheit = App.getAppContext().getString(R.string.wi_fahrenheit);
    private final String celsius = App.getAppContext().getString(R.string.wi_celsius);
    private String query;
    private int index;

    public void onWeatherLoaded(Weather weather) {
        if (weather != null) {
            updateView(weather);
        }
        else
            Toast.makeText(getApplicationContext(), "Can't get weather", Toast.LENGTH_LONG).show();

        findViewById(R.id.mainLayout).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_now);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Setup views
        contentView = (ScrollView) findViewById(R.id.content_weather_now);
        // Condition
        locationName = (TextView) findViewById(R.id.label_location_name);
        updateTime = (TextView) findViewById(R.id.label_updatetime);
        weatherIcon = (WeatherIcon) findViewById(R.id.weather_icon);
        weatherCondition = (TextView) findViewById(R.id.weather_condition);
        weatherTemp = (WeatherIcon) findViewById(R.id.weather_temp);
        humidity = (TextView) findViewById(R.id.humidity);
        pressureState = (WeatherIcon) findViewById(R.id.pressure_state);
        pressure = (TextView) findViewById(R.id.pressure);
        pressureUnit = (TextView) findViewById(R.id.pressure_unit);
        visiblity = (TextView) findViewById(R.id.visibility_val);
        visiblityUnit = (TextView) findViewById(R.id.visibility_unit);
        feelslike = (TextView) findViewById(R.id.feelslike);
        windDirection = (WeatherIcon) findViewById(R.id.wind_direction);
        windSpeed = (TextView) findViewById(R.id.wind_speed);
        windUnit = (TextView) findViewById(R.id.wind_unit);
        sunrise = (TextView) findViewById(R.id.sunrise_time);
        sunset = (TextView) findViewById(R.id.sunset_time);

        Restore();
    }

    private void Restore() {
        // Hide view until weather is loaded
        findViewById(R.id.mainLayout).setVisibility(View.INVISIBLE);

        Intent intent = getIntent();
        String query = intent.getStringExtra("query");
        if (TextUtils.isEmpty(query)) {
            query = Settings.getLocations_WU().get(0);
        }

        this.query = query;
        this.index = intent.getIntExtra("index", 0);

        wLoader = new WeatherDataLoader(this, this.query, this.index);

        RefreshWeather(false);
    }

    private void RefreshWeather(boolean forceRefresh) {
        if (!wLoader.getStatus().equals(AsyncTask.Status.PENDING)) {
            wLoader = new WeatherDataLoader(this, query, index);
        }

        wLoader.execute(forceRefresh);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.weather_now, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_refresh) {
            RefreshWeather(true);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void updateView(Weather weather) {
        // Background
        try {
            contentView.setBackground(WeatherUtils.GetBackground(weather, contentView.getRight(), contentView.getBottom()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Condition
        locationName.setText(weather.location.full_name);
        updateTime.setText(WeatherUtils.GetLastBuildDate(weather));
        weatherIcon.setText(WeatherUtils.GetWeatherIcon(weather.condition.icon));
        weatherCondition.setText(weather.condition.weather);
        weatherTemp.setText(Settings.getTempUnit().equals("F") ?
                Math.round(weather.condition.temp_f) + farenheit : Math.round(weather.condition.temp_c) + celsius);

        // Details
        humidity.setText(weather.condition.relative_humidity);
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
        pressure.setText(Settings.getTempUnit().equals("F") ? weather.condition.pressure_in : weather.condition.pressure_mb);
        pressureUnit.setText(Settings.getTempUnit().equals("F") ? "in" : "mb");
        visiblity.setText(Settings.getTempUnit().equals("F") ? weather.condition.visibility_mi : weather.condition.visibility_km);
        visiblityUnit.setText(Settings.getTempUnit().equals("F") ? "mi" : "km");
        feelslike.setText(Settings.getTempUnit().equals("F") ?
                Math.round(Float.valueOf(weather.condition.feelslike_f)) + "°": Math.round(Float.valueOf(weather.condition.feelslike_c)) + "°");
        updateWindDirection(weather.condition.wind_degrees);
        windSpeed.setText(String.format("%.1f",
                Settings.getTempUnit().equals("F") ? weather.condition.wind_mph : weather.condition.wind_kph));
        windUnit.setText(Settings.getTempUnit().equals("F") ? "mph" : "kph");

        // Sun Phase
        DateFormat sunphaseFormat = new SimpleDateFormat("hh:mm a");
        Calendar sunriseCal = Calendar.getInstance();
        Calendar sunsetCal = Calendar.getInstance();
        sunriseCal.set(0, 0, 0, Integer.valueOf(weather.sun_phase.sunrise.hour), Integer.valueOf(weather.sun_phase.sunrise.minute));
        sunsetCal.set(0, 0, 0, Integer.valueOf(weather.sun_phase.sunset.hour), Integer.valueOf(weather.sun_phase.sunset.minute));

        sunrise.setText(sunphaseFormat.format(sunriseCal.getTime()));
        sunset.setText(sunphaseFormat.format(sunsetCal.getTime()));

        // Forecast
        LinearLayout panel = (LinearLayout)findViewById(R.id.forecast_panel);
        panel.removeAllViews();
        for (int i = 0; i < weather.forecast.forecastday.length; i++)
        {
            ForecastView view = new ForecastView(this.getApplicationContext());
            view.setForecast(weather.forecast.forecastday[i]);
            panel.addView(view);
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
