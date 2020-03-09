package com.thewizrd.simpleweather.controls;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.ColorUtils;
import androidx.core.util.ObjectsCompat;

import com.google.android.material.tabs.TabLayout;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.BaseForecastItemViewModel;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.ListChangedArgs;
import com.thewizrd.shared_resources.helpers.ObservableArrayList;
import com.thewizrd.shared_resources.helpers.OnListChangedListener;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.ILoadingCollection;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.ObservableLoadingArrayList;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.controls.lineview.LineDataSeries;
import com.thewizrd.simpleweather.controls.lineview.LineView;
import com.thewizrd.simpleweather.controls.lineview.XLabelData;
import com.thewizrd.simpleweather.controls.lineview.YEntryData;

import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.ArrayList;
import java.util.List;

public class ForecastGraphPanel extends LinearLayout {
    private Context context;
    private LineView view;
    private TabLayout tabLayout;
    private List<BaseForecastItemViewModel> forecasts;

    private boolean isDarkMode;
    private final int FETCH_SIZE = (int) (5 * App.getInstance().getAppContext().getResources().getDisplayMetrics().scaledDensity);

    private enum GraphType {
        FORECASTS,
        WIND,
        PRECIPITATION
    }

    private GraphType mGraphType = GraphType.FORECASTS;

    // Event listeners
    private RecyclerOnClickListenerInterface onClickListener;

    public void setOnClickPositionListener(RecyclerOnClickListenerInterface onClickListener) {
        this.onClickListener = onClickListener;
    }

    public ForecastGraphPanel(Context context) {
        super(context);
        initialize(context);
    }

