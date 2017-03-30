package com.thewizrd.simpleweather;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.LayoutParams;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.thewizrd.simpleweather.utils.Settings;

public class SetupActivity extends AppCompatActivity {

    private LocationSearchFragment mSearchFragment;
    private Spinner apiSpinner;
    private EditText keyEntry;
    private EditText searchView;
    private ImageView backButtonView;
    private ImageView clearButtonView;
    private ImageView locationButtonView;
    private boolean inSearchUI;
    private String query;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        // Setup Actionbar
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setElevation(0);

        apiSpinner = (Spinner) findViewById(R.id.api_spinner);
        keyEntry = (EditText) findViewById(R.id.key_entry);

        /* Event Listeners */
        apiSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) { // WeatherUnderground
                    Settings.setAPI("WUnderground");
                    findViewById(R.id.key_entry_box).setVisibility(View.VISIBLE);
                } else {
                    Settings.setAPI("Yahoo");
                    findViewById(R.id.key_entry_box).setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //
            }
        });
        keyEntry.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showInputMethod(v.findFocus());
                } else {
                    hideInputMethod(v);
                }
            }
        });
        keyEntry.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(s)) {
                    Settings.setAPIKEY(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        findViewById(R.id.activity_setup).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear focus
                keyEntry.clearFocus();
            }
        });
        findViewById(R.id.search_view_container).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                enterSearchUi();
            }
        });
        findViewById(R.id.search_fragment_container).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                exitSearchUi();
            }
        });

        // Reset focus
        findViewById(R.id.activity_setup).requestFocus();

        // Set WUnderground as default API
        apiSpinner.setSelection(0);

        // Load API key
        if (Settings.getAPIKEY() != null) {
            keyEntry.setText(Settings.getAPIKEY());
        }
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof LocationSearchFragment) {
            mSearchFragment = (LocationSearchFragment) fragment;
            setupSearchUi();
        }
    }

    private void enterSearchUi() {
        inSearchUI = true;
        if (mSearchFragment == null) {
            addSearchFragment();
            return;
        }
        mSearchFragment.setUserVisibleHint(true);
        mSearchFragment.getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        final FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction();
        transaction.show(mSearchFragment);
        transaction.commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();
        setupSearchUi();
    }

    private void setupSearchUi() {
        if (searchView == null) {
            prepareSearchView();
        }
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setBackgroundDrawable(null);
        actionBar.setElevation(5);
        searchView.requestFocus();
    }

    private void addSearchFragment() {
        if (mSearchFragment != null) {
            return;
        }
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        final Fragment searchFragment = new LocationSearchFragment();
        searchFragment.setUserVisibleHint(false);
        ft.add(R.id.search_fragment_container, searchFragment);
        ft.commitAllowingStateLoss();
    }

    private void prepareSearchView() {
        final View searchViewLayout = getLayoutInflater().inflate(
                R.layout.search_action_bar, null);
        backButtonView = (ImageView) searchViewLayout
                .findViewById(R.id.search_back_button);
        backButtonView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                exitSearchUi();
            }
        });
        searchView = (EditText) searchViewLayout
                .findViewById(R.id.search_view);
        clearButtonView = (ImageView) searchViewLayout.findViewById(R.id.search_close_button);
        locationButtonView = (ImageView) searchViewLayout
                .findViewById(R.id.search_location_button);
        clearButtonView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                searchView.setText("");
            }
        });
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final String newText = s.toString();
                if (newText.equals(query)) {
                    // If the query hasn't changed (perhaps due to activity being destroyed
                    // and restored, or user launching the same DIAL intent twice), then there is
                    // no need to do anything here.
                    return;
                }
                query = newText;
                if (mSearchFragment != null) {
                    clearButtonView.setVisibility(TextUtils.isEmpty(s) ? View.GONE : View.VISIBLE);
                    mSearchFragment.fetchLocations(query);
                }
            }

            @Override
            public void afterTextChanged(Editable e) {
            }
        });
        clearButtonView.setVisibility(View.GONE);
        searchView.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    showInputMethod(v.findFocus());
                } else {
                    hideInputMethod(v);
                }
            }
        });
        searchView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    if (mSearchFragment != null) {
                        mSearchFragment.fetchLocations(query);
                        hideInputMethod(v);
                    }
                    return true;
                }
                return false;
            }
        });
        locationButtonView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchFragment.fetchGeoLocation();
            }
        });

        getSupportActionBar().setCustomView(
                searchViewLayout,
                new LayoutParams(LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT));
    }

    @Override
    public void onBackPressed() {
        if (inSearchUI) {
            // We should let the user go back to usual screens with tabs.
            exitSearchUi();
        } else {
            super.onBackPressed();
        }
    }

    private void exitSearchUi() {
        final ActionBar actionBar = getSupportActionBar();
        if (mSearchFragment != null) {
            mSearchFragment.setUserVisibleHint(false);

            final FragmentTransaction transaction = getSupportFragmentManager()
                    .beginTransaction();
            transaction.remove(mSearchFragment);
            mSearchFragment = null;
            transaction.commitAllowingStateLoss();
        }

        // We want to hide SearchView and show Tabs. Also focus on previously
        // selected one.
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.colorPrimary)));
        actionBar.setElevation(0);
        hideInputMethod(getCurrentFocus());
        invalidateOptionsMenu();
        searchView.clearFocus();
        inSearchUI = false;
    }

    private void showInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void hideInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
