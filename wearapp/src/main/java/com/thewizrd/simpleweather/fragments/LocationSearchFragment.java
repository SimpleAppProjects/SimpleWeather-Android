package com.thewizrd.simpleweather.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.wear.widget.SwipeDismissFrameLayout;
import androidx.wear.widget.WearableLinearLayoutManager;

import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.AsyncTaskEx;
import com.thewizrd.shared_resources.CallableEx;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.adapters.LocationQueryAdapter;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.here.HERELocationProvider;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.CommonActions;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.weatherdata.Forecasts;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.HourlyForecasts;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentLocationSearchBinding;
import com.thewizrd.simpleweather.main.MainActivity;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class LocationSearchFragment extends SwipeDismissFragment {
    private FragmentLocationSearchBinding binding;
    private LocationQueryAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsLogger.logEvent("LocationSearchFragment: onCreate");
    }

    @Override
    public void onPause() {
        super.onPause();
        ctsCancel();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ctsCancel();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ctsCancel();
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
                    if (mActivity != null) {
                        // Get selected query view
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
                                    Toast.makeText(mActivity, R.string.werror_invalidkey, Toast.LENGTH_SHORT).show();
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

                        String country_code = query_vm.getLocationCountry();
                        if (!StringUtils.isNullOrWhitespace(country_code))
                            country_code = country_code.toLowerCase();

                        if (WeatherAPI.NWS.equals(Settings.getAPI()) && !("usa".equals(country_code) || "us".equals(country_code))) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mActivity, R.string.error_message_weather_us_only, Toast.LENGTH_SHORT).show();
                                }
                            });
                            showLoading(false);
                            return;
                        }

                        // Need to get FULL location data for HERE API
                        // Data provided is incomplete
                        if (WeatherAPI.HERE.equals(query_vm.getLocationSource())
                                && query_vm.getLocationLat() == -1 && query_vm.getLocationLong() == -1
                                && query_vm.getLocationTZLong() == null) {
                            final LocationQueryViewModel loc = query_vm;
                            try {
                                query_vm = new AsyncTaskEx<LocationQueryViewModel, WeatherException>().await(new CallableEx<LocationQueryViewModel, WeatherException>() {
                                    @Override
                                    public LocationQueryViewModel call() throws WeatherException {
                                        return new HERELocationProvider().getLocationfromLocID(loc.getLocationQuery(), loc.getWeatherSource());
                                    }
                                });
                            } catch (final WeatherException wEx) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(mActivity, wEx.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                                showLoading(false);
                            }
                        }

                        // Get weather data
                        LocationData location = new LocationData(query_vm);
                        if (!location.isValid()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(mActivity, R.string.werror_noweather, Toast.LENGTH_SHORT).show();
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
                                        Toast.makeText(mActivity, wEx.getMessage(), Toast.LENGTH_SHORT).show();
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
                                if (binding != null)
                                    binding.recyclerView.setEnabled(false);
                            }
                        });

                        // Save weather data
                        Settings.saveHomeData(location);
                        if (wm.supportsAlerts() && weather.getWeatherAlerts() != null)
                            Settings.saveWeatherAlerts(location, weather.getWeatherAlerts());
                        Settings.saveWeatherData(weather);
                        Settings.saveWeatherForecasts(new Forecasts(weather.getQuery(), weather.getForecast(), weather.getTxtForecast()));
                        final Weather finalWeather = weather;
                        Settings.saveWeatherForecasts(location.getQuery(), weather.getHrForecast() == null ? null :
                                Collections2.transform(weather.getHrForecast(), new Function<HourlyForecast, HourlyForecasts>() {
                                    @NullableDecl
                                    @Override
                                    public HourlyForecasts apply(@NullableDecl HourlyForecast input) {
                                        return new HourlyForecasts(finalWeather.getQuery(), input);
                                    }
                                }));

                        // If we're changing locations, trigger an update
                        if (Settings.isWeatherLoaded()) {
                            LocalBroadcastManager.getInstance(mActivity)
                                    .sendBroadcast(new Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE));
                        }

                        // If we're using search
                        // make sure gps feature is off
                        Settings.setFollowGPS(false);
                        Settings.setWeatherLoaded(true);

                        // Start WeatherNow Activity with weather data
                        Intent intent = new Intent(mActivity, MainActivity.class);
                        intent.putExtra(Constants.KEY_DATA, JSONParser.serializer(location, LocationData.class));

                        mActivity.startActivity(intent);
                        mActivity.finishAffinity();
                    }
                }
            });
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ViewGroup view = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        // Inflate the layout for this fragment
        binding = FragmentLocationSearchBinding.inflate(inflater, view, true);

        swipeCallback = new SwipeDismissFrameLayout.Callback() {
            @Override
            public void onDismissed(SwipeDismissFrameLayout layout) {
                layout.setVisibility(View.GONE);
            }
        };
        binding.recyclerViewLayout.addCallback(swipeCallback);
        binding.keyboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.searchView.setVisibility(View.VISIBLE);
                binding.searchView.requestFocus();
                showInputMethod(binding.searchView);
            }
        });
        binding.voiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.searchView.setVisibility(View.GONE);
                binding.searchView.setText("");
                view.requestFocus();
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        .putExtra(RecognizerIntent.EXTRA_PROMPT, mActivity.getString(R.string.location_search_hint));
                startActivityForResult(intent, REQUEST_CODE_VOICE_INPUT);
            }
        });

        binding.searchView.addTextChangedListener(new TextWatcher() {
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
        binding.searchView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showInputMethod(v.findFocus());
                } else {
                    hideInputMethod(v);
                }
            }
        });
        binding.searchView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.recyclerView.setHasFixedSize(true);

        // To align the edge children (first and last) with the center of the screen
        binding.recyclerView.setEdgeItemsCenteringEnabled(true);

        // use a linear layout manager
        mLayoutManager = new WearableLinearLayoutManager(mActivity);
        binding.recyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new LocationQueryAdapter(new ArrayList<LocationQueryViewModel>());
        mAdapter.setOnClickListener(recyclerClickListener);
        binding.recyclerView.setAdapter(mAdapter);

        binding.recyclerView.requestFocus();

        return view;
    }

    @Override
    public void onDestroyView() {
        hideInputMethod(binding.searchView);
        binding.recyclerViewLayout.removeCallback(swipeCallback);
        super.onDestroyView();
        binding = null;
    }

    private void doSearchAction() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.searchView.setVisibility(View.GONE);
        binding.recyclerViewLayout.setVisibility(View.VISIBLE);
        binding.recyclerViewLayout.requestFocus();
        hideInputMethod(binding.searchView);
        fetchLocations(binding.searchView.getText().toString());
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

                    final Collection<LocationQueryViewModel> results;
                    try {
                        results = wm.getLocations(queryString);
                    } catch (final WeatherException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mActivity, e.getMessage(), Toast.LENGTH_SHORT).show();
                                mAdapter.setLocations(Collections.singletonList(new LocationQueryViewModel()));
                            }
                        });
                        return;
                    }

                    if (ctsToken.isCancellationRequested()) return;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.setLocations(new ArrayList<>(results));
                            if (binding != null)
                                binding.progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            });
        } else if (StringUtils.isNullOrWhitespace(queryString)) {
            // Cancel pending searches
            ctsCancel();
            binding.progressBar.setVisibility(View.GONE);
            binding.recyclerViewLayout.setVisibility(View.GONE);
            binding.recyclerViewLayout.clearFocus();
            // Hide flyout if query is empty or null
            mAdapter.getDataset().clear();
            mAdapter.notifyDataSetChanged();
        }
    }

    private void showLoading(final boolean show) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (binding != null)
                    binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK || data == null)
            return;

        switch (requestCode) {
            case REQUEST_CODE_VOICE_INPUT:
                List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (results != null && results.size() > 0) {
                    String text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);

                    if (!StringUtils.isNullOrWhitespace(text)) {
                        binding.searchView.setText(text);
                        doSearchAction();
                    }
                }
                break;
            default:
                break;
        }
    }

    private void showInputMethod(View view) {
        if (mActivity != null) {
            InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
        }
    }

    private void hideInputMethod(View view) {
        if (mActivity != null) {
            InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null && view != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}