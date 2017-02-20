package com.thewizrd.simpleweather;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.thewizrd.simpleweather.weather.weatherunderground.AutoCompleteQuery;
import com.thewizrd.simpleweather.weather.weatherunderground.GeopositionQuery;
import com.thewizrd.simpleweather.weather.weatherunderground.data.AC_Location;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class LocationSearchFragment extends Fragment {

    OnLocationSelectedListener mCallback;

    // Container Activity must implement this interface
    public interface OnLocationSelectedListener {
        public void onLocationSelected(String query);
    }

    private Location mLocation;
    private String mQueryString;
    private RecyclerView mRecyclerView;
    private LocationQueryAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private static final int PERMISSION_LOCATION_REQUEST_CODE = 0;

    public LocationSearchFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallback = (OnLocationSelectedListener) context;
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
        mRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    View child = rv.findChildViewUnder(e.getX(), e.getY());
                    if (child != null)  {
                        LocationQueryView lqv = (LocationQueryView) child;
                        mCallback.onLocationSelected(lqv.getLocationQuery());
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
            }
        });

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new LocationQueryAdapter(getActivity(), new ArrayList<AC_Location>());
        mRecyclerView.setAdapter(mAdapter);
    }

    public void fetchLocations(String queryString) {
        if (!TextUtils.equals(mQueryString, queryString)) {
            mQueryString = queryString;
            // Get locations
            List<AC_Location> locations = new ArrayList<>();

            if (TextUtils.isEmpty(mQueryString)) {
                // No results
            } else {
                try {
                    List<AC_Location> results = new AutoCompleteQuery().execute(mQueryString).get();

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
        List<AC_Location> locations = new ArrayList<>();

        if (mLocation != null) {
            // Get geo location
            try {
                List<AC_Location> results = new GeopositionQuery().execute(mLocation).get();

                if (results.size() > 0)
                    locations.addAll(results);
            } catch (Exception e) {
                e.printStackTrace();
            }

            mQueryString = locations.get(0).name;
            mAdapter.setLocations(locations);
        }
        else {
            updateLocation();
        }
    }

    private void updateLocation()
    {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
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

    public Location getLocation() { return mLocation; }
    public String getQueryString() { return mQueryString; }
}
