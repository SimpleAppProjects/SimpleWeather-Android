package com.thewizrd.simpleweather;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.thewizrd.simpleweather.utils.JSONParser;
import com.thewizrd.simpleweather.utils.Settings;
import com.thewizrd.simpleweather.utils.WeatherUtils;
import com.thewizrd.simpleweather.weather.weatherunderground.WUDataLoader;
import com.thewizrd.simpleweather.weather.weatherunderground.WUDataLoaderTask;
import com.thewizrd.simpleweather.weather.weatherunderground.data.WUWeather;
import com.thewizrd.simpleweather.weather.yahoo.YahooWeatherDataLoader;
import com.thewizrd.simpleweather.weather.yahoo.YahooWeatherLoaderTask;
import com.thewizrd.simpleweather.weather.yahoo.data.YahooWeather;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LocationsFragment extends Fragment implements WeatherLoadedListener {

    private Context context;

    // Views
    private LocationPanel HomePanel;
    private RecyclerView mRecyclerView;
    private LocationPanelAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private Button addLocationsButton;

    // Search
    private LocationSearchFragment mSearchFragment;
    private ActionMode mActionMode;
    private View searchViewLayout;
    private EditText searchView;
    private ImageView backButtonView;
    private ImageView clearButtonView;
    private ImageView locationButtonView;
    private boolean inSearchUI;
    private String query;

    public LocationsFragment() {
        // Required empty public constructor
    }

    public void onWeatherLoaded(int locationIdx, Object weather) {
        if (weather != null) {

            // Home Panel
            if (locationIdx == 0)
            {
                if (weather instanceof WUWeather) {
                    HomePanel.setWeather((WUWeather) weather);
                } else if (weather instanceof YahooWeather) {
                    HomePanel.setWeather((YahooWeather) weather);
                }
            }
            // Others
            else
            {
                LocationPanelModel panel = mAdapter.get(locationIdx - 1);
                panel.Weather = weather;
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (searchViewLayout == null) {
                searchViewLayout = getActivity().getLayoutInflater().inflate(R.layout.search_action_bar, null);
                searchViewLayout.setPadding(0, 0, 16, 0); // l, t, r, b
            }
            mode.setCustomView(searchViewLayout);
            enterSearchUi();
            return true;
        }

        // Called each time the action mode is shown. Always called after onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false; // Return false if nothing is done
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            exitSearchUi();
            mActionMode = null;
        }
    };

    // For LocationPanels
    private View.OnClickListener onPanelClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.isEnabled()) {
                LocationPanel v = (LocationPanel) view;
                Pair<Integer, Object> pair = (Pair<Integer, Object>) v.getTag();

                Fragment fragment = null;

                if (Settings.getAPI().equals("WUnderground")) {
                    fragment = WeatherNowFragment.newInstance((String) pair.second, pair.first);
                } else {
                    WeatherUtils.Coordinate coord = (WeatherUtils.Coordinate) pair.second;
                    fragment = WeatherNowFragment.newInstance(coord.getCoordinatePair(), pair.first);
                }

                // Navigate to WeatherNowFragment
                AppCompatActivity activity = (AppCompatActivity) getActivity();
                activity.getSupportFragmentManager().beginTransaction().add(
                        R.id.fragment_container, fragment).addToBackStack(null).commit();
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getActivity().getApplicationContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_locations, container, false);
        view.findViewById(R.id.search_fragment_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitSearchUi();
            }
        });

        HomePanel = (LocationPanel) view.findViewById(R.id.home_panel);
        HomePanel.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                menu.add(Menu.NONE, v.getId(), 0, "Change Favorite Location");
            }
        });
        HomePanel.setOnClickListener(onPanelClickListener);

        // Other Locations
        mRecyclerView = (RecyclerView) view.findViewById(R.id.other_location_container);
        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(this.getActivity(), onPanelClickListener));

        addLocationsButton = (Button) view.findViewById(R.id.other_location_add);
        addLocationsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppCompatActivity activity = (AppCompatActivity) getActivity();
                mActionMode = activity.startSupportActionMode(mActionModeCallback);
            }
        });

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(false);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new LocationPanelAdapter(new ArrayList<LocationPanelModel>());
        mRecyclerView.setAdapter(mAdapter);

        view.post(new Runnable() {
            @Override
            public void run() {
                LoadLocations();
            }
        });

        return view;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // Home Panel
        if (item.getItemId() == R.id.home_panel)
        {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            mActionMode = activity.startSupportActionMode(mActionModeCallback);
            mActionMode.setTag(R.id.home_panel);
        } else if (item.getTitle() == "Delete Location") {
            // Other Locations
            mAdapter.remove(item.getItemId());

            if (Settings.getAPI().equals("WUnderground")) {
                List<String> locations = Settings.getLocations_WU();
                locations.remove(item.getItemId() + 1);
                Settings.saveLocations_WU(locations);
            } else {
                List<WeatherUtils.Coordinate> locations = Settings.getLocations();
                locations.remove(item.getItemId() + 1);
                Settings.saveLocations(locations);
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update view on resume
        // ex. If temperature unit changed
        // TODO: do this

        // Title
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        activity.getSupportActionBar().setTitle(getString(R.string.label_nav_locations));
    }

    private void LoadLocations() {
        // Lets load it up...
        if (Settings.getAPI().equals("WUnderground"))
        {
            // Weather Loader
            WUDataLoader wu_Loader = null;

            List<String> locations = Settings.getLocations_WU();

            for (String location : locations)
            {
                int index = locations.indexOf(location);

                if (index == 0) { // Home
                    // Nothing
                    HomePanel.setTag(new Pair<>(index, location));
                } else {
                    LocationPanelModel panel =  new LocationPanelModel(new Pair<Integer, Object>(index, location), null);
                    mAdapter.add(index - 1, panel);
                }

                wu_Loader = new WUDataLoader(context, this, location, index);
                try {
                    wu_Loader.loadWeatherData(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // Weather Loader
            YahooWeatherDataLoader wLoader = null;

            List<WeatherUtils.Coordinate> locations = Settings.getLocations();

            for (WeatherUtils.Coordinate location : locations)
            {
                int index = locations.indexOf(location);

                if (index == 0) { // Home
                    // Nothing
                    HomePanel.setTag(new Pair<>(index, location));
                } else {
                    LocationPanelModel panel =  new LocationPanelModel(new Pair<Integer, Object>(index, location), null);
                    mAdapter.add(index - 1, panel);
                }

                wLoader = new YahooWeatherDataLoader(context, this, location, index);
                try {
                    wLoader.loadWeatherData(false);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onAttachFragment(Fragment childFragment) {
        if (childFragment instanceof LocationSearchFragment) {
            mSearchFragment = (LocationSearchFragment) childFragment;
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
        final FragmentTransaction transaction = getChildFragmentManager()
                .beginTransaction();
        transaction.show(mSearchFragment);
        transaction.commitAllowingStateLoss();
        getChildFragmentManager().executePendingTransactions();
        setupSearchUi();
    }

    private void setupSearchUi() {
        if (searchView == null) {
            prepareSearchView();
        }
        searchView.requestFocus();
    }

    private void addSearchFragment() {
        if (mSearchFragment != null) {
            return;
        }
        final FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        final LocationSearchFragment searchFragment = new LocationSearchFragment();
        searchFragment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LocationQueryView v = (LocationQueryView)view;
                int index = 0;

                if (Settings.getAPI().equals("WUnderground")) {
                    String query = v.getLocationQuery();
                    List<String> locations = Settings.getLocations_WU();

                    WUWeather weather = null;
                    try {
                        weather = new WUDataLoaderTask(getActivity()).execute(query).get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (weather == null) {
                        return;
                    }

                    if (mActionMode.getTag() != null && (int)mActionMode.getTag() == R.id.home_panel)
                    {
                        index = 0;
                        locations.set(index, query);
                        HomePanel.setTag(new Pair<>(index, query));
                        // ProgressBar
                        HomePanel.showLoading(true);
                        HomePanel.setWeather(weather);
                    }
                    else
                    {
                        index = locations.size();
                        locations.add(query);
                        // (TODO:) NOTE: panel number could be wrong since we're adding
                        LocationPanelModel panel = new LocationPanelModel(new Pair<Integer, Object>(index, query), weather);
                        mAdapter.add(index - 1, panel);
                    }

                    // Save new locations
                    Settings.saveLocations_WU(locations);
                    // Save weather
                    try {
                        JSONParser.serializer(weather, new File(getContext().getFilesDir(), "weather" + index + ".json"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    List<WeatherUtils.Coordinate> locations = Settings.getLocations();

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

                    if (mActionMode.getTag() != null && (int)mActionMode.getTag() == R.id.home_panel)
                    {
                        index = 0;
                        locations.set(index, local);
                        HomePanel.setTag(new Pair<>(index, query));
                        // ProgressBar
                        HomePanel.showLoading(true);
                        HomePanel.setWeather(weather);
                    }
                    else
                    {
                        index = locations.size();
                        locations.add(local);
                        // (TODO:) NOTE: panel number could be wrong since we're adding
                        LocationPanelModel panel = new LocationPanelModel(new Pair<Integer, Object>(index, local), weather);
                        mAdapter.add(index - 1, panel);
                    }

                    // Save new locations
                    Settings.saveLocations(locations);
                    // Save weather
                    try {
                        JSONParser.serializer(weather, new File(getContext().getFilesDir(), "weather" + index + ".json"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                exitSearchUi();
            }
        });
        searchFragment.setUserVisibleHint(false);
        ft.add(R.id.search_fragment_container, searchFragment);
        ft.commitAllowingStateLoss();
    }

    private void prepareSearchView() {
        backButtonView = (ImageView) searchViewLayout
                .findViewById(R.id.search_back_button);
        backButtonView.setVisibility(View.GONE);
        searchView = (EditText) searchViewLayout
                .findViewById(R.id.search_view);
        clearButtonView = (ImageView) searchViewLayout.findViewById(R.id.search_close_button);
        locationButtonView = (ImageView) searchViewLayout
                .findViewById(R.id.search_location_button);
        clearButtonView.setOnClickListener(new View.OnClickListener() {
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
        searchView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
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
        locationButtonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchFragment.fetchGeoLocation();
            }
        });
    }

    private void exitSearchUi() {
        searchView.setText("");

        if (mSearchFragment != null) {
            mSearchFragment.setUserVisibleHint(false);

            final FragmentTransaction transaction = getChildFragmentManager()
                    .beginTransaction();
            transaction.remove(mSearchFragment);
            mSearchFragment = null;
            transaction.commitAllowingStateLoss();
        }

        hideInputMethod(getActivity().getCurrentFocus());
        searchView.clearFocus();
        mActionMode.finish();
        inSearchUI = false;
    }

    private void showInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    private void hideInputMethod(View view) {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        if (imm != null && view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}