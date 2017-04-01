package com.thewizrd.simpleweather;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.thewizrd.simpleweather.utils.JSONParser;
import com.thewizrd.simpleweather.utils.Settings;
import com.thewizrd.simpleweather.utils.WeatherUtils;
import com.thewizrd.simpleweather.weather.weatherunderground.AutoCompleteQuery;
import com.thewizrd.simpleweather.weather.weatherunderground.GeopositionQuery;
import com.thewizrd.simpleweather.weather.weatherunderground.WUDataLoaderTask;
import com.thewizrd.simpleweather.weather.weatherunderground.data.AC_Location;
import com.thewizrd.simpleweather.weather.weatherunderground.data.WUWeather;
import com.thewizrd.simpleweather.weather.yahoo.YahooWeatherLoaderTask;
import com.thewizrd.simpleweather.weather.yahoo.data.YahooWeather;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class LocationSearchFragment extends Fragment {

    private Location mLocation;
    private String mQueryString;
    private RecyclerView mRecyclerView;
    private LocationQueryAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;

    private String ARG_QUERY = "query";
    private String ARG_INDEX = "index";

    public LocationSearchFragment() {
        // Required empty public constructor
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            LocationQueryView v = (LocationQueryView)view;
            int homeIdx = 0;

            if (TextUtils.isEmpty(Settings.getAPIKEY()) && Settings.getAPI().equals("WUnderground")) {
                String errorMsg = "Invalid API Key";
                Toast.makeText(getActivity().getApplicationContext(), errorMsg, Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(getActivity(), MainActivity.class);

            if (Settings.getAPI().equals("WUnderground")) {
                WUWeather weather = null;
                String query = v.getLocationQuery();

                try {
                    weather = new WUDataLoaderTask(getActivity()).execute(query).get();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (weather == null) {
                    return;
                }

                intent.putExtra(ARG_QUERY, query);
                intent.putExtra(ARG_INDEX, homeIdx);

                List<String> locations = new ArrayList<>();
                locations.add(query);
                // Save data
                try {
                    JSONParser.serializer(weather, new File(getContext().getFilesDir(), "weather0.json"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Settings.saveLocations_WU(locations);
                Settings.setWeatherLoaded(true);
            } else {
                List<WeatherUtils.Coordinate> locations = new ArrayList<>();
                YahooWeather weather = null;
                try {
                    weather = new YahooWeatherLoaderTask(getActivity()).execute(v.getLocationName()).get();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (weather == null) {
                    return;
                }

                WeatherUtils.Coordinate local = new WeatherUtils.Coordinate(
                        String.format("%s, %s", weather.location.lat, weather.location._long));
                intent.putExtra(ARG_QUERY, local.getCoordinatePair());
                intent.putExtra(ARG_INDEX, homeIdx);

                locations.add(local);
                // Save data
                try {
                    JSONParser.serializer(weather, new File(getContext().getFilesDir(), "weather0.json"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Settings.saveLocations(locations);
                Settings.setWeatherLoaded(true);
            }

            // Navigate
            getActivity().startActivity(intent);
            getActivity().finishAffinity();
        }
    };

    public void setOnClickListener(View.OnClickListener listener) {
        clickListener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_location_search, container, false);
        setupView(view);
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void setupView(View view) {
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(this.getActivity(), clickListener));

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new LocationQueryAdapter(new ArrayList<AC_Location>());
        mRecyclerView.setAdapter(mAdapter);
    }

    public void fetchLocations(String queryString) {
        if (!TextUtils.equals(mQueryString, queryString)) {
            mQueryString = queryString;
            // Get locations
            List<AC_Location> locations = new ArrayList<>();

            if (!TextUtils.isEmpty(mQueryString)) {
                try {
                    List<AC_Location> results = new AutoCompleteQuery(getActivity()).execute(mQueryString).get();

                    if (results.size() > 0)
                        locations.addAll(results);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            mAdapter.setLocations(locations);
        }
    }

    public void fetchGeoLocation() {
        if (mLocation != null) {
            // Get geo location
            AC_Location gpsLocation = null;
            ArrayList<AC_Location> results = new ArrayList<>(1);

            try {
                WeatherUtils.Coordinate coordinate = new WeatherUtils.Coordinate(mLocation.getLatitude(), mLocation.getLongitude());
                gpsLocation = new GeopositionQuery(getActivity()).execute(coordinate).get();

                if (gpsLocation != null) {
                    mQueryString = gpsLocation.name;
                    results.add(gpsLocation);
                    mAdapter.setLocations(results);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            updateLocation();
        }
    }

    private void updateLocation()
    {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_LOCATION_REQUEST_CODE);
            return;
        }

        LocationManager locMan = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetEnabled = locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Location location = null;

        if (isGPSEnabled) {
            location = locMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (location == null)
                location = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (location == null)
                locMan.requestSingleUpdate(LocationManager.GPS_PROVIDER, mLocListnr, null);
            else {
                mLocation = location;
                fetchGeoLocation();
            }
        } else if (isNetEnabled) {
            location = locMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (location == null)
                locMan.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, mLocListnr, null);
            else {
                mLocation = location;
                fetchGeoLocation();
            }
        } else {
            Toast.makeText(getActivity(), "Unable to get location", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_LOCATION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    fetchGeoLocation();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getActivity(), "Location access denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    private LocationListener mLocListnr = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mLocation = location;
            fetchGeoLocation();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };
}