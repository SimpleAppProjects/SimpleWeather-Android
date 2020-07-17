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
import androidx.core.view.ViewGroupCompat;

import com.google.android.material.tabs.TabLayout;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.controls.BaseForecastItemViewModel;
import com.thewizrd.shared_resources.controls.ForecastItemViewModel;
import com.thewizrd.shared_resources.controls.HourlyForecastItemViewModel;
import com.thewizrd.shared_resources.helpers.ActivityUtils;
import com.thewizrd.shared_resources.helpers.RecyclerOnClickListenerInterface;
import com.thewizrd.shared_resources.utils.Colors;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.NumberUtils;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.StringUtils;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.simpleweather.R;
import com.thewizrd.simpleweather.controls.lineview.LineDataSeries;
import com.thewizrd.simpleweather.controls.lineview.LineView;
import com.thewizrd.simpleweather.controls.lineview.XLabelData;
import com.thewizrd.simpleweather.controls.lineview.YEntryData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class ForecastGraphPanel extends LinearLayout {
    private Context context;
    private LineView view;
    private TabLayout tabLayout;
    private List<BaseForecastItemViewModel> forecasts;

    private Configuration currentConfig;

    private static final int MAX_FETCH_SIZE = 24; // 24hrs

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
        currentConfig = new Configuration(newConfig);

        updateTabColors();
        updateLineViewColors();

        view.invalidate();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initialize(Context context) {
        this.context = context;
        this.currentConfig = new Configuration(context.getResources().getConfiguration());
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
        // Individual transitions on the view can cause
        // OpenGLRenderer: GL error:  GL_INVALID_VALUE
        ViewGroupCompat.setTransitionGroup(this, true);

        resetLineView(true);
    }

    private void updateTabs() {
        int count = 1;
        BaseForecastItemViewModel first = forecasts != null && forecasts.size() > 0 ? forecasts.get(0) : null;

        if (first instanceof ForecastItemViewModel) {
            if (!StringUtils.isNullOrWhitespace(first.getWindSpeed()))
                count++;
            if (first.getPop() != null && !StringUtils.isNullOrWhitespace(first.getPop().replace("%", "")))
                count++;
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

        updateTabColors();

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

                tabLayout.getTabAt(0).view.setVisibility(VISIBLE);
                tabLayout.getTabAt(1).view.setVisibility(VISIBLE);

                TabLayout.Tab precipTab = tabLayout.getTabAt(2);
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

    private void updateTabColors() {
        final int systemNightMode = currentConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        final boolean isNightMode = systemNightMode == Configuration.UI_MODE_NIGHT_YES;

        TabLayout.Tab forecastTab = tabLayout.getTabAt(0);
        ((TextView) forecastTab.view.findViewById(R.id.icon)).setTextColor(isNightMode ? Colors.WHITE : Colors.BLACK);
        ((TextView) forecastTab.view.findViewById(android.R.id.text1)).setTextColor(isNightMode ? Colors.WHITE : Colors.BLACK);

        TabLayout.Tab windTab = tabLayout.getTabAt(1);
        ((TextView) windTab.view.findViewById(R.id.icon)).setTextColor(isNightMode ? Colors.WHITE : Colors.BLACK);
        ((TextView) windTab.view.findViewById(android.R.id.text1)).setTextColor(isNightMode ? Colors.WHITE : Colors.BLACK);

        TabLayout.Tab precipTab = tabLayout.getTabAt(2);
        ((TextView) precipTab.view.findViewById(R.id.icon)).setTextColor(isNightMode ? Colors.WHITE : Colors.BLACK);
        ((TextView) precipTab.view.findViewById(android.R.id.text1)).setTextColor(isNightMode ? Colors.WHITE : Colors.BLACK);

        tabLayout.setSelectedTabIndicatorColor(isNightMode ? Colors.WHITE : Colors.BLACK);
    }

    private void updateLineViewColors() {
        final int systemNightMode = currentConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        final boolean isNightMode = systemNightMode == Configuration.UI_MODE_NIGHT_YES;

        view.setLineColor(ColorUtils.setAlphaComponent(isNightMode ? Colors.WHITE : Colors.GRAY, 0x80));
        view.setBackgroundLineColor(ColorUtils.setAlphaComponent(isNightMode ? Colors.WHITE : Colors.GRAY, 0x80));
        view.setBottomTextColor(isNightMode ? Colors.WHITE : Colors.BLACK);
    }

    private void resetLineView(boolean resetOffset) {
        updateLineViewColors();
        updateTabs();

        view.resetData();

        new AsyncTask<Void>().await(new Callable<Void>() {
            @Override
            public Void call() {
                switch (mGraphType) {
                    case FORECASTS:
                    default: // Temp
                        updateForecastGraph();
                        break;
                    case WIND: // Wind
                        updateWindForecastGraph();
                        break;
                    case PRECIPITATION: // PoP
                        updatePrecipicationGraph();
                        break;
                }
                return null;
            }
        });

        if (resetOffset) view.smoothScrollTo(0, 0);
    }

    private void updateForecastGraph() {
        if (forecasts != null && forecasts.size() > 0) {
            view.setDrawGridLines(false);
            view.setDrawDotLine(false);
            view.setDrawDataLabels(true);
            view.setDrawIconLabels(true);
            view.setDrawGraphBackground(true);
            view.setDrawDotPoints(false);

            List<XLabelData> labelDataset = new ArrayList<>();
            List<YEntryData> hiTempDataset = new ArrayList<>();
            List<YEntryData> loTempDataset = null;

            if (forecasts.get(0) instanceof ForecastItemViewModel) {
                loTempDataset = new ArrayList<>();
                view.setDrawSeriesLabels(true);
            } else {
                view.setDrawSeriesLabels(false);
            }

            for (int i = 0; i < forecasts.size(); i++) {
                BaseForecastItemViewModel forecastItemViewModel = forecasts.get(i);

                try {
                    Float hiTemp = NumberUtils.tryParseFloat(StringUtils.removeNonDigitChars(forecastItemViewModel.getHiTemp()));
                    if (hiTemp != null) {
                        YEntryData hiTempData = new YEntryData(hiTemp, forecastItemViewModel.getHiTemp().trim());
                        hiTempDataset.add(hiTempData);
                    } else if (i == 0 && i + 1 < forecasts.size()) { // For NWS, which contains bi-daily forecasts
                        BaseForecastItemViewModel nextVM = forecasts.get(i + 1);
                        YEntryData hiTempData = new YEntryData(Float.parseFloat(StringUtils.removeNonDigitChars(nextVM.getHiTemp())), "");
                        hiTempDataset.add(hiTempData);
                    } else if (i == forecasts.size() - 1) { // For NWS, which contains bi-daily forecasts
                        BaseForecastItemViewModel prevVM = forecasts.get(i - 1);
                        YEntryData hiTempData = new YEntryData(Float.parseFloat(StringUtils.removeNonDigitChars(prevVM.getHiTemp())), "");
                        hiTempDataset.add(hiTempData);
                    }

                    if (loTempDataset != null && forecastItemViewModel instanceof ForecastItemViewModel) {
                        ForecastItemViewModel fVM = (ForecastItemViewModel) forecastItemViewModel;

                        Float loTemp = NumberUtils.tryParseFloat(StringUtils.removeNonDigitChars(fVM.getLoTemp()));
                        if (loTemp != null) {
                            YEntryData loTempData = new YEntryData(loTemp, fVM.getLoTemp().trim());
                            loTempDataset.add(loTempData);
                        } else if (i == 0 && i + 1 < forecasts.size()) { // For NWS, which contains bi-daily forecasts
                            ForecastItemViewModel nextVM = (ForecastItemViewModel) forecasts.get(i + 1);
                            YEntryData loTempData = new YEntryData(Float.parseFloat(StringUtils.removeNonDigitChars(nextVM.getLoTemp())), "");
                            loTempDataset.add(loTempData);
                        } else if (i == forecasts.size() - 1) { // For NWS, which contains bi-daily forecasts
                            ForecastItemViewModel prevVM = (ForecastItemViewModel) forecasts.get(i - 1);
                            YEntryData loTempData = new YEntryData(Float.parseFloat(StringUtils.removeNonDigitChars(prevVM.getLoTemp())), "");
                            loTempDataset.add(loTempData);
                        }
                    }

                    XLabelData xLabelData = new XLabelData(forecastItemViewModel.getDate(), forecastItemViewModel.getWeatherIcon(), 0);
                    labelDataset.add(xLabelData);
                } catch (NumberFormatException ex) {
                    Logger.writeLine(Log.DEBUG, ex);
                }
            }

            view.getDataLabels().addAll(labelDataset);

            if (hiTempDataset.size() > 0) {
                final String hiTempSeriesLabel = context.getString(R.string.label_high);
                view.getDataLists().add(new LineDataSeries(hiTempSeriesLabel, hiTempDataset));
            }

            if (loTempDataset != null) {
                if (loTempDataset.size() > 0) {
                    final String loTempSeriesLabel = context.getString(R.string.label_low);
                    view.getDataLists().add(new LineDataSeries(loTempSeriesLabel, loTempDataset));
                }
            }
        }
    }

    private void updateWindForecastGraph() {
        if (forecasts != null && forecasts.size() > 0) {
            view.setDrawGridLines(false);
            view.setDrawDotLine(false);
            view.setDrawDataLabels(true);
            view.setDrawIconLabels(false);
            view.setDrawGraphBackground(true);
            view.setDrawDotPoints(false);
            view.setDrawSeriesLabels(false);

            List<XLabelData> labelData = new ArrayList<>();
            List<YEntryData> windDataSet = new ArrayList<>();

            for (int i = 0; i < forecasts.size(); i++) {
                BaseForecastItemViewModel forecastItemViewModel = forecasts.get(i);

                try {
                    float wind = Float.parseFloat(StringUtils.removeNonDigitChars(forecastItemViewModel.getWindSpeed()));
                    YEntryData windData = new YEntryData(wind, forecastItemViewModel.getWindSpeed());

                    windDataSet.add(windData);
                    XLabelData xLabelData = new XLabelData(forecastItemViewModel.getDate(), context.getString(R.string.wi_wind_direction), forecastItemViewModel.getWindDirection() + 180);
                    labelData.add(xLabelData);
                } catch (NumberFormatException ex) {
                    Logger.writeLine(Log.DEBUG, ex);
                }
            }

            view.getDataLabels().addAll(labelData);

            if (windDataSet.size() > 0) {
                view.getDataLists().add(new LineDataSeries(windDataSet));
            }
        }
    }

    private void updatePrecipicationGraph() {
        if (forecasts != null && forecasts.size() > 0) {
            view.setDrawGridLines(false);
            view.setDrawDotLine(false);
            view.setDrawDataLabels(true);
            view.setDrawIconLabels(false);
            view.setDrawGraphBackground(true);
            view.setDrawDotPoints(false);
            view.setDrawSeriesLabels(false);

            List<XLabelData> labelData = new ArrayList<>();
            List<YEntryData> popDataSet = new ArrayList<>();

            for (int i = 0; i < forecasts.size(); i++) {
                BaseForecastItemViewModel forecastItemViewModel = forecasts.get(i);

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

            if (popDataSet.size() > 0) {
                view.getDataLists().add(new LineDataSeries(popDataSet));
            }
        }
    }

    public void updateForecasts(@NonNull final List<BaseForecastItemViewModel> dataset) {
        if (forecasts != dataset) {
            forecasts = dataset;
            resetLineView(true);
        }
    }
}