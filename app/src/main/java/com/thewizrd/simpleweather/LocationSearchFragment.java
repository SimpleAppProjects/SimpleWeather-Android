package com.thewizrd.simpleweather;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.adapters.LocationQueryAdapter;
import com.thewizrd.shared_resources.controls.LocationQuery;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.wearable.WearableDataListenerService;

import java.util.ArrayList;
import java.util.Collection;

public class LocationSearchFragment extends Fragment {
    private RecyclerView mRecyclerView;
    private LocationQueryAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ProgressBar mProgressBar;
    private View mClearButton;
    private EditText mSearchView;
    private FragmentActivity mActivity;

    private CancellationTokenSource cts;

    private WeatherManager wm;

    public void setRecyclerOnClickListener(RecyclerOnClickListenerInterface listener) {
        recyclerClickListener = listener;
    }

    // Widget id for ConfigurationActivity
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    public LocationSearchFragment() {
        // Required empty public constructor
        wm = WeatherManager.getInstance();
        cts = new CancellationTokenSource();
    }

    public CancellationTokenSource getCancellationTokenSource() {
        return cts;
    }

    public void ctsCancel() {
        cts.cancel();
        cts = new CancellationTokenSource();
    }

    public boolean ctsCancelRequested() {
        if (cts == null)
            return false;
        else
            return cts.getToken().isCancellationRequested();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (FragmentActivity) context;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mActivity = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }

    public LocationQueryAdapter getAdapter() {
        return mAdapter;
    }

    private RecyclerOnClickListenerInterface recyclerClickListener = new RecyclerOnClickListenerInterface() {
        @Override
        public void onClick(final View view, final int position) {
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    // Get selected query view
                    LocationQuery v = (LocationQuery) view;
                    LocationQueryViewModel query_vm = null;

                    try {
                        if (!StringUtils.isNullOrEmpty(mAdapter.getDataset().get(position).getLocationQuery()))
                            query_vm = mAdapter.getDataset().get(position);
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        query_vm = null;
                    } finally {
                        if (query_vm == null)
                            query_vm = new LocationQueryViewModel();
                    }

                    if (StringUtils.isNullOrWhitespace(query_vm.getLocationQuery())) {
                        // Stop since there is no valid query
                        return;
                    }

                    if (Settings.usePersonalKey() && StringUtils.isNullOrWhitespace(Settings.getAPIKEY()) && wm.isKeyRequired()) {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(App.getInstance().getAppContext(), R.string.werror_invalidkey, Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }

                    // Cancel pending search
                    ctsCancel();
                    CancellationToken ctsToken = cts.getToken();

                    showLoading(true);

                    if (ctsToken.isCancellationRequested()) {
                        showLoading(false);
                        return;
                    }

                    // Get weather data
                    LocationData location = new LocationData(query_vm);
                    if (!location.isValid()) {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(App.getInstance().getAppContext(), R.string.werror_noweather, Toast.LENGTH_SHORT).show();
                                showLoading(false);
                            }
                        });
                        return;
                    }
                    Weather weather = Settings.getWeatherData(location.getQuery());
                    if (weather == null) {
                        try {
                            weather = wm.getWeather(location);
                        } catch (final WeatherException wEx) {
                            weather = null;
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(App.getInstance().getAppContext(), wEx.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    if (weather == null) {
                        showLoading(false);
                        return;
                    }

                    // We got our data so disable controls just in case
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.getDataset().clear();
                            mAdapter.notifyDataSetChanged();
                            mRecyclerView.setEnabled(false);
                        }
                    });

                    // Save weather data
                    Settings.deleteLocations();
                    Settings.addLocation(location);
                    if (wm.supportsAlerts() && weather.getWeatherAlerts() != null)
                        Settings.saveWeatherAlerts(location, weather.getWeatherAlerts());
                    Settings.saveWeatherData(weather);

                    // If we're using search
                    // make sure gps feature is off
                    Settings.setFollowGPS(false);
                    Settings.setWeatherLoaded(true);

                    // Send data for wearables
                    WearableDataListenerService.enqueueWork(mActivity,
                            new Intent(mActivity, WearableDataListenerService.class)
                                    .setAction(WearableDataListenerService.ACTION_SENDSETTINGSUPDATE));
                    WearableDataListenerService.enqueueWork(mActivity,
                            new Intent(mActivity, WearableDataListenerService.class)
                                    .setAction(WearableDataListenerService.ACTION_SENDLOCATIONUPDATE));
                    WearableDataListenerService.enqueueWork(mActivity,
                            new Intent(mActivity, WearableDataListenerService.class)
                                    .setAction(WearableDataListenerService.ACTION_SENDWEATHERUPDATE));

                    if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                        // Start WeatherNow Activity with weather data
                        Intent intent = new Intent(mActivity, MainActivity.class);
                        intent.putExtra("data", location.toJson());

                        mActivity.startActivity(intent);
                        mActivity.finishAffinity();
                    } else {
                        // Create return intent
                        Intent resultValue = new Intent();
                        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                        resultValue.putExtra("data", location.toJson());
                        mActivity.setResult(Activity.RESULT_OK, resultValue);
                        mActivity.finish();
                    }
                }
            });
        }
    };

    private void showLoading(final boolean show) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);

                if (show || (!show && StringUtils.isNullOrEmpty(mSearchView.getText().toString())))
                    mClearButton.setVisibility(View.GONE);
                else
                    mClearButton.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mAppWidgetId = getArguments().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_location_search, container, false);
        setupView(view);

        mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return view;
    }

    private void setupView(View view) {
        mProgressBar = mActivity.findViewById(R.id.search_progressBar);
        mClearButton = mActivity.findViewById(R.id.search_close_button);
        mSearchView = mActivity.findViewById(R.id.search_view);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mProgressBar.setIndeterminateDrawable(
                    ContextCompat.getDrawable(mActivity, R.drawable.progressring));
        }

        mRecyclerView = view.findViewById(R.id.recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new LocationQueryAdapter(new ArrayList<LocationQueryViewModel>());
        mAdapter.setOnClickListener(recyclerClickListener);
        mRecyclerView.setAdapter(mAdapter);
    }

    public void fetchLocations(final String queryString) {
        // Cancel pending searches
        ctsCancel();

        if (!StringUtils.isNullOrWhitespace(queryString)) {
            AsyncTask.run(new Runnable() {
                @Override
                public void run() {
                    CancellationToken ctsToken = cts.getToken();

                    if (ctsToken.isCancellationRequested()) return;

                    final Collection<LocationQueryViewModel> results = wm.getLocations(queryString);

                    if (ctsToken.isCancellationRequested()) return;

                    if (mActivity != null) {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mAdapter.setLocations(new ArrayList<>(results));
                            }
                        });
                    }
                }
            });
        } else if (StringUtils.isNullOrWhitespace(queryString)) {
            // Cancel pending searches
            ctsCancel();
            // Hide flyout if query is empty or null
            mAdapter.getDataset().clear();
            mAdapter.notifyDataSetChanged();
        }
    }
}