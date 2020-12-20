package com.thewizrd.simpleweather.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.transition.MaterialContainerTransform;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.adapters.LocationQueryAdapter;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.lifecycle.LifecycleRunnable;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.locationdata.here.HERELocationProvider;
import com.thewizrd.shared_resources.tasks.AsyncTask;
import com.thewizrd.shared_resources.tasks.CallableEx;
import com.thewizrd.shared_resources.tasks.TaskUtils;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.CustomException;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.LocationUtils;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.utils.WeatherUtils;
import com.thewizrd.shared_resources.weatherdata.Forecasts;
import com.thewizrd.shared_resources.weatherdata.HourlyForecast;
import com.thewizrd.shared_resources.weatherdata.HourlyForecasts;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentLocationSearchBinding;
import com.thewizrd.simpleweather.databinding.SearchActionBarBinding;
import com.thewizrd.simpleweather.snackbar.Snackbar;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;
import com.thewizrd.simpleweather.snackbar.SnackbarWindowAdjustCallback;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

public class LocationSearchFragment extends WindowColorFragment {
    private FragmentLocationSearchBinding binding;
    private SearchActionBarBinding searchBarBinding;
    private LocationQueryAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private CancellationTokenSource cts = new CancellationTokenSource();
    private WeatherManager wm = WeatherManager.getInstance();

    private static final String KEY_SEARCHTEXT = "search_text";

    private void resetTokenSource() {
        if (cts != null) cts.cancel();
        cts = new CancellationTokenSource();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        initSnackManager();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsLogger.logEvent("LocationSearchFragment: onCreate");
        setSharedElementEnterTransition(new MaterialContainerTransform().setDuration(Constants.ANIMATION_DURATION));
    }

    @Override
    public void onPause() {
        searchBarBinding.searchView.clearFocus();
        cts.cancel();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        cts.cancel();
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        cts.cancel();
        unloadSnackManager();
        super.onDetach();
    }

