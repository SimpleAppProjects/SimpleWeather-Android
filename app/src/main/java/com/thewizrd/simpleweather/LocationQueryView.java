package com.thewizrd.simpleweather;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.thewizrd.simpleweather.weather.weatherunderground.data.AC_Location;

public class LocationQueryView extends LinearLayout {

    private View viewLayout;
    private TextView locationNameView;
    private TextView locationCountryView;
    private String locationName = "";
    private String locationCountry = "";
    private String locationQuery = "";

    public LocationQueryView(Context context) {
        super(context);
        init(context);
    }

    public LocationQueryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LocationQueryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public LocationQueryView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = LayoutInflater.from(context);
        viewLayout = inflater.inflate(R.layout.location_query_view, this);

        locationNameView = (TextView) viewLayout.findViewById(R.id.location_name);
        locationCountryView = (TextView) viewLayout.findViewById(R.id.location_country);
        locationNameView.setText(locationName);
        locationCountryView.setText(locationCountry);
    }

    public String getLocationName() { return locationName; }

    public String getLocationCountry() { return locationCountry; }

    public String getLocationQuery() { return locationQuery; }

    public void setLocationName(String name) {
        locationName = name;
        updateTextView();
    }

    public void setLocationCountry(String country) {
        locationCountry = country;
        updateTextView();
    }

    public void setLocationQuery(String query) {
        locationQuery = query;
    }

    public void setLocation(AC_Location location) {
        setLocationName(location.name);
        setLocationCountry(location.c);
        setLocationQuery(location.l);
        updateTextView();
    }

    private void updateTextView() {
        locationNameView.setText(locationName);
        locationCountryView.setText(locationCountry);
    }
}
