package com.thewizrd.simpleweather.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.adapters.LocationQueryAdapter;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.databinding.FragmentLocationSearchBinding;
import com.thewizrd.simpleweather.databinding.SearchActionBarBinding;
import com.thewizrd.simpleweather.snackbar.Snackbar;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;
import com.thewizrd.simpleweather.snackbar.SnackbarWindowAdjustCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

public class LocationSearchFragment extends CustomFragment {
    private FragmentLocationSearchBinding binding;
    private SearchActionBarBinding searchBarBinding;
    private LocationQueryAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private CancellationTokenSource cts;

    private WeatherManager wm;

    private static final String KEY_SEARCHTEXT = "search_text";

    public RecyclerOnClickListenerInterface getRecyclerOnClickListener() {
        return recyclerClickListener;
    }

    public void setRecyclerOnClickListener(RecyclerOnClickListenerInterface listener) {
        recyclerClickListener = listener;
    }

    public LocationSearchFragment() {
        // Required empty public constructor
        wm = WeatherManager.getInstance();
        cts = new CancellationTokenSource();
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
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        initSnackManager();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsLogger.logEvent("LocationSearchFragment: onCreate");
    }

    @Override
    public void onPause() {
        searchBarBinding.searchView.clearFocus();
        super.onPause();
        ctsCancel();
    }

    @Override
    public void onDestroy() {
        getAppCompatActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        ctsCancel();
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        ctsCancel();
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

    private RecyclerOnClickListenerInterface recyclerClickListener;

    public void showLoading(final boolean show) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (searchBarBinding != null) {
                    searchBarBinding.searchProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);

                    if (show || StringUtils.isNullOrEmpty(searchBarBinding.searchView.getText().toString()))
                        searchBarBinding.searchCloseButton.setVisibility(View.GONE);
                    else
                        searchBarBinding.searchCloseButton.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public void enableRecyclerView(final boolean enable) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                binding.recyclerView.setEnabled(enable);
            }
        });
    }

    @Override
    public boolean isAlive() {
        return binding != null && super.isAlive();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentLocationSearchBinding.inflate(inflater, container, false);
        searchBarBinding = binding.searchBar;
        View view = binding.getRoot();

        // Initialize
        searchBarBinding.searchBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getAppCompatActivity() != null) getAppCompatActivity().onBackPressed();
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView, new OnApplyWindowInsetsListener() {
            private int paddingStart = ViewCompat.getPaddingStart(binding.recyclerView);
            private int paddingTop = binding.recyclerView.getPaddingTop();
            private int paddingEnd = ViewCompat.getPaddingEnd(binding.recyclerView);
            private int paddingBottom = binding.recyclerView.getPaddingBottom();

            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                ViewCompat.setPaddingRelative(v,
                        paddingStart,
                        paddingTop,
                        paddingEnd,
                        paddingBottom + insets.getSystemWindowInsetBottom());
                return insets;
            }
        });

        binding.recyclerView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                ViewCompat.requestApplyInsets(v);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {

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

        int bg_color = Settings.getUserThemeMode() != UserThemeMode.AMOLED_DARK ?
                ActivityUtils.getColor(getAppCompatActivity(), android.R.attr.colorBackground) : Colors.BLACK;
        view.setBackgroundColor(bg_color);
    }

    @Override
    public void onDestroyView() {
        searchBarBinding = null;
        binding = null;
        super.onDestroyView();
    }

    public void fetchLocations(final String queryString) {
        // Cancel pending searches
        ctsCancel();

        if (!StringUtils.isNullOrWhitespace(queryString)) {
            final CancellationToken ctsToken = cts.getToken();

            AsyncTask.create(new Callable<Collection<LocationQueryViewModel>>() {
                @Override
                public Collection<LocationQueryViewModel> call() throws Exception {
                    if (ctsToken.isCancellationRequested()) return null;
                    return wm.getLocations(queryString);
                }
            }).addOnSuccessListener(new OnSuccessListener<Collection<LocationQueryViewModel>>() {
                @Override
                public void onSuccess(Collection<LocationQueryViewModel> results) {
                    mAdapter.setLocations(new ArrayList<>(results));
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    if (e instanceof WeatherException) {
                        showSnackbar(Snackbar.make(e.getMessage(), Snackbar.Duration.SHORT),
                                new SnackbarWindowAdjustCallback(getAppCompatActivity()));
                    }
                    mAdapter.setLocations(Collections.singletonList(new LocationQueryViewModel()));
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
    public void onResume() {
        super.onResume();

        final View root = getView();
        if (root != null) {
            final View searchBarContainer = searchBarBinding.getRoot();
            searchBarContainer.setVisibility(View.VISIBLE);
        }
    }

    public void requestSearchbarFocus() {
        if (searchBarBinding != null)
            searchBarBinding.searchView.requestFocus();
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
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        final FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        ft.detach(this);
        ft.attach(this);
        ft.commit();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(KEY_SEARCHTEXT,
                searchBarBinding.searchView.getText() != null && !StringUtils.isNullOrWhitespace(searchBarBinding.searchView.getText().toString())
                        ? searchBarBinding.searchView.getText().toString() : "");

        super.onSaveInstanceState(outState);
    }
}