    @NonNull
    @Override
    public SnackbarManager createSnackManager() {
        SnackbarManager mSnackMgr = new SnackbarManager(getAppCompatActivity().findViewById(android.R.id.content));
        mSnackMgr.setSwipeDismissEnabled(true);
        mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);
        return mSnackMgr;
    }

    public LocationQueryAdapter getAdapter() {
        return mAdapter;
    }

    private RecyclerOnClickListenerInterface recyclerClickListener = new RecyclerOnClickListenerInterface() {
        @Override
        public void onClick(final View view, final int position) {
            AnalyticsLogger.logEvent("LocationsFragment: searchFragment click");
            runWithView(new LifecycleRunnable(getViewLifecycleOwner().getLifecycle()) {
                @Override
                public void run() {
                    if (!isActive() || !isViewAlive()) return;

                    showLoading(true);
                    enableRecyclerView(false);

                    // Cancel other tasks
                    resetTokenSource();
                    final CancellationToken token = cts.getToken();

                    AsyncTask.create(new Callable<LocationData>() {
                        @Override
                        public LocationData call() throws CustomException, InterruptedException, WeatherException {
                            LocationQueryViewModel queryResult = new LocationQueryViewModel();

                            if (!StringUtils.isNullOrEmpty(mAdapter.getDataset().get(position).getLocationQuery()))
                                queryResult = mAdapter.getDataset().get(position);

                            if (StringUtils.isNullOrWhitespace(queryResult.getLocationQuery())) {
                                // Stop since there is no valid query
                                throw new CustomException(R.string.error_retrieve_location);
                            }

                            final boolean isUS = LocationUtils.isUS(queryResult.getLocationCountry());

                            if (!Settings.isWeatherLoaded()) {
                                // Default US location to NWS
                                if (isUS) {
                                    Settings.setAPI(WeatherAPI.NWS);
                                } else {
                                    Settings.setAPI(WeatherAPI.HERE);
                                }
                                wm.updateAPI();
                            }

                            if (Settings.usePersonalKey() && StringUtils.isNullOrWhitespace(Settings.getAPIKEY()) && wm.isKeyRequired()) {
                                throw new CustomException(R.string.werror_invalidkey);
                            }

                            TaskUtils.throwIfCancellationRequested(token);

                            if (WeatherAPI.NWS.equals(Settings.getAPI()) && !LocationUtils.isUS(queryResult.getLocationCountry())) {
                                throw new CustomException(R.string.error_message_weather_us_only);
                            }

                            // Need to get FULL location data for HERE API
                            // Data provided is incomplete
                            if (WeatherAPI.HERE.equals(queryResult.getLocationSource())
                                    && queryResult.getLocationLat() == -1 && queryResult.getLocationLong() == -1
                                    && queryResult.getLocationTZLong() == null) {
                                final LocationQueryViewModel loc = queryResult;
                                queryResult = AsyncTask.await(new CallableEx<LocationQueryViewModel, WeatherException>() {
                                    @Override
                                    public LocationQueryViewModel call() throws WeatherException {
                                        return new HERELocationProvider().getLocationfromLocID(loc.getLocationQuery(), loc.getWeatherSource());
                                    }
                                }, token);
                            }

                            if (queryResult == null) {
                                throw new InterruptedException();
                            }

                            // Check if location already exists
                            List<LocationData> locData = Settings.getLocationData();
                            final LocationQueryViewModel finalQueryResult = queryResult;
                            LocationData loc = Iterables.find(locData, new Predicate<LocationData>() {
                                @Override
                                public boolean apply(@NullableDecl LocationData input) {
                                    return input != null && input.getQuery().equals(finalQueryResult.getLocationQuery());
                                }
                            }, null);

                            if (loc != null) {
                                // Location exists; return
                                return null;
                            }

                            TaskUtils.throwIfCancellationRequested(token);

                            LocationData location = new LocationData(queryResult);
                            if (!location.isValid()) {
                                throw new CustomException(R.string.werror_noweather);
                            }
                            Weather weather = Settings.getWeatherData(location.getQuery());
                            if (weather == null) {
                                weather = wm.getWeather(location);
                            }

                            if (weather == null) {
                                throw new WeatherException(WeatherUtils.ErrorStatus.NOWEATHER);
                            } else if (wm.supportsAlerts() && wm.needsExternalAlertData()) {
                                weather.setWeatherAlerts(wm.getAlerts(location));
                            }

                            // Save data
                            Settings.addLocation(location);
                            if (wm.supportsAlerts() && weather.getWeatherAlerts() != null)
                                Settings.saveWeatherAlerts(location, weather.getWeatherAlerts());
                            Settings.saveWeatherData(weather);
                            Settings.saveWeatherForecasts(new Forecasts(weather.getQuery(), weather.getForecast(), weather.getTxtForecast()));
                            final Weather finalWeather = weather;
                            Settings.saveWeatherForecasts(location.getQuery(), weather.getHrForecast() == null ? null :
                                    Collections2.transform(weather.getHrForecast(), new Function<HourlyForecast, HourlyForecasts>() {
                                        @NonNull
                                        @Override
                                        public HourlyForecasts apply(@NullableDecl HourlyForecast input) {
                                            return new HourlyForecasts(finalWeather.getQuery(), input);
                                        }
                                    }));

                            Settings.setWeatherLoaded(true);

                            return location;
                        }
                    }).addOnSuccessListener(new OnSuccessListener<LocationData>() {
                        @Override
                        public void onSuccess(final LocationData result) {
                            runWithView(new Runnable() {
                                @Override
                                public void run() {
                                    // Go back to where we started
                                    NavController navController = Navigation.findNavController(binding.getRoot());
                                    if (result != null) {
                                        navController.getPreviousBackStackEntry()
                                                .getSavedStateHandle()
                                                .set(Constants.KEY_DATA, JSONParser.serializer(result, LocationData.class));
                                    }
                                    navController.navigateUp();
                                }
                            });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull final Exception e) {
                            runWithView(new Runnable() {
                                @Override
                                public void run() {
                                    if (e instanceof WeatherException || e instanceof CustomException) {
                                        showSnackbar(Snackbar.make(e.getMessage(), Snackbar.Duration.SHORT),
                                                new SnackbarWindowAdjustCallback(getAppCompatActivity()));
                                    } else {
                                        showSnackbar(Snackbar.make(R.string.error_retrieve_location, Snackbar.Duration.SHORT),
                                                new SnackbarWindowAdjustCallback(getAppCompatActivity()));
                                    }
                                    showLoading(false);
                                    enableRecyclerView(true);
                                }
                            });
                        }
                    });
                }
            });
        }
    };

    private void showLoading(final boolean show) {
        if (searchBarBinding != null) {
            searchBarBinding.searchProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);

            if (show || StringUtils.isNullOrEmpty(searchBarBinding.searchView.getText().toString()))
                searchBarBinding.searchCloseButton.setVisibility(View.GONE);
            else
                searchBarBinding.searchCloseButton.setVisibility(View.VISIBLE);
        }
    }

    public void enableRecyclerView(final boolean enable) {
        binding.recyclerView.setEnabled(enable);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentLocationSearchBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        searchBarBinding = binding.searchBar;
        searchBarBinding.setLifecycleOwner(getViewLifecycleOwner());
        View view = binding.getRoot();

        ViewCompat.setTransitionName(view, Constants.SHARED_ELEMENT);
        ViewGroupCompat.setTransitionGroup((ViewGroup) view, true);

        // Initialize
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigateUp();
            }
        });
        searchBarBinding.searchBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigateUp();
            }
        });

        // For landscape orientation
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                layoutParams.setMargins(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
                return insets;
            }
        });

        searchBarBinding.searchCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchBarBinding.searchView.setText("");
            }
        });
        searchBarBinding.searchCloseButton.setVisibility(View.GONE);

        searchBarBinding.searchView.addTextChangedListener(new TextWatcher() {
            private Timer timer = new Timer();
            private final long DELAY = 1000; // milliseconds

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // nothing to do here
            }

            @Override
            public void onTextChanged(final CharSequence s, int start, int before, int count) {
                // user is typing: reset already started timer (if existing)
                if (timer != null) {
                    timer.cancel();
                }
            }

            @Override
            public void afterTextChanged(final Editable e) {
                // If string is null or empty (ex. from clearing text) run right away
                if (StringUtils.isNullOrEmpty(e.toString())) {
                    runSearchOp(e);
                } else {
                    timer = new Timer();
                    timer.schedule(
                            new TimerTask() {
                                @Override
                                public void run() {
                                    runSearchOp(e);
                                }
                            }, DELAY
                    );
                }
            }

            private void runSearchOp(final Editable e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final String newText = e.toString();

                        if (searchBarBinding != null) {
                            searchBarBinding.searchCloseButton.setVisibility(StringUtils.isNullOrEmpty(newText) ? View.GONE : View.VISIBLE);
                            fetchLocations(newText);
                        }
                    }
                });
            }
        });
        searchBarBinding.searchView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showInputMethod(v.findFocus());
                } else {
                    hideInputMethod(v);
                }
            }
        });
        searchBarBinding.searchView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    fetchLocations(v.getText().toString());
                    hideInputMethod(v);
                    return true;
                }
                return false;
            }
        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            searchBarBinding.searchProgressBar.setIndeterminateDrawable(
                    ContextCompat.getDrawable(getAppCompatActivity(), R.drawable.progressring));
        }

        /*
           Capture touch events on RecyclerView
           We're not using ADJUST_RESIZE so hide the keyboard when necessary
           Hide the keyboard if we're scrolling to the bottom (so the bottom items behind the keyboard are visible)
           Leave the keyboard up if we're scrolling to the top (items at the top are already visible)
        */
        binding.recyclerView.setOnTouchListener(new View.OnTouchListener() {
            private int mY;
            private boolean shouldCloseKeyboard = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: // Pointer down
                        mY = (int) event.getY();
                        break;
                    case MotionEvent.ACTION_UP: // Pointer raised/lifted
                        mY = (int) event.getY();

                        if (shouldCloseKeyboard) {
                            hideInputMethod(v);
                            shouldCloseKeyboard = false;
                        }
                        break;
                    case MotionEvent.ACTION_MOVE: // Scroll Action
                        int newY = (int) event.getY();
                        int dY = mY - newY;

                        mY = newY;
                        // Set flag to hide the keyboard if we're scrolling down
                        // So we can see what's behind the keyboard
                        shouldCloseKeyboard = dY > 0;
                        break;
                }

                return false;
            }
        });
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (recyclerView.computeVerticalScrollOffset() > 0) {
                    ViewCompat.setElevation(searchBarBinding.getRoot(), ActivityUtils.dpToPx(getAppCompatActivity(), 4f));
                } else {
                    ViewCompat.setElevation(searchBarBinding.getRoot(), 0f);
                }
            }
        });

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        binding.recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getAppCompatActivity());
        binding.recyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new LocationQueryAdapter(new ArrayList<LocationQueryViewModel>());
        mAdapter.setOnClickListener(recyclerClickListener);
        binding.recyclerView.setAdapter(mAdapter);

        if (savedInstanceState != null) {
            String text = savedInstanceState.getString(KEY_SEARCHTEXT);
            if (!StringUtils.isNullOrWhitespace(text)) {
                searchBarBinding.searchView.setText(text, TextView.BufferType.EDITABLE);
            }
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requestSearchbarFocus();
    }

    @Override
    public void updateWindowColors() {
        int bg_color = Settings.getUserThemeMode() != UserThemeMode.AMOLED_DARK ?
                ActivityUtils.getColor(getAppCompatActivity(), android.R.attr.colorBackground) : Colors.BLACK;

        binding.rootView.setBackgroundColor(bg_color);
        binding.rootView.setStatusBarBackgroundColor(bg_color);
        searchBarBinding.getRoot().setBackgroundColor(bg_color);
    }

    @Override
    public void onDestroyView() {
        cts.cancel();
        super.onDestroyView();
        searchBarBinding = null;
        binding = null;
    }

    public void fetchLocations(final String queryString) {
        // Cancel pending searches
        resetTokenSource();

        if (!StringUtils.isNullOrWhitespace(queryString)) {
            final CancellationToken token = cts.getToken();

            AsyncTask.create(new Callable<Collection<LocationQueryViewModel>>() {
                @Override
                public Collection<LocationQueryViewModel> call() throws Exception {
                    return wm.getLocations(queryString);
                }
            }, token).addOnSuccessListener(new OnSuccessListener<Collection<LocationQueryViewModel>>() {
                @Override
                public void onSuccess(final Collection<LocationQueryViewModel> results) {
                    runWithView(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.setLocations(new ArrayList<>(results));
                        }
                    });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull final Exception e) {
                    runWithView(new Runnable() {
                        @Override
                        public void run() {
                            if (e instanceof WeatherException) {
                                showSnackbar(Snackbar.make(e.getMessage(), Snackbar.Duration.SHORT),
                                        new SnackbarWindowAdjustCallback(getAppCompatActivity()));
                            }
                            mAdapter.setLocations(Collections.singletonList(new LocationQueryViewModel()));
                        }
                    });
                }
            });
        } else if (StringUtils.isNullOrWhitespace(queryString)) {
            // Cancel pending searches
            resetTokenSource();
            // Hide flyout if query is empty or null
            mAdapter.getDataset().clear();
            mAdapter.notifyDataSetChanged();
        }
    }

    public void requestSearchbarFocus() {
        if (searchBarBinding != null) {
            searchBarBinding.searchView.requestFocus();
        }
    }

    private void showInputMethod(View view) {
        if (getAppCompatActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getAppCompatActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null && view != null) {
                imm.showSoftInput(view, 0);
            }
        }
    }

    private void hideInputMethod(View view) {
        if (getAppCompatActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getAppCompatActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null && view != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(KEY_SEARCHTEXT,
                searchBarBinding.searchView.getText() != null && !StringUtils.isNullOrWhitespace(searchBarBinding.searchView.getText().toString())
                        ? searchBarBinding.searchView.getText().toString() : "");

        super.onSaveInstanceState(outState);
    }
}