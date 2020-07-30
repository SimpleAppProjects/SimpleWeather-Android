package com.thewizrd.simpleweather.main;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.util.ObjectsCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.transition.MaterialSharedAxis;
import com.thewizrd.shared_resources.Constants;
import com.thewizrd.shared_resources.adapters.WeatherAlertPanelAdapter;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.ForecastsViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.controls.WeatherAlertViewModel;
import com.thewizrd.shared_resources.controls.WeatherAlertsViewModel;
import com.thewizrd.shared_resources.controls.WeatherNowViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.locationdata.LocationData;
import com.thewizrd.shared_resources.utils.AnalyticsLogger;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.JSONParser;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.UserThemeMode;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader;
import com.thewizrd.shared_resources.weatherdata.WeatherRequest;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.adapters.WeatherDetailsAdapter;
import com.thewizrd.simpleweather.databinding.FragmentWeatherListBinding;
import com.thewizrd.simpleweather.fragments.ToolbarFragment;
import com.thewizrd.simpleweather.snackbar.Snackbar;
import com.thewizrd.simpleweather.snackbar.SnackbarManager;

import java.util.List;

public class WeatherListFragment extends ToolbarFragment {
    private WeatherNowViewModel weatherView;
    private ForecastsViewModel forecastsView;
    private WeatherAlertsViewModel alertsView;
    private LocationData location;

    private FragmentWeatherListBinding binding;
    private LinearLayoutManager layoutManager;

    private WeatherListType weatherType;

    public WeatherListType getWeatherListType() {
        return weatherType;
    }

    private WeatherListFragmentArgs args;

    public WeatherListFragment() {
        setArguments(new Bundle());
    }

    public static WeatherListFragment newInstance(WeatherListType type) {
        WeatherListFragment fragment = new WeatherListFragment();
        fragment.weatherType = type;

        Bundle args = new Bundle();
        args.putInt(Constants.ARGS_WEATHERLISTTYPE, type.getValue());
        fragment.setArguments(args);

        return fragment;
    }

