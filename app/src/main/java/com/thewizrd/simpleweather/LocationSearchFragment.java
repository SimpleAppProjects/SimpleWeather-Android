package com.thewizrd.simpleweather;

import android.annotation.SuppressLint;
import android.content.Context;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.adapters.LocationQueryAdapter;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;

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
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (FragmentActivity) context;
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
        mActivity = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ctsCancel();
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

    private RecyclerOnClickListenerInterface recyclerClickListener;

    public void showLoading(final boolean show) {
        runOnUiThread(new Runnable() {
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
        mActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_location_search, container, false);

        mProgressBar = mActivity.findViewById(R.id.search_progressBar);
        mClearButton = mActivity.findViewById(R.id.search_close_button);
        mSearchView = mActivity.findViewById(R.id.search_view);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mProgressBar.setIndeterminateDrawable(
                    ContextCompat.getDrawable(mActivity, R.drawable.progressring));
        }

        mRecyclerView = view.findViewById(R.id.recycler_view);

        /*
           Capture touch events on RecyclerView
           We're not using ADJUST_RESIZE so hide the keyboard when necessary
           Hide the keyboard if we're scrolling to the bottom (so the bottom items behind the keyboard are visible)
           Leave the keyboard up if we're scrolling to the top (items at the top are already visible)
        */
        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
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

        return view;
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
                        runOnUiThread(new Runnable() {
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

    private void showInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.showSoftInput(view, 0);
        }
    }

    private void hideInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) mActivity.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}