    public ForecastGraphPanel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public ForecastGraphPanel(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ForecastGraphPanel(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        view.invalidate();
    }

    private View.OnLayoutChangeListener onLayoutChangeListener = new View.OnLayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            if (forecasts instanceof ILoadingCollection) {
                final ILoadingCollection collection = ((ILoadingCollection) forecasts);

                if (forecasts.isEmpty() && collection.hasMoreItems()) {
                    AsyncTask.run(new Runnable() {
                        @Override
                        public void run() {
                            collection.loadMoreItems(1);
                        }
                    });
                }
            }
        }
    };

    private LineView.OnScrollChangeListener onScrollChangeListener = new LineView.OnScrollChangeListener() {
        @Override
        public void onScrollChange(LineView v, int scrollX, int oldScrollX) {
            int distanceToEnd = v.getExtentWidth() - (scrollX + v.getViewportWidth());

            if (distanceToEnd <= 2 * v.getViewportWidth() && forecasts instanceof ILoadingCollection) {
                final ILoadingCollection collection = ((ILoadingCollection) forecasts);
                if (collection.hasMoreItems() && !collection.isLoading()) {
                    AsyncTask.run(new Runnable() {
                        @Override
                        public void run() {
                            collection.loadMoreItems(FETCH_SIZE);
                        }
                    });
                }
            }
        }
    };

    private LineView.OnSizeChangedListener onSizeChangedListener = new LineView.OnSizeChangedListener() {
        @Override
        public void onSizeChanged(LineView v, int canvasWidth) {
            if (v.getViewportWidth() > 0 && canvasWidth > 0) {
                if (canvasWidth <= v.getViewportWidth() &&
                        forecasts instanceof ILoadingCollection) {
                    final ILoadingCollection collection = ((ILoadingCollection) forecasts);
                    if (collection.hasMoreItems() && !collection.isLoading()) {
                        AsyncTask.run(new Runnable() {
                            @Override
                            public void run() {
                                collection.loadMoreItems(FETCH_SIZE);
                            }
                        });
                    }
                }
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    private void initialize(Context context) {
        this.context = context;
        setOrientation(LinearLayout.VERTICAL);
        view = new LineView(context);
        tabLayout = new TabLayout(context);

        int lineViewHeight = context.getResources().getDimensionPixelSize(R.dimen.forecast_panel_height);
        view.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, lineViewHeight));
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (onClickListener != null)
                        onClickListener.onClick(v, view.getItemPositionFromPoint(event.getX()));
                    return true;
                }
                return false;
            }
        });
        view.addOnLayoutChangeListener(onLayoutChangeListener);
        view.setOnScrollChangedListener(onScrollChangeListener);
        view.setOnSizeChangedListener(onSizeChangedListener);

        int tabHeight = (int) ActivityUtils.dpToPx(context, 48.f);
        int tabLayoutPadding = (int) ActivityUtils.dpToPx(context, 12.f);
        tabLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, tabHeight));
        tabLayout.setBackgroundColor(Colors.TRANSPARENT);
        tabLayout.setTabMode(TabLayout.MODE_AUTO);
        tabLayout.setTabIndicatorFullWidth(true);
        tabLayout.setSelectedTabIndicatorColor(Color.WHITE);
        tabLayout.setInlineLabel(true);
        ((MarginLayoutParams) tabLayout.getLayoutParams()).topMargin = tabLayoutPadding;
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mGraphType = (GraphType) tab.getTag();
                resetLineView(true);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                view.smoothScrollTo(0, 0);
            }
        });

        this.removeAllViews();
        this.addView(view);
        this.addView(tabLayout);

        resetLineView(true);
    }

    private void updateTabs() {
        int count = 1;
        BaseForecastItemViewModel first = forecasts != null && forecasts.size() > 0 ? forecasts.get(0) : null;

        if (first instanceof ForecastItemViewModel) {
            if (!StringUtils.isNullOrWhitespace(first.getWindSpeed()) &&
                    !StringUtils.isNullOrWhitespace(first.getPop().replace("%", ""))) {
                count = 3;
            }
        } else if (first instanceof HourlyForecastItemViewModel) {
            if (Settings.getAPI().equals(WeatherAPI.OPENWEATHERMAP) ||
                    Settings.getAPI().equals(WeatherAPI.METNO) ||
                    Settings.getAPI().equals(WeatherAPI.NWS)) {
                count = 2;
            } else {
                count = 3;
            }
        }

        if (tabLayout.getTabCount() == 0) {
            TabLayout.Tab forecastTab = tabLayout.newTab();
            forecastTab.setCustomView(R.layout.forecast_graph_panel_tablayout);
            forecastTab.setText(R.string.notificationicon_temperature);
            TextView forecastIconView = forecastTab.view.findViewById(R.id.icon);
            forecastIconView.setText(R.string.wi_thermometer);
            forecastTab.setTag(GraphType.FORECASTS);
            tabLayout.addTab(forecastTab, 0, false);

            TabLayout.Tab windTab = tabLayout.newTab();
            windTab.setCustomView(R.layout.forecast_graph_panel_tablayout);
            windTab.setText(R.string.label_wind);
            TextView windIconView = windTab.view.findViewById(R.id.icon);
            windIconView.setText(R.string.wi_strong_wind);
            windTab.setTag(GraphType.WIND);
            tabLayout.addTab(windTab, 1, false);

            TabLayout.Tab precipTab = tabLayout.newTab();
            precipTab.setCustomView(R.layout.forecast_graph_panel_tablayout);
            precipTab.setText(R.string.label_precipitation);
            precipTab.setIcon(R.drawable.showers);
            TextView precipIconView = precipTab.view.findViewById(R.id.icon);
            precipIconView.setText(R.string.wi_raindrop);
            precipTab.setTag(GraphType.PRECIPITATION);
            tabLayout.addTab(precipTab, 2, false);
        }

        switch (count) {
            case 1: // Forecasts
            default:
                mGraphType = GraphType.FORECASTS;

                tabLayout.getTabAt(0).view.setVisibility(VISIBLE);
                tabLayout.getTabAt(1).view.setVisibility(GONE);
                tabLayout.getTabAt(2).view.setVisibility(GONE);

                if (!tabLayout.getTabAt(0).isSelected())
                    tabLayout.getTabAt(0).select();
                break;
            case 2: // Wind
                if (mGraphType == GraphType.PRECIPITATION) {
                    mGraphType = GraphType.WIND;
                }

                TabLayout.Tab precipTab = tabLayout.getTabAt(1);
                if (precipTab.view.getVisibility() == VISIBLE) {
                    precipTab.view.setVisibility(GONE);
                }
                if (precipTab.isSelected()) {
                    tabLayout.getTabAt(0).select();
                }
                break;
            case 3: // PoP
                tabLayout.getTabAt(0).view.setVisibility(VISIBLE);
                tabLayout.getTabAt(1).view.setVisibility(VISIBLE);
                tabLayout.getTabAt(2).view.setVisibility(VISIBLE);
                break;
        }
    }

    private void updateLineViewColors() {
        view.setLineColor(ColorUtils.setAlphaComponent(isDarkMode ? Colors.WHITE : Colors.GRAY, 0x80));
        view.setBackgroundLineColor(ColorUtils.setAlphaComponent(isDarkMode ? Colors.WHITE : Colors.GRAY, 0x80));
        view.setBottomTextColor(isDarkMode ? Colors.WHITE : Colors.BLACK);
    }

    private void resetLineView(boolean resetOffset) {
        updateLineViewColors();
        updateTabs();

        view.resetData();

        switch (mGraphType) {
            case FORECASTS:
            default: // Temp
                updateForecastGraph(forecasts);
                break;
            case WIND: // Wind
                updateWindForecastGraph(forecasts);
                break;
            case PRECIPITATION: // PoP
                updatePrecipicationGraph(forecasts);
                break;
        }

        if (resetOffset) view.smoothScrollTo(0, 0);
    }

    private void updateLineView(List<BaseForecastItemViewModel> dataset, boolean resetOffset) {
        updateLineViewColors();
        updateTabs();

        switch (mGraphType) {
            case FORECASTS:
            default: // Temp
                updateForecastGraph(dataset);
                break;
            case WIND: // Wind
                updateWindForecastGraph(dataset);
                break;
            case PRECIPITATION: // PoP
                updatePrecipicationGraph(dataset);
                break;
        }

        if (resetOffset) view.smoothScrollTo(0, 0);
    }

    private void updateForecastGraph(List<BaseForecastItemViewModel> dataset) {
        if (dataset != null && dataset.size() > 0) {
            view.setDrawGridLines(false);
            view.setDrawDotLine(false);
            view.setDrawDataLabels(true);
            view.setDrawIconLabels(true);
            view.setDrawGraphBackground(true);
            view.setDrawDotPoints(false);

            List<XLabelData> labelDataset = new ArrayList<>();
            List<YEntryData> hiTempDataset = new ArrayList<>();
            List<YEntryData> loTempDataset = null;

            if (dataset.get(0) instanceof ForecastItemViewModel) {
                loTempDataset = new ArrayList<>();
                view.setDrawSeriesLabels(true);
            } else {
                view.setDrawSeriesLabels(false);
            }

            for (int i = 0; i < dataset.size(); i++) {
                BaseForecastItemViewModel forecastItemViewModel = dataset.get(i);

                try {
                    float hiTemp = Float.parseFloat(StringUtils.removeNonDigitChars(forecastItemViewModel.getHiTemp()));
                    YEntryData hiTempData = new YEntryData(hiTemp, forecastItemViewModel.getHiTemp().trim());
                    hiTempDataset.add(hiTempData);

                    if (loTempDataset != null && forecastItemViewModel instanceof ForecastItemViewModel) {
                        ForecastItemViewModel fVM = (ForecastItemViewModel) forecastItemViewModel;

                        float loTemp = Float.parseFloat(StringUtils.removeNonDigitChars(fVM.getLoTemp()));
                        YEntryData loTempData = new YEntryData(loTemp, fVM.getLoTemp().trim());
                        loTempDataset.add(loTempData);
                    }

                    XLabelData xLabelData = new XLabelData(forecastItemViewModel.getDate(), forecastItemViewModel.getWeatherIcon(), 0);
                    labelDataset.add(xLabelData);
                } catch (NumberFormatException ex) {
                    Logger.writeLine(Log.DEBUG, ex);
                }
            }

            view.getDataLabels().addAll(labelDataset);

            final String hiTempSeriesLabel = context.getString(R.string.label_high);
            LineDataSeries hiTempDataSeries = Iterables.find(view.getDataLists(), new Predicate<LineDataSeries>() {
                @Override
                public boolean apply(@NullableDecl LineDataSeries input) {
                    return input != null && ObjectsCompat.equals(input.getSeriesLabel(), hiTempSeriesLabel);
                }
            }, null);

            if (hiTempDataSeries == null) {
                hiTempDataSeries = new LineDataSeries(hiTempSeriesLabel, hiTempDataset);
                view.getDataLists().add(0, hiTempDataSeries);
            } else {
                hiTempDataSeries.getSeriesData().addAll(hiTempDataset);
                view.getDataLists().set(0, hiTempDataSeries);
            }

            if (loTempDataset != null) {
                final String loTempSeriesLabel = context.getString(R.string.label_low);
                LineDataSeries loTempDataSeries = Iterables.find(view.getDataLists(), new Predicate<LineDataSeries>() {
                    @Override
                    public boolean apply(@NullableDecl LineDataSeries input) {
                        return input != null && ObjectsCompat.equals(input.getSeriesLabel(), loTempSeriesLabel);
                    }
                }, null);

                if (loTempDataSeries == null) {
                    loTempDataSeries = new LineDataSeries(loTempSeriesLabel, loTempDataset);
                    view.getDataLists().add(view.getDataLists().isEmpty() ? 0 : 1, loTempDataSeries);
                } else {
                    loTempDataSeries.getSeriesData().addAll(loTempDataset);
                    view.getDataLists().set(1, loTempDataSeries);
                }
            }
        }
    }

    private void updateWindForecastGraph(List<BaseForecastItemViewModel> dataset) {
        if (dataset != null && dataset.size() > 0) {
            view.setDrawGridLines(false);
            view.setDrawDotLine(false);
            view.setDrawDataLabels(true);
            view.setDrawIconLabels(false);
            view.setDrawGraphBackground(true);
            view.setDrawDotPoints(false);
            view.setDrawSeriesLabels(false);

            List<XLabelData> labelData = new ArrayList<>();
            List<YEntryData> windDataSet = new ArrayList<>();

            for (int i = 0; i < dataset.size(); i++) {
                BaseForecastItemViewModel forecastItemViewModel = dataset.get(i);

                try {
                    float wind = Float.parseFloat(StringUtils.removeNonDigitChars(forecastItemViewModel.getWindSpeed()));
                    YEntryData windData = new YEntryData(wind, forecastItemViewModel.getWindSpeed());

                    windDataSet.add(windData);
                    XLabelData xLabelData = new XLabelData(forecastItemViewModel.getDate(), context.getString(R.string.wi_wind_direction), forecastItemViewModel.getWindDirection());
                    labelData.add(xLabelData);
                } catch (NumberFormatException ex) {
                    Logger.writeLine(Log.DEBUG, ex);
                }
            }

            view.getDataLabels().addAll(labelData);

            LineDataSeries windDataSeries = Iterables.getFirst(view.getDataLists(), null);
            if (windDataSeries == null) {
                windDataSeries = new LineDataSeries(windDataSet);
                view.getDataLists().add(windDataSeries);
            } else {
                view.getDataLists().set(0, windDataSeries);
            }
        }
    }

    private void updatePrecipicationGraph(List<BaseForecastItemViewModel> dataset) {
        if (dataset != null && dataset.size() > 0) {
            view.setDrawGridLines(false);
            view.setDrawDotLine(false);
            view.setDrawDataLabels(true);
            view.setDrawIconLabels(false);
            view.setDrawGraphBackground(true);
            view.setDrawDotPoints(false);
            view.setDrawSeriesLabels(false);

            List<XLabelData> labelData = new ArrayList<>();
            List<YEntryData> popDataSet = new ArrayList<>();

            for (int i = 0; i < dataset.size(); i++) {
                BaseForecastItemViewModel forecastItemViewModel = dataset.get(i);

                try {
                    float pop = Float.parseFloat(StringUtils.removeNonDigitChars(forecastItemViewModel.getPop()));
                    YEntryData popData = new YEntryData(pop, forecastItemViewModel.getPop().trim());

                    popDataSet.add(popData);
                    XLabelData xLabelData = new XLabelData(forecastItemViewModel.getDate(), context.getString(R.string.wi_raindrop), 0);
                    labelData.add(xLabelData);
                } catch (NumberFormatException ex) {
                    Logger.writeLine(Log.DEBUG, ex);
                }
            }

            view.getDataLabels().addAll(labelData);

            LineDataSeries popDataSeries = Iterables.getFirst(view.getDataLists(), null);
            if (popDataSeries == null) {
                popDataSeries = new LineDataSeries(popDataSet);
                view.getDataLists().add(popDataSeries);
            } else {
                view.getDataLists().set(0, popDataSeries);
            }
        }
    }

    private OnListChangedListener<BaseForecastItemViewModel> onListChangedListener = new OnListChangedListener<BaseForecastItemViewModel>() {
        @Override
        public void onChanged(final ArrayList<BaseForecastItemViewModel> sender, final ListChangedArgs<BaseForecastItemViewModel> args) {
            if (forecasts instanceof ILoadingCollection) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        switch (args.action) {
                            case ADD:
                                updateLineView(args.newItems, false);
                                break;
                            case MOVE:
                            case REMOVE:
                            case REPLACE:
                            case RESET:
                                resetLineView(false);
                                break;
                        }
                    }
                });
            }
        }
    };

    public void updateColors(boolean isDark) {
        isDarkMode = isDark;
        updateLineViewColors();
    }

    public void updateForecasts(@NonNull final List<BaseForecastItemViewModel> dataset) {
        if (forecasts != dataset) {
            // Remove handler
            if (forecasts instanceof ObservableLoadingArrayList) {
                ((ObservableArrayList) forecasts).removeOnListChangedCallback(onListChangedListener);
            }

            forecasts = dataset;

            // Add new handler
            if (forecasts instanceof ObservableLoadingArrayList) {
                ((ObservableArrayList) forecasts).addOnListChangedCallback(onListChangedListener);
            }

            resetLineView(false);
        }
    }
}