    public static WeatherListFragment newInstance(LocationData locData, WeatherListType type) {
        WeatherListFragment fragment = new WeatherListFragment();
        fragment.weatherType = type;
        fragment.location = locData;

        Bundle args = new Bundle();
        args.putInt(Constants.ARGS_WEATHERLISTTYPE, type.getValue());
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AnalyticsLogger.logEvent("WeatherListFragment: onCreate");

        args = WeatherListFragmentArgs.fromBundle(requireArguments());

        if (args.getWeatherListType() == WeatherListType.ALERTS) {
            setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
            setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.ARGS_WEATHERLISTTYPE)) {
                weatherType = WeatherListType.valueOf(savedInstanceState.getInt(Constants.ARGS_WEATHERLISTTYPE));
            }
            if (savedInstanceState.containsKey(Constants.KEY_DATA)) {
                location = JSONParser.deserializer(savedInstanceState.getString(Constants.KEY_DATA), LocationData.class);
            }
        } else {
            weatherType = args.getWeatherListType();
            if (args.getData() != null) {
                location = JSONParser.deserializer(args.getData(), LocationData.class);
            }
        }

        if (location == null)
            location = Settings.getHomeData();
    }

    @Override
    public boolean isAlive() {
        return binding != null && super.isAlive();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) super.onCreateView(inflater, container, savedInstanceState);
        // Use this to return your custom view for this Fragment
        binding = FragmentWeatherListBinding.inflate(inflater, root, true);

        // Setup Actionbar
        final Context context = binding.getRoot().getContext();
        Drawable navIcon = DrawableCompat.wrap(ContextCompat.getDrawable(context, ActivityUtils.getResourceId(getAppCompatActivity(), R.attr.homeAsUpIndicator)));
        DrawableCompat.setTint(navIcon, ContextCompat.getColor(context, R.color.invButtonColorText));
        getToolbar().setNavigationIcon(navIcon);

        getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigateUp();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            binding.locationHeader.setClipToOutline(false);
            binding.locationHeader.setOutlineProvider(new ViewOutlineProvider() {
                int elevation = context.getResources().getDimensionPixelSize(R.dimen.appbar_elevation);

                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRect(0, view.getHeight() - elevation, view.getWidth(), view.getHeight());
                }
            });
        }

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the binding.recyclerView
        binding.recyclerView.setHasFixedSize(true);
        // use a linear layout manager
        binding.recyclerView.setLayoutManager(layoutManager = new LinearLayoutManager(getAppCompatActivity()));

        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (recyclerView.computeVerticalScrollOffset() > 0) {
                        binding.locationHeader.setCardElevation(ActivityUtils.dpToPx(context, 4f));
                    } else {
                        binding.locationHeader.setCardElevation(0);
                    }
                }
            }
        });

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ViewModelProvider vmProvider = new ViewModelProvider(getAppCompatActivity());
        weatherView = vmProvider.get(WeatherNowViewModel.class);
        alertsView = vmProvider.get(WeatherAlertsViewModel.class);
        forecastsView = new ViewModelProvider(this).get(ForecastsViewModel.class);

        args = WeatherListFragmentArgs.fromBundle(requireArguments());

        binding.locationHeader.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (isAlive()) {
                    ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) binding.recyclerView.getLayoutParams();
                    layoutParams.topMargin = binding.locationHeader.getHeight();
                    binding.recyclerView.setLayoutParams(layoutParams);
                }
                return true;
            }
        });
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!isHidden()) {
            AnalyticsLogger.logEvent("WeatherListFragment: onResume");
            initialize();
        }
    }

    @Override
    public void onPause() {
        AnalyticsLogger.logEvent("WeatherListFragment: onPause");
        super.onPause();
    }

    @Override
    protected int getTitle() {
        switch (weatherType) {
            case FORECAST:
            case HOURLYFORECAST:
                return R.string.label_forecast;
            case ALERTS:
                return R.string.title_fragment_alerts;
            default:
                return R.string.label_nav_weathernow;
        }
    }

    // Initialize views here
    @CallSuper
    protected void initialize() {
        if (!weatherView.isValid() || (location != null && !ObjectsCompat.equals(location.getQuery(), weatherView.getQuery()))) {
            new WeatherDataLoader(location)
                    .loadWeatherData(new WeatherRequest.Builder()
                            .forceLoadSavedData()
                            .setErrorListener(new WeatherRequest.WeatherErrorListener() {
                                @Override
                                public void onWeatherError(WeatherException wEx) {
                                    switch (wEx.getErrorStatus()) {
                                        case NETWORKERROR:
                                        case NOWEATHER:
                                            // Show error message and prompt to refresh
                                            showSnackbar(Snackbar.make(wEx.getMessage(), Snackbar.Duration.LONG), null);
                                            break;
                                        case QUERYNOTFOUND:
                                            if (WeatherAPI.NWS.equals(Settings.getAPI())) {
                                                showSnackbar(Snackbar.make(R.string.error_message_weather_us_only, Snackbar.Duration.LONG), null);
                                                break;
                                            }
                                        default:
                                            // Show error message
                                            showSnackbar(Snackbar.make(wEx.getMessage(), Snackbar.Duration.LONG), null);
                                            break;
                                    }
                                }
                            })
                            .build())
                    .addOnSuccessListener(requireActivity(), new OnSuccessListener<Weather>() {
                        @Override
                        public void onSuccess(Weather weather) {
                            if (isAlive()) {
                                weatherView.updateView(weather);
                                forecastsView.updateForecasts(location);
                                alertsView.updateAlerts(location);
                                binding.locationName.setText(weatherView.getLocation());
                            }
                        }
                    });
        } else {
            forecastsView.updateForecasts(location);
            alertsView.updateAlerts(location);
            binding.locationName.setText(weatherView.getLocation());
        }

        // specify an adapter (see also next example)
        final RecyclerView.Adapter adapter = binding.recyclerView.getAdapter();
        switch (weatherType) {
            case FORECAST:
            case HOURLYFORECAST:
                if (binding.recyclerView.getItemDecorationCount() == 0)
                    binding.recyclerView.addItemDecoration(new DividerItemDecoration(getAppCompatActivity(), DividerItemDecoration.VERTICAL));

                final WeatherDetailsAdapter detailsAdapter;
                if (!(adapter instanceof WeatherDetailsAdapter)) {
                    detailsAdapter = new WeatherDetailsAdapter();
                    binding.recyclerView.setAdapter(detailsAdapter);
                } else {
                    detailsAdapter = (WeatherDetailsAdapter) adapter;
                }

                if (weatherType == WeatherListType.FORECAST) {
                    forecastsView.getForecasts().removeObservers(WeatherListFragment.this);
                    forecastsView.getForecasts().observe(WeatherListFragment.this, new Observer<PagedList<ForecastItemViewModel>>() {
                        @Override
                        public void onChanged(PagedList<ForecastItemViewModel> forecasts) {
                            detailsAdapter.submitList(forecasts);
                        }
                    });
                } else {
                    forecastsView.getHourlyForecasts().removeObservers(WeatherListFragment.this);
                    forecastsView.getHourlyForecasts().observe(WeatherListFragment.this, new Observer<PagedList<HourlyForecastItemViewModel>>() {
                        @Override
                        public void onChanged(PagedList<HourlyForecastItemViewModel> hrforecasts) {
                            detailsAdapter.submitList(hrforecasts);
                        }
                    });
                }

                detailsAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                    @Override
                    public void onChanged() {
                        if (detailsAdapter.getCurrentList() != null) {
                            detailsAdapter.unregisterAdapterDataObserver(this);

                            detailsAdapter.getCurrentList().loadAround(args.getPosition());
                            binding.recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                @Override
                                public void onGlobalLayout() {
                                    binding.recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    layoutManager.scrollToPositionWithOffset(args.getPosition(), 0);
                                }
                            });
                        }
                    }

                    @Override
                    public void onItemRangeChanged(int positionStart, int itemCount) {
                        onChanged();
                    }

                    @Override
                    public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
                        onChanged();
                    }

                    @Override
                    public void onItemRangeInserted(int positionStart, int itemCount) {
                        onChanged();
                    }

                    @Override
                    public void onItemRangeRemoved(int positionStart, int itemCount) {
                        onChanged();
                    }

                    @Override
                    public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                        onChanged();
                    }
                });
                break;
            case ALERTS:
                final WeatherAlertPanelAdapter alertAdapter;
                if (!(adapter instanceof WeatherAlertPanelAdapter)) {
                    alertAdapter = new WeatherAlertPanelAdapter();
                    binding.recyclerView.setAdapter(alertAdapter);
                } else {
                    alertAdapter = (WeatherAlertPanelAdapter) adapter;
                }

                alertsView.getAlerts().removeObservers(WeatherListFragment.this);
                alertsView.getAlerts().observe(WeatherListFragment.this, new Observer<List<WeatherAlertViewModel>>() {
                    @Override
                    public void onChanged(List<WeatherAlertViewModel> alerts) {
                        alertAdapter.updateItems(alerts);
                    }
                });
                break;
            default:
                binding.recyclerView.setAdapter(null);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Save data
        outState.putInt(Constants.ARGS_WEATHERLISTTYPE, weatherType.getValue());
        outState.putString(Constants.KEY_DATA, JSONParser.serializer(location, LocationData.class));

        super.onSaveInstanceState(outState);
    }

    @Override
    public void updateWindowColors() {
        super.updateWindowColors();

        int color = ActivityUtils.getColor(getAppCompatActivity(), android.R.attr.colorBackground);
        if (Settings.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
            color = Colors.BLACK;
        }
        binding.locationHeader.setCardBackgroundColor(color);
        binding.recyclerView.setBackgroundColor(color);
    }

    @NonNull
    @Override
    public SnackbarManager createSnackManager() {
        SnackbarManager mSnackMgr = new SnackbarManager(binding.getRoot());
        mSnackMgr.setSwipeDismissEnabled(true);
        mSnackMgr.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE);
        return mSnackMgr;
    }
}
