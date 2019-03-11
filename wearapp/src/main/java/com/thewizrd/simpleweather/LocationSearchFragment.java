package com.thewizrd.simpleweather;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.support.wear.widget.SwipeDismissFrameLayout;
import android.support.wear.widget.WearableLinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.adapters.LocationQueryAdapter;
import com.thewizrd.shared_resources.controls.LocationQuery;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.here.HERELocationProvider;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

public class LocationSearchFragment extends SwipeDismissFragment {
    private WearableRecyclerView mRecyclerView;
    private LocationQueryAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ProgressBar mProgressBar;
    private EditText mSearchView;
    private Activity mActivity;

    private FloatingActionButton keyboardButton;
    private FloatingActionButton voiceButton;
    private SwipeDismissFrameLayout swipeViewLayout;
    private SwipeDismissFrameLayout.Callback swipeCallback;

    private CancellationTokenSource cts;

    private WeatherManager wm;

    private static final int REQUEST_CODE_VOICE_INPUT = 0;

    public void setRecyclerOnClickListener(RecyclerOnClickListenerInterface listener) {
        recyclerClickListener = listener;
    }

    public LocationSearchFragment() {
        // Required empty public constructor
        cts = new CancellationTokenSource();
        wm = WeatherManager.getInstance();
        setUserVisibleHint(true);
    }

    public CancellationTokenSource getCancellationTokenSource() {
        return cts;
    }

    public void ctsCancel() {
        if (cts != null) cts.cancel();
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
        mActivity = (Activity) context;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cts != null) cts.cancel();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cts != null) cts.cancel();
        mActivity = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (cts != null) cts.cancel();
        mActivity = null;
    }

    private void runOnUiThread(Runnable action) {
        if (mActivity != null) {
            mActivity.runOnUiThread(action);
        }
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
                        runOnUiThread(new Runnable() {
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

                    // Need to get FULL location data for HERE API
                    // Data provided is incomplete
                    if (WeatherAPI.HERE.equals(query_vm.getLocationSource())
                            && query_vm.getLocationLat() == -1 && query_vm.getLocationLong() == -1
                            && query_vm.getLocationTZLong() == null) {
                        final LocationQueryViewModel loc = query_vm;
                        query_vm = new AsyncTask<LocationQueryViewModel>().await(new Callable<LocationQueryViewModel>() {
                            @Override
                            public LocationQueryViewModel call() throws Exception {
                                return new HERELocationProvider().getLocationfromLocID(loc.getLocationQuery(), loc.getWeatherSource());
                            }
                        });
                    }

                    // Get weather data
                    LocationData location = new LocationData(query_vm);
                    if (!location.isValid()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(App.getInstance().getAppContext(), R.string.werror_noweather, Toast.LENGTH_SHORT).show();
                            }
                        });
                        showLoading(false);
                        return;
                    }
                    Weather weather = Settings.getWeatherData(location.getQuery());
                    if (weather == null) {
                        try {
                            weather = wm.getWeather(location);
                        } catch (final WeatherException wEx) {
                            weather = null;
                            runOnUiThread(new Runnable() {
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
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.getDataset().clear();
                            mAdapter.notifyDataSetChanged();
                            mRecyclerView.setEnabled(false);
                        }
                    });

                    // Save weather data
                    Settings.saveHomeData(location);
                    if (wm.supportsAlerts() && weather.getWeatherAlerts() != null)
                        Settings.saveWeatherAlerts(location, weather.getWeatherAlerts());
                    Settings.saveWeatherData(weather);

                    // If we're using search
                    // make sure gps feature is off
                    Settings.setFollowGPS(false);
                    Settings.setWeatherLoaded(true);

                    // Start WeatherNow Activity with weather data
                    Intent intent = new Intent(mActivity, MainActivity.class);
                    intent.putExtra("data", location.toJson());

                    mActivity.startActivity(intent);
                    mActivity.finishAffinity();
                }
            });
        }
    };

    private void showLoading(final boolean show) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        // Inflate the layout for this fragment
        inflater.inflate(R.layout.fragment_location_search, (ViewGroup) view, true);

        swipeViewLayout = view.findViewById(R.id.recycler_view_layout);
        swipeCallback = new SwipeDismissFrameLayout.Callback() {
            @Override
            public void onDismissed(SwipeDismissFrameLayout layout) {
                layout.setVisibility(View.GONE);
                //layout.reset();
            }
        };
        swipeViewLayout.addCallback(swipeCallback);
        keyboardButton = view.findViewById(R.id.keyboard_button);
        keyboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchView.setVisibility(View.VISIBLE);
                mSearchView.requestFocus();
                showInputMethod(mSearchView);
            }
        });
        voiceButton = view.findViewById(R.id.voice_button);
        voiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchView.setVisibility(View.GONE);
                mSearchView.setText("");
                view.requestFocus();
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        .putExtra(RecognizerIntent.EXTRA_PROMPT, mActivity.getString(R.string.location_search_hint));
                startActivityForResult(intent, REQUEST_CODE_VOICE_INPUT);
            }
        });

        mProgressBar = view.findViewById(R.id.progressBar);
        mSearchView = view.findViewById(R.id.search_view);
        mSearchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // If we're using searchfragment
                // make sure gps feature is off
                if (Settings.useFollowGPS()) {
                    Settings.setFollowGPS(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mSearchView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showInputMethod(v.findFocus());
                } else {
                    hideInputMethod(v);
                }
            }
        });
        mSearchView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    doSearchAction();

                    // If we're using searchfragment
                    // make sure gps feature is off
                    if (Settings.useFollowGPS()) {
                        Settings.setFollowGPS(false);
                    }

                    return true;
                }
                return false;
            }
        });

        mRecyclerView = view.findViewById(R.id.recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // To align the edge children (first and last) with the center of the screen
        mRecyclerView.setEdgeItemsCenteringEnabled(true);

        // use a linear layout manager
        mLayoutManager = new WearableLinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new LocationQueryAdapter(new ArrayList<LocationQueryViewModel>());
        mAdapter.setOnClickListener(recyclerClickListener);
        mRecyclerView.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onDestroyView() {
        swipeViewLayout.removeCallback(swipeCallback);
        super.onDestroyView();
    }

    private void doSearchAction() {
        mProgressBar.setVisibility(View.VISIBLE);
        fetchLocations(mSearchView.getText().toString());
        hideInputMethod(mSearchView);
        mSearchView.setVisibility(View.GONE);
        swipeViewLayout.setVisibility(View.VISIBLE);
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
                                mProgressBar.setVisibility(View.GONE);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK || data == null)
            return;

        switch (requestCode) {
            case REQUEST_CODE_VOICE_INPUT:
                String text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);

                if (!StringUtils.isNullOrWhitespace(text)) {
                    mSearchView.setText(text);
                    doSearchAction();
                }
                break;
            default:
                break;
        }
    }

    private void showInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    private void hideInